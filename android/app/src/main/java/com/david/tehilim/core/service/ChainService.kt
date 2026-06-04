package com.david.tehilim.core.service

import android.content.Context
import com.david.tehilim.core.model.ChainAssignment
import com.david.tehilim.core.model.ChainIntention
import com.david.tehilim.core.model.ChainParticipant
import com.david.tehilim.core.model.TehilimChain
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Couche d'accès **Supabase** (Postgres + Realtime + auth anonyme) — mirror
 * Android. Remplace Firebase Firestore.
 *
 * Modèle **relationnel** : une ligne par attribution dans `chain_assignments`,
 * dont la **clé primaire `(chain_id, psalm_id)`** fait office de **verrou
 * exclusif** « 1 lecteur / Tehilim » (un INSERT en double échoue ⇒ « déjà
 * pris », atomiquement). La propriété est imposée **côté serveur** par la RLS.
 *
 * L'API publique (suspend funcs + `chainFlow` / `participantsFlow` /
 * `boardFlow`) est **identique** à la version Firebase : la couche UI (Compose)
 * est inchangée. `context` est conservé pour la signature mais inutilisé (le
 * client est global, cf. [SupabaseClientProvider]).
 */
class ChainService(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val client get() = SupabaseClientProvider.client

    val isAvailable: Boolean get() = client != null

    val currentUid: String? get() = client?.auth?.currentUserOrNull()?.id?.lowercase()

    suspend fun ensureSignedIn(): String {
        val c = client ?: error("Supabase non configuré")
        c.auth.currentUserOrNull()?.let { return it.id.lowercase() }
        c.auth.signInAnonymously()
        return c.auth.currentUserOrNull()?.id?.lowercase() ?: error("Auth anonyme indisponible")
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
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        val now = System.currentTimeMillis()
        // RPC atomique : crée la chaîne + le créateur-participant, renvoie l'id.
        // (supabase-kt 3.0.x : rpc prend un JsonObject de paramètres.)
        val params = buildJsonObject {
            put("p_name", name)
            put("p_intention_type", intention.wire)
            put("p_intention_detail", detail)
            put("p_creator_name", creatorName)
            put("p_selection_deadline", iso(now + selectionDurationMillis))
            put("p_reading_deadline", iso(readingDeadlineMillis))
            put("p_expires_at", iso(readingDeadlineMillis + EXPIRY_GRACE_MILLIS))
        }
        return c.postgrest.rpc("create_chain", params).decodeAs<String>()
    }

    suspend fun join(chainId: String, name: String) {
        val c = client ?: error("Supabase non configuré")
        val uid = ensureSignedIn()
        c.from(PARTICIPANTS).upsert(
            ParticipantUpsert(chainId = chainId, uid = uid, name = name, isCreator = false)
        ) { onConflict = "chain_id,uid" }
    }

    /** Réserve un Tehilim. INSERT : échoue (violation de PK) s'il est déjà pris. */
    suspend fun select(chainId: String, psalmId: Int, name: String) {
        val c = client ?: error("Supabase non configuré")
        val uid = ensureSignedIn()
        c.from(ASSIGNMENTS).insert(
            AssignmentInsert(chainId = chainId, psalmId = psalmId, uid = uid, name = name, byCreator = false)
        )
    }

    /** Libère un Tehilim réservé par soi-même (la RLS empêche de libérer celui d'autrui). */
    suspend fun deselect(chainId: String, psalmId: Int) {
        val c = client ?: error("Supabase non configuré")
        val uid = ensureSignedIn()
        c.from(ASSIGNMENTS).delete {
            filter {
                eq("chain_id", chainId)
                eq("psalm_id", psalmId)
                eq("uid", uid)
            }
        }
    }

    /** (Créateur) attribue tous les Tehilim restants à lui-même (RPC, une requête). */
    suspend fun assignRemaining(chainId: String, name: String) {
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        c.postgrest.rpc("assign_remaining", buildJsonObject {
            put("p_chain_id", chainId)
            put("p_name", name)
        })
    }

    suspend fun distribute(chainId: String) {
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        c.from(CHAINS).update({ set("distributed", true) }) {
            filter { eq("id", chainId) }
        }
    }

    /** (Créateur) supprime définitivement la chaîne (cascade → participants + attributions). */
    suspend fun deleteChain(chainId: String) {
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        c.from(CHAINS).delete { filter { eq("id", chainId) } }
    }

    /** Un participant quitte la chaîne : libère ses Tehilim puis se retire. */
    suspend fun leaveChain(chainId: String) {
        val c = client ?: error("Supabase non configuré")
        val uid = ensureSignedIn()
        c.from(ASSIGNMENTS).delete { filter { eq("chain_id", chainId); eq("uid", uid) } }
        c.from(PARTICIPANTS).delete { filter { eq("chain_id", chainId); eq("uid", uid) } }
    }

    /** (Créateur) édite la chaîne (avant distribution). RLS : réservé au créateur. */
    suspend fun updateChain(
        chainId: String, name: String, intention: ChainIntention, detail: String,
        selectionDeadlineMillis: Long, readingDeadlineMillis: Long
    ) {
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        c.from(CHAINS).update({
            set("name", name)
            set("intention_type", intention.wire)
            set("intention_detail", detail)
            set("selection_deadline", iso(selectionDeadlineMillis))
            set("reading_deadline", iso(readingDeadlineMillis))
            set("expires_at", iso(readingDeadlineMillis + EXPIRY_GRACE_MILLIS))
        }) { filter { eq("id", chainId) } }
    }

    /** (Créateur) retire un participant + libère ses cases (RPC). */
    suspend fun removeParticipant(chainId: String, uid: String) {
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        c.postgrest.rpc("remove_participant", buildJsonObject {
            put("p_chain_id", chainId)
            put("p_uid", uid)
        })
    }

    /** (Créateur) prolonge la sélection : repousse l'échéance, réarme les rappels
     *  et re-notifie les participants (RPC, réservé au créateur). */
    suspend fun extendSelection(chainId: String, newDeadlineMillis: Long) {
        val c = client ?: error("Supabase non configuré")
        ensureSignedIn()
        c.postgrest.rpc("extend_chain_selection", buildJsonObject {
            put("p_chain_id", chainId)
            put("p_new_deadline", iso(newDeadlineMillis))
        })
    }

    /** Enregistre / met à jour le token push de cet appareil (notifs de chaîne). */
    suspend fun registerDeviceToken(token: String, platform: String = "android", locale: String) {
        val c = client ?: return
        val uid = runCatching { ensureSignedIn() }.getOrNull() ?: return
        runCatching {
            c.from("device_tokens").upsert(
                DeviceTokenRow(token = token, uid = uid, platform = platform,
                               locale = if (locale == "en") "en" else "fr")
            ) { onConflict = "token" }
        }
    }

    // MARK: - Lectures ponctuelles

    suspend fun fetchChain(id: String): TehilimChain? {
        val c = client ?: return null
        return c.from(CHAINS).select { filter { eq("id", id) } }
            .decodeList<ChainRow>().firstOrNull()?.let(::toChain)
    }

    private suspend fun fetchParticipants(chainId: String): List<ChainParticipant> {
        val c = client ?: return emptyList()
        return c.from(PARTICIPANTS).select {
            filter { eq("chain_id", chainId) }
            order("joined_at", Order.ASCENDING)
        }.decodeList<ParticipantRow>().map(::toParticipant)
    }

    private suspend fun fetchBoard(chainId: String): Map<Int, ChainAssignment> {
        val c = client ?: return emptyMap()
        return c.from(ASSIGNMENTS).select { filter { eq("chain_id", chainId) } }
            .decodeList<AssignmentRow>().associate { it.psalmId to toAssignment(it) }
    }

    // MARK: - Flows temps réel (Supabase Realtime)
    // Sur chaque évènement realtime de la table, on recharge la collection
    // concernée (SELECT léger) : simple, robuste, adapté à l'échelle. Quand le
    // collecteur s'arrête (écran fermé / arrière-plan via collectAsStateWith-
    // Lifecycle), `awaitClose` se déclenche → on retire le canal (= écoute
    // coupée, équivalent du start/stop iOS et de l'optimisation quota).

    fun chainFlow(chainId: String): Flow<TehilimChain?> =
        realtimeFlow(chainId, CHAINS, "id", null) { fetchChain(chainId) }

    fun participantsFlow(chainId: String): Flow<List<ChainParticipant>> =
        realtimeFlow(chainId, PARTICIPANTS, "chain_id", emptyList()) { fetchParticipants(chainId) }

    fun boardFlow(chainId: String): Flow<Map<Int, ChainAssignment>> =
        realtimeFlow(chainId, ASSIGNMENTS, "chain_id", emptyMap()) { fetchBoard(chainId) }

    private fun <T> realtimeFlow(
        chainId: String,
        tableName: String,
        filterColumn: String,
        initial: T,
        fetch: suspend () -> T
    ): Flow<T> = callbackFlow {
        val c = client
        if (c == null) {
            trySend(initial); close(); return@callbackFlow
        }
        val channel = c.channel("rt:$tableName:$chainId:${System.nanoTime()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = tableName
            filter(filterColumn, FilterOperator.EQ, chainId)
        }
        val collectJob = launch {
            changes.collect { runCatching { trySend(fetch()) } }
        }
        launch {
            runCatching {
                ensureSignedIn()
                channel.subscribe()
                trySend(fetch())   // état initial
            }
        }
        awaitClose {
            collectJob.cancel()
            // Nettoyage sur un scope indépendant (le scope du flow se ferme).
            CoroutineScope(Dispatchers.Default).launch {
                runCatching { c.realtime.removeChannel(channel) }
            }
        }
    }

    // MARK: - Dates (Postgres timestamptz ↔ epochMillis)

    private fun millis(s: String?): Long =
        s?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
            ?: System.currentTimeMillis()

    private fun iso(millis: Long): String = Instant.fromEpochMilliseconds(millis).toString()

    // MARK: - Mapping DTO → modèles applicatifs (inchangés)

    private fun toChain(r: ChainRow) = TehilimChain(
        id = r.id,
        name = r.name,
        intentionType = ChainIntention.fromWire(r.intentionType) ?: ChainIntention.REUSSITE,
        intentionDetail = r.intentionDetail ?: "",
        creatorUid = r.creatorUid.lowercase(),
        creatorName = r.creatorName ?: "",
        createdAtMillis = millis(r.createdAt),
        selectionDeadlineMillis = millis(r.selectionDeadline),
        readingDeadlineMillis = millis(r.readingDeadline),
        distributed = r.distributed,
        expiresAtMillis = millis(r.expiresAt)
    )

    private fun toParticipant(r: ParticipantRow) = ChainParticipant(
        uid = r.uid.lowercase(),
        name = r.name ?: "—",
        isCreator = r.isCreator,
        joinedAtMillis = millis(r.joinedAt)
    )

    private fun toAssignment(r: AssignmentRow) = ChainAssignment(
        psalmId = r.psalmId,
        uid = r.uid.lowercase(),
        name = r.name ?: "—",
        byCreator = r.byCreator,
        assignedAtMillis = millis(r.assignedAt)
    )

    // MARK: - DTO (colonnes Postgres en snake_case)

    @Serializable
    private data class ChainRow(
        val id: String,
        val name: String,
        @SerialName("intention_type") val intentionType: String,
        @SerialName("intention_detail") val intentionDetail: String? = null,
        @SerialName("creator_uid") val creatorUid: String,
        @SerialName("creator_name") val creatorName: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("selection_deadline") val selectionDeadline: String? = null,
        @SerialName("reading_deadline") val readingDeadline: String? = null,
        val distributed: Boolean = false,
        @SerialName("expires_at") val expiresAt: String? = null
    )

    @Serializable
    private data class ParticipantRow(
        val uid: String,
        val name: String? = null,
        @SerialName("is_creator") val isCreator: Boolean = false,
        @SerialName("joined_at") val joinedAt: String? = null
    )

    @Serializable
    private data class AssignmentRow(
        @SerialName("psalm_id") val psalmId: Int,
        val uid: String,
        val name: String? = null,
        @SerialName("by_creator") val byCreator: Boolean = false,
        @SerialName("assigned_at") val assignedAt: String? = null
    )

    @Serializable
    private data class ParticipantUpsert(
        @SerialName("chain_id") val chainId: String,
        val uid: String,
        val name: String,
        @SerialName("is_creator") val isCreator: Boolean
    )

    @Serializable
    private data class AssignmentInsert(
        @SerialName("chain_id") val chainId: String,
        @SerialName("psalm_id") val psalmId: Int,
        val uid: String,
        val name: String,
        @SerialName("by_creator") val byCreator: Boolean
    )

    @Serializable
    private data class DeviceTokenRow(
        val token: String,
        val uid: String,
        val platform: String,
        val locale: String
    )

    companion object {
        const val CHAINS = "chains"
        const val PARTICIPANTS = "chain_participants"
        const val ASSIGNMENTS = "chain_assignments"
        const val EXPIRY_GRACE_MILLIS = 7L * 24 * 3600 * 1000
    }
}
