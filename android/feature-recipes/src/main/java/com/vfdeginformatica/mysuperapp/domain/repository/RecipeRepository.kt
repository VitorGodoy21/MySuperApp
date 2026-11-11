package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeUserId(): Flow<String?>
    fun currentUserId(): String?
    suspend fun list(userId: String): List<Recipe>
    suspend fun get(userId: String, id: String): Recipe?
    suspend fun save(recipe: Recipe)
    suspend fun delete(userId: String, id: String)
    suspend fun favorite(userId: String, id: String, value: Boolean)
}

interface RecipeImageRepository {
    suspend fun upload(userId: String, recipeId: String, bytes: ByteArray): String
    suspend fun read(userId: String, path: String): ByteArray
    suspend fun delete(userId: String, path: String)
}

interface RecipePhotoCompressor {
    suspend fun compress(uri: String): ByteArray
}
