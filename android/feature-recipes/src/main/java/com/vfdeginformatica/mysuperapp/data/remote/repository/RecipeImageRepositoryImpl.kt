package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RecipeImageRepositoryImpl(private val storage: FirebaseStorage, private val auth: FirebaseAuth) : RecipeImageRepository {
    private fun checkPath(userId: String, path: String) {
        check(userId.isNotBlank() && auth.currentUser?.uid == userId) { "Sua conta mudou" }
        require(path.startsWith("recipe_images/$userId/") && !path.contains("..")) { "Imagem inválida" }
    }

    override suspend fun upload(userId: String, recipeId: String, bytes: ByteArray): String {
        require(bytes.isNotEmpty() && bytes.size < MAX_BYTES) { "A foto deve ter menos de 5 MB" }
        require(recipeId.isNotBlank() && !recipeId.contains('/'))
        val path = "recipe_images/$userId/$recipeId/${UUID.randomUUID()}.jpg"
        checkPath(userId, path)
        storage.reference.child(path).putBytes(
            bytes, StorageMetadata.Builder().setContentType("image/jpeg").build(),
        ).await()
        return path
    }

    override suspend fun read(userId: String, path: String): ByteArray {
        checkPath(userId, path)
        val bytes = storage.reference.child(path).getBytes(MAX_BYTES.toLong()).await()
        checkPath(userId, path)
        return bytes
    }

    override suspend fun delete(userId: String, path: String) {
        checkPath(userId, path)
        storage.reference.child(path).delete().await()
    }

    companion object { const val MAX_BYTES = 5 * 1024 * 1024 }
}
