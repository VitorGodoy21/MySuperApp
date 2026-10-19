package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.ExerciseDto
import kotlinx.coroutines.tasks.await

class ExerciseDaoImpl(
    private val db: FirebaseFirestore
) : ExerciseDao {

    private companion object {
        const val COLLECTION = "exercises"
        const val TAG = "Firebase"
    }

    override suspend fun getExercises(): List<ExerciseDto>? {
        return try {
            val snap = db.collection(COLLECTION).get().await()
            snap.documents.mapNotNull { it.toObject(ExerciseDto::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar exercícios", e)
            null
        }
    }

    override suspend fun addExercise(exercise: ExerciseDto): Resource<String> {
        return try {
            val data = mapOf(
                "name" to exercise.name,
                "muscleGroup" to exercise.muscleGroup,
                "description" to exercise.description,
                "notes" to exercise.notes,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            val ref = db.collection(COLLECTION).add(data).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar exercício", e)
            Resource.Error(e.message ?: "Erro inesperado ao salvar exercício")
        }
    }

    override suspend fun updateExercise(exercise: ExerciseDto): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(exercise.id).update(
                mapOf(
                    "name" to exercise.name,
                    "muscleGroup" to exercise.muscleGroup,
                    "description" to exercise.description,
                    "notes" to exercise.notes,
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar exercício ${exercise.id}", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar exercício")
        }
    }

    override suspend fun deleteExercise(id: String): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir exercício $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir exercício")
        }
    }
}
