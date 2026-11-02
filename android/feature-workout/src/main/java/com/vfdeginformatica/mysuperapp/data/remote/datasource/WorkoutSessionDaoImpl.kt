package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutSessionDto
import kotlinx.coroutines.tasks.await

class WorkoutSessionDaoImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : WorkoutSessionDao {

    private companion object {
        const val COLLECTION = "workout_sessions"
        const val TAG = "Firebase"
    }

    private fun currentUserId(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun userSessions(userId: String): Query =
        db.collection(COLLECTION).whereEqualTo("userId", userId)

    override suspend fun getSessions(workoutId: String): List<WorkoutSessionDto>? {
        return try {
            val userId = currentUserId() ?: return emptyList()
            val snap = userSessions(userId)
                .whereEqualTo("workoutId", workoutId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(WorkoutSessionDto::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar registros do treino $workoutId", e)
            null
        }
    }

    override suspend fun getSessionsSince(from: Timestamp): List<WorkoutSessionDto>? {
        return try {
            val userId = currentUserId() ?: return emptyList()
            val snap = userSessions(userId)
                .whereGreaterThanOrEqualTo("date", from)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(WorkoutSessionDto::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar registros recentes", e)
            null
        }
    }

    override suspend fun addSession(session: WorkoutSessionDto): Resource<String> {
        return try {
            val userId = checkNotNull(currentUserId()) { "Entre na sua conta para registrar treinos" }
            val data = mapOf(
                "userId" to userId,
                "workoutId" to session.workoutId,
                "date" to session.date,
                "durationMinutes" to session.durationMinutes,
                "calories" to session.calories,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            val ref = db.collection(COLLECTION).add(data).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar treino", e)
            Resource.Error(e.message ?: "Erro inesperado ao registrar treino")
        }
    }

    override suspend fun deleteSession(id: String): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir registro $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir registro")
        }
    }
}
