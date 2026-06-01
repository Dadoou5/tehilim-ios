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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Couche Firestore « Chaîne de Tehilim » (mirror Android).
 *
 * Optimisation quota : l'état des attributions tient dans **un seul document**
 * `chains/{id}/state/board` (champ map `a` = psalmId → {u,n,c}) → charger la
 * grille = 1 lecture au lieu de 150. Verrou atomique via transaction.
 */
class ChainService(private val appContext: Context) {

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

    @Suppress("UNCHECKED_CAST")
    private fun boardMap(snap: DocumentSnapshot): MutableMap<String, Map<String, Any>> =
        ((snap.get(B_MAP) as? Map<String, Map<String, Any>>) ?: emptyMap()).toMutableMap()

    /** Réserve un Tehilim (transaction sur le doc board ; échoue si déjà pris). */
    suspend fun select(chainId: String, psalmId: Int, name: String) {
        val uid = ensureSignedIn()
        val ref = boardRef(chainId)
        db.runTransaction { txn ->
            val a = boardMap(txn.get(ref))
            if (a.containsKey(psalmId.toString())) {
                throw FirebaseFirestoreException("Tehilim déjà pris", FirebaseFirestoreException.Code.ABORTED)
            }
            a[psalmId.toString()] = mapOf(B_U to uid, B_N to name, B_C to false)
            txn.set(ref, mapOf(B_MAP to a), SetOptions.merge())
            null
        }.await()
    }

    suspend fun deselect(chainId: String, psalmId: Int) {
        val uid = ensureSignedIn()
        val ref = boardRef(chainId)
        db.runTransaction { txn ->
            val a = boardMap(txn.get(ref))
            val entry = a[psalmId.toString()]
            if (entry != null && entry[B_U] == uid) {
                a.remove(psalmId.toString())
                txn.set(ref, mapOf(B_MAP to a), SetOptions.merge())
            }
            null
        }.await()
    }

    /** (Créateur) attribue tous les Tehilim restants à lui-même. */
    suspend fun assignRemaining(chainId: String, name: String) {
        val uid = ensureSignedIn()
        val ref = boardRef(chainId)
        db.runTransaction { txn ->
            val a = boardMap(txn.get(ref))
            for (p in 1..TehilimChain.TOTAL_PSALMS) {
                if (!a.containsKey(p.toString())) {
                    a[p.toString()] = mapOf(B_U to uid, B_N to name, B_C to true)
                }
            }
            txn.set(ref, mapOf(B_MAP to a), SetOptions.merge())
            null
        }.await()
    }

    suspend fun distribute(chainId: String) {
        ensureSignedIn()
        db.collection(CHAINS).document(chainId).update(F_DISTRIBUTED, true).await()
    }

    private fun boardRef(chainId: String) =
        db.collection(CHAINS).document(chainId).collection(STATE).document(BOARD)

    // MARK: - Lecture ponctuelle

    suspend fun fetchChain(id: String): TehilimChain? =
        chainFrom(db.collection(CHAINS).document(id).get().await())

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

    /** Écoute le doc board unique → map psalmId → attribution. */
    fun boardFlow(chainId: String): Flow<Map<Int, ChainAssignment>> = callbackFlow {
        val reg = boardRef(chainId).addSnapshotListener { snap, _ -> trySend(boardFrom(snap)) }
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

    @Suppress("UNCHECKED_CAST")
    private fun boardFrom(snap: DocumentSnapshot?): Map<Int, ChainAssignment> {
        val map = (snap?.get(B_MAP) as? Map<String, Map<String, Any>>) ?: return emptyMap()
        val out = HashMap<Int, ChainAssignment>()
        for ((k, v) in map) {
            val pid = k.toIntOrNull() ?: continue
            val uid = v[B_U] as? String ?: continue
            out[pid] = ChainAssignment(
                psalmId = pid, uid = uid,
                name = v[B_N] as? String ?: "—",
                byCreator = v[B_C] as? Boolean ?: false,
                assignedAtMillis = 0L
            )
        }
        return out
    }

    companion object {
        const val CHAINS = "chains"
        const val PARTICIPANTS = "participants"
        const val STATE = "state"
        const val BOARD = "board"
        const val B_MAP = "a"
        const val B_U = "u"
        const val B_N = "n"
        const val B_C = "c"
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
        const val EXPIRY_GRACE_MILLIS = 7L * 24 * 3600 * 1000
    }
}
