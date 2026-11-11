package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.vfdeginformatica.mysuperapp.data.mapper.toSpotifyConnection
import com.vfdeginformatica.mysuperapp.data.mapper.toSpotifyDevices
import com.vfdeginformatica.mysuperapp.data.mapper.toSpotifyItems
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyErrorCode
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyException
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SpotifyRemoteDaoImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : SpotifyRemoteDao {

    companion object {
        private const val TAG = "SpotifyRemoteDaoImpl"
        private const val USERS = "users"
        private const val FIELD_SPOTIFY = "spotify"
    }

    override fun observeConnection(): Flow<SpotifyConnection?> = callbackFlow {
        var registration: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            registration?.remove()
            registration = null
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
                return@AuthStateListener
            }
            registration = db.collection(USERS).document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeConnection: ${error.message}")
                        trySend(SpotifyConnection())
                        return@addSnapshotListener
                    }
                    trySend((snapshot?.get(FIELD_SPOTIFY) as? Map<*, *>).toSpotifyConnection())
                }
        }
        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            registration?.remove()
        }
    }

    override suspend fun startAuth(): String {
        val result = call("spotifyAuthStart")
        return result["authUrl"] as? String
            ?: throw SpotifyException(SpotifyErrorCode.UNKNOWN, "Resposta inválida do servidor")
    }

    override suspend fun disconnect() {
        call("spotifyDisconnect")
    }

    override suspend fun getMyPlaylists(offset: Int, limit: Int): List<SpotifyItem> {
        val result = call("spotifyMyPlaylists", mapOf("offset" to offset, "limit" to limit))
        return (result["items"] as? List<*>).toSpotifyItems()
    }

    override suspend fun search(query: String, kinds: Set<SpotifyItemKind>): List<SpotifyItem> {
        if (query.isBlank() || kinds.isEmpty()) return emptyList()
        val result = call(
            "spotifySearch",
            mapOf("query" to query, "types" to kinds.map { it.name.lowercase() })
        )
        return (result["items"] as? List<*>).toSpotifyItems()
    }

    override suspend fun getDevices(): List<SpotifyDevice> {
        val result = call("spotifyDevices")
        return (result["devices"] as? List<*>).toSpotifyDevices()
    }

    private suspend fun call(name: String, data: Map<String, Any?>? = null): Map<*, *> {
        if (auth.currentUser == null) {
            throw SpotifyException(SpotifyErrorCode.UNKNOWN, "Entre na sua conta")
        }
        return try {
            val callable = functions.getHttpsCallable(name)
            val task = if (data != null) callable.call(data) else callable.call()
            task.await().data as? Map<*, *> ?: emptyMap<String, Any>()
        } catch (e: FirebaseFunctionsException) {
            val details = e.details as? Map<*, *>
            val code = SpotifyErrorCode.from(details?.get("code") as? String)
            Log.w(TAG, "$name failed: ${e.code} ${e.message}")
            throw SpotifyException(code, e.message ?: "Erro ao falar com o Spotify")
        } catch (e: SpotifyException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$name failed: ${e.message}", e)
            throw SpotifyException(SpotifyErrorCode.UNKNOWN, e.message ?: "Erro ao falar com o Spotify")
        }
    }
}
