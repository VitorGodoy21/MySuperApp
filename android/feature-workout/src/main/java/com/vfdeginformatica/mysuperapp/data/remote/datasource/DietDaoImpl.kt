package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.DietDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.DietRecordDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class DietDaoImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : DietDao {
    override fun observeOwner(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun getDiet(): Resource<DietRecordDto> = operation("Erro ao carregar dieta") {
        val ownerId = requireOwner()
        val snapshot = db.collection("diets").document(ownerId).get().await()
        checkOwner(ownerId)
        val diet = if (snapshot.exists()) {
            checkNotNull(snapshot.toObject(DietDto::class.java)) { "Dieta inválida" }
        } else null
        DietRecordDto(ownerId, diet)
    }

    override suspend fun saveDiet(ownerId: String, diet: DietDto): Resource<Unit> =
        operation("Erro ao salvar dieta") {
            checkOwner(ownerId)
            db.collection("diets").document(ownerId).set(
                mapOf(
                    "notes" to diet.notes,
                    "meals" to diet.meals.map { meal ->
                        mapOf(
                            "id" to meal.id,
                            "name" to meal.name,
                            "time" to meal.time,
                            "notes" to meal.notes,
                            "foods" to meal.foods.map { food ->
                                mapOf("id" to food.id, "name" to food.name, "quantity" to food.quantity)
                            },
                        )
                    },
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            checkOwner(ownerId)
        }

    override suspend fun deleteDiet(ownerId: String): Resource<Unit> =
        operation("Erro ao excluir dieta") {
            checkOwner(ownerId)
            db.collection("diets").document(ownerId).delete().await()
            checkOwner(ownerId)
        }

    private fun requireOwner(): String =
        checkNotNull(auth.currentUser?.uid) { "Entre na sua conta para acessar a dieta" }

    private fun checkOwner(ownerId: String) {
        check(ownerId.isNotBlank() && requireOwner() == ownerId) {
            "A conta mudou. Abra a dieta novamente."
        }
    }

    private suspend fun <T> operation(message: String, block: suspend () -> T): Resource<T> =
        try {
            Resource.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("DietDao", message, e)
            Resource.Error("$message: ${e.message ?: "tente novamente"}")
        }
}
