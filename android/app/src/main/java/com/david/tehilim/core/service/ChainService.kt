package com.david.tehilim.core.service

import android.content.Context
import com.david.tehilim.core.model.ChainAssignment
import com.david.tehilim.core.model.ChainIntention
import com.david.tehilim.core.model.ChainParticipant
import com.david.tehilim.core.model.TehilimChain
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Couche d'accès Firestore pour la « Chaîne de Tehilim » (mirror Android de
 * ChainService.swift). Écritures via méthodes suspend ; lectures temps réel
 * via Flows (callbackFlow autour de addSnapshotListener). Identité = uid
 * anonyme Firebase.
 */
class ChainService(private val appContext: Context) {

    /** Firebase configuré ? (false si google-services.json absent au build.) */
    val isAvailable: Boolean
        get() = FirebaseApp.getApps(appContext).isNotEmpty()

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user!!.uid
    }

    // MARK: - Écritures

    suspend fun createChain(
        name: String,
        intention: ChainIntention,
        detail: String,
        selectionDurationMillis: Long,
        readingDeadlineMillis: Long,
        creatorName: String
    ): String {
        val uid = ensureSignedIn()
        val now = System.currentTimeMillis()
        val chainRef = db.collection(CHAINS).document()
        val data = mapOf(
            F_NAME to name,
            F_INTENTION_TYPE to intention.wire,
            F_INTENTION_DETAIL to detail,
            F_CREATOR_UID to uid,
            F_CREATOR_NAME to creatorName,
            F_CREATED_AT to Date(now),
            F_SELECTION_DEADLINE to Date(now + selectionDurationMillis),
            F_READING_DEADLINE to Date(readingDeadlineMillis),
            F_DISTRIBUTED to false,
            F_EXPIRES_AT to Date(readingDeadlineMillis + EXPIRY_GRACE_MILLIS)
        )
        chainRef.set(data).await()
        chainRef.collection(PARTICIPANTS).document(uid).set(
            mapOf(F_NAME to creatorName, F_IS_CREATOR to true, F_JOINED_AT to Date(now))
        ).await()
        return chainRef.id
    }

    suspend fun join(chainId: String, name: String) {
        val uid = ensureSignedIn()
        db.collection(CHAINS).document(chainId).collection(PARTICIPANTS).document(uid)
            .set(mapOf(F_NAME to name, F_IS_CREATOR to false, F_JOINED_AT to Date()))
            .await()
    }

    /** Réserve un Tehilim (transaction : échoue si déjà pris). */
    suspend fun select(chainId: String, psalmId: Int, name: String) {
        val uid = ensureSignedIn()
        val ref = assignmentRef(chainId, psalmId)
        db.runTransaction { txn ->
            val snap = txn.get(ref)
            if (snap.exists()) {
                throw FirebaseFirestoreException(
                    "Tehilim déjà pris", FirebaseFirestoreException.Code.ABORTED
                )
            }
            txn.set(ref, mapOf(
                F_UID to uid, F_NAME to name, F_BY_CREATOR to false, F_ASSIGNED_AT to Date()
            ))
            null
        }.await()
    }

    suspend fun deselect(chainId: String, psalmId: Int) {
        ensureSignedIn()
        assignmentRef(chainId, psalmId).delete().await()
    }

    /** (Créateur) attribue tous les Tehilim restants à lui-même. */
    suspend fun assignRemaining(chainId: String, name: String) {
        val uid = ensureSignedIn()
        val col = db.collection(CHAINS).document(chainId).collection(ASSIGNMENTS)
        val taken = col.get().await().documents.mapNotNull { it.id.toIntOrNull() }.toSet()
        val batch = db.batch()
        for (p in 1..TehilimChain.TOTAL_PSALMS) {
            if (p !in taken) {
                batch.set(col.document(p.toString()), mapOf(
                    F_UID to uid, F_NAME to name, F_BY_CREATOR to true, F_ASSIGNED_AT to Date()
                ))
            }
        }
        batch.commit().await()
    }

    suspend fun distribute(chainId: String) {
        ensureSignedIn()
        db.collection(CHAINS).document(chainId).update(F_DISTRIBUTED, true).await()
    }

    suspend fun fetchChain(id: String): TehilimChain? =
        chainFrom(db.collection(CHAINS).document(id).get().await())

    private fun assignmentRef(chainId: String, psalmId: Int) =
        db.collection(CHAINS).document(chainId).collection(ASSIGNMENTS).document(psalmId.toString())

    // MARK: - Flows temps réel

    fun chainFlow(chainId: String): Flow<TehilimChain?> = callbackFlow {
        val reg = db.collection(CHAINS).document(chainId)
            .addSnapshotListener { snap, _ -> trySend(snap?.let { chainFrom(it) }) }
        awaitClose { reg.remove() }
    }

    fun participantsFlow(chainId: String): Flow<List<ChainParticipant>> = callbackFlow {
        val reg = db.collection(CHAINS).document(chainId).collection(PARTICIPANTS)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { participantFrom(it) }
                    ?.sortedBy { it.joinedAtMillis } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun assignmentsFlow(chainId: String): Flow<Map<Int, ChainAssignment>> = callbackFlow {
        val reg = db.collection(CHAINS).document(chainId).collection(ASSIGNMENTS)
            .addSnapshotListener { snap, _ ->
                val map = snap?.documents?.mapNotNull { assignmentFrom(it) }
                    ?.associateBy { it.psalmId } ?: emptyMap()
                trySend(map)
            }
        awaitClose { reg.remove() }
    }

    // MARK: - Mapping

    private fun millis(snap: DocumentSnapshot, key: String): Long =
        (snap.get(key) as? Timestamp)?.toDate()?.time ?: 0L

    private fun chainFrom(snap: DocumentSnapshot): TehilimChain? {
        if (!snap.exists()) return null
        val name = snap.getString(F_NAME) ?: return null
        val intention = ChainIntention.fromWire(snap.getString(F_INTENTION_TYPE)) ?: return null
        val creatorUid = snap.getString(F_CREATOR_UID) ?: return null
        return TehilimChain(
            id = snap.id,
            name = name,
            intentionType = intention,
            intentionDetail = snap.getString(F_INTENTION_DETAIL) ?: "",
            creatorUid = creatorUid,
            creatorName = snap.getString(F_CREATOR_NAME) ?: "",
            createdAtMillis = millis(snap, F_CREATED_AT),
            selectionDeadlineMillis = millis(snap, F_SELECTION_DEADLINE),
            readingDeadlineMillis = millis(snap, F_READING_DEADLINE),
            distributed = snap.getBoolean(F_DISTRIBUTED) ?: false,
            expiresAtMillis = millis(snap, F_EXPIRES_AT)
        )
    }

    private fun participantFrom(snap: DocumentSnapshot): ChainParticipant? {
        if (!snap.exists()) return null
        return ChainParticipant(
            uid = snap.id,
            name = snap.getString(F_NAME) ?: "—",
            isCreator = snap.getBoolean(F_IS_CREATOR) ?: false,
            joinedAtMillis = millis(snap, F_JOINED_AT)
        )
    }

    private fun assignmentFrom(snap: DocumentSnapshot): ChainAssignment? {
        val psalmId = snap.id.toIntOrNull() ?: return null
        val uid = snap.getString(F_UID) ?: return null
        return ChainAssignment(
            psalmId = psalmId,
            uid = uid,
            name = snap.getString(F_NAME) ?: "—",
            byCreator = snap.getBoolean(F_BY_CREATOR) ?: false,
            assignedAtMillis = millis(snap, F_ASSIGNED_AT)
        )
    }

    companion object {
        const val CHAINS = "chains"
        const val PARTICIPANTS = "participants"
        const val ASSIGNMENTS = "assignments"
        const val F_NAME = "name"
        const val F_INTENTION_TYPE = "intentionType"
        const val F_INTENTION_DETAIL = "intentionDetail"
        const val F_CREATOR_UID = "creatorUid"
        const val F_CREATOR_NAME = "creatorName"
        const val F_CREATED_AT = "createdAt"
        const val F_SELECTION_DEADLINE = "selectionDeadline"
        const val F_READING_DEADLINE = "readingDeadline"
        const val F_DISTRIBUTED = "distributed"
        const val F_EXPIRES_AT = "expiresAt"
        const val F_IS_CREATOR = "isCreator"
        const val F_JOINED_AT = "joinedAt"
        const val F_UID = "uid"
        const val F_BY_CREATOR = "byCreator"
        const val F_ASSIGNED_AT = "assignedAt"
        const val EXPIRY_GRACE_MILLIS = 7L * 24 * 3600 * 1000
    }
}
