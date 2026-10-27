package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutItemDto
import kotlinx.coroutines.tasks.await

class WorkoutDaoImpl(
    private val db: FirebaseFirestore
) : WorkoutDao {

    private companion object {
        const val COLLECTION = "workouts"
        const val TAG = "Firebase"
    }

    override suspend fun getWorkouts(): List<WorkoutDto>? {
        return try {
            val snap = db.collection(COLLECTION).get().await()
            snap.documents.mapNotNull { it.toObject(WorkoutDto::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar treinos", e)
            null
        }
    }

    override suspend fun getWorkoutsUsingExercise(exerciseId: String): List<WorkoutDto>? {
        return try {
            val snap = db.collection(COLLECTION)
                .whereArrayContains("exerciseIds", exerciseId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(WorkoutDto::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar treinos do exercício $exerciseId", e)
            null
        }
    }

    override suspend fun addWorkout(workout: WorkoutDto): Resource<String> {
        return try {
            val data = workout.toWriteMap() + mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val ref = db.collection(COLLECTION).add(data).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar treino", e)
            Resource.Error(e.message ?: "Erro inesperado ao salvar treino")
        }
    }

    override suspend fun updateWorkout(workout: WorkoutDto): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(workout.id).update(
                workout.toWriteMap() + mapOf("updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar treino ${workout.id}", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar treino")
        }
    }

    override suspend fun deleteWorkout(id: String): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir treino $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir treino")
        }
    }

    private fun WorkoutDto.toWriteMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "dayOfWeek" to dayOfWeek,
        "items" to items.map { it.toWriteMap() },
        "exerciseIds" to exerciseIds,
        "notes" to notes,
        "icon" to icon,
        "isOther" to isOther,
    )

    private fun WorkoutItemDto.toWriteMap(): Map<String, Any?> = mapOf(
        "exerciseId" to exerciseId,
        "order" to order,
        "sets" to sets,
        "reps" to reps,
        "loadKg" to loadKg,
        "restSeconds" to restSeconds,
        "notes" to notes,
    )
}
