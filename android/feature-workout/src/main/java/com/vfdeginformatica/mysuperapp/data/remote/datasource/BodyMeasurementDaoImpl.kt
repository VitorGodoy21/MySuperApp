package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.BodyMeasurementDto
import kotlinx.coroutines.tasks.await

class BodyMeasurementDaoImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : BodyMeasurementDao {

    private companion object {
        const val COLLECTION = "body_measurements"
        const val TAG = "Firebase"
    }

    private fun currentUserId(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    override suspend fun getMeasurements(): List<BodyMeasurementDto>? {
        return try {
            val userId = currentUserId() ?: return emptyList()
            val snap = db.collection(COLLECTION).whereEqualTo("userId", userId).get().await()
            snap.documents.mapNotNull { it.toObject(BodyMeasurementDto::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar medidas", e)
            null
        }
    }

    override suspend fun addMeasurement(measurement: BodyMeasurementDto): Resource<String> {
        return try {
            val userId = checkNotNull(currentUserId()) { "Entre na sua conta para registrar medidas" }
            val data = mapOf(
                "userId" to userId,
                "weightKg" to measurement.weightKg,
                "heightCm" to measurement.heightCm,
                "measuredAt" to measurement.measuredAt,
                "notes" to measurement.notes,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            val ref = db.collection(COLLECTION).add(data).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar medida", e)
            Resource.Error(e.message ?: "Erro inesperado ao salvar medida")
        }
    }

    override suspend fun updateMeasurement(measurement: BodyMeasurementDto): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(measurement.id).update(
                mapOf(
                    "weightKg" to measurement.weightKg,
                    "heightCm" to measurement.heightCm,
                    "measuredAt" to measurement.measuredAt,
                    "notes" to measurement.notes,
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar medida ${measurement.id}", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar medida")
        }
    }

    override suspend fun deleteMeasurement(id: String): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir medida $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir medida")
        }
    }
}
