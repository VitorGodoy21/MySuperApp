package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.data.remote.dto.RecipeDto
import kotlinx.coroutines.flow.Flow

interface RecipeDao {
    fun observeUserId(): Flow<String?>
    fun currentUserId(): String?
    suspend fun list(userId: String): List<RecipeDto>
    suspend fun get(userId: String, id: String): RecipeDto?
    suspend fun save(recipe: RecipeDto)
    suspend fun delete(userId: String, id: String)
    suspend fun favorite(userId: String, id: String, value: Boolean)
}
