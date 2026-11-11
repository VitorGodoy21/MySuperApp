package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.RecipeDao
import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeRepository

class RecipeRepositoryImpl(private val dao: RecipeDao) : RecipeRepository {
    override fun observeUserId() = dao.observeUserId()
    override fun currentUserId() = dao.currentUserId()
    override suspend fun list(userId: String) = dao.list(userId).map { it.toModel() }
    override suspend fun get(userId: String, id: String) = dao.get(userId, id)?.toModel()
    override suspend fun save(recipe: Recipe) = dao.save(recipe.toDto())
    override suspend fun delete(userId: String, id: String) = dao.delete(userId, id)
    override suspend fun favorite(userId: String, id: String, value: Boolean) = dao.favorite(userId, id, value)
}
