package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.RecipeDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class RecipeDaoImpl(private val db: FirebaseFirestore, private val auth: FirebaseAuth) : RecipeDao {
    private val collection get() = db.collection("recipes")

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun observeUserId() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    private fun requireAccount(userId: String) {
        check(userId.isNotBlank() && currentUserId() == userId) { "Entre na sua conta e abra a receita novamente" }
    }

    override suspend fun list(userId: String): List<RecipeDto> {
        requireAccount(userId)
        val result = collection.whereEqualTo("userId", userId).get().await()
            .documents.mapNotNull { it.toObject(RecipeDto::class.java) }
        requireAccount(userId)
        return result
    }

    override suspend fun get(userId: String, id: String): RecipeDto? {
        requireAccount(userId)
        val result = collection.document(id).get().await().toObject(RecipeDto::class.java)
        requireAccount(userId)
        check(result == null || result.userId == userId) { "Receita não encontrada" }
        return result
    }

    override suspend fun save(recipe: RecipeDto) {
        requireAccount(recipe.userId)
        val ref = collection.document(recipe.id)
        db.runTransaction { transaction ->
            requireAccount(recipe.userId)
            val existing = transaction.get(ref)
            check(!existing.exists() || existing.getString("userId") == recipe.userId) { "Receita não encontrada" }
            val data = mutableMapOf<String, Any?>(
                "userId" to recipe.userId, "name" to recipe.name, "description" to recipe.description,
                "ingredients" to recipe.ingredients.map { mapOf("name" to it.name, "quantity" to it.quantity) },
                "steps" to recipe.steps, "prepMinutes" to recipe.prepMinutes, "cookMinutes" to recipe.cookMinutes,
                "yield" to recipe.yield, "category" to recipe.category, "difficulty" to recipe.difficulty,
                "notes" to recipe.notes, "isFavorite" to recipe.isFavorite, "imagePath" to recipe.imagePath,
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to if (existing.exists()) existing.getTimestamp("createdAt") else FieldValue.serverTimestamp(),
            )
            transaction.set(ref, data)
        }.await()
    }

    override suspend fun delete(userId: String, id: String) {
        requireAccount(userId)
        checkNotNull(get(userId, id)) { "Receita não encontrada" }
        requireAccount(userId)
        collection.document(id).delete().await()
    }

    override suspend fun favorite(userId: String, id: String, value: Boolean) {
        requireAccount(userId)
        checkNotNull(get(userId, id)) { "Receita não encontrada" }
        requireAccount(userId)
        collection.document(id).update(mapOf("isFavorite" to value, "updatedAt" to FieldValue.serverTimestamp())).await()
    }
}
