package com.vfdeginformatica.mysuperapp.domain.use_case.recipes

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface RecipePhotoChange {
    data object Keep : RecipePhotoChange
    data object Remove : RecipePhotoChange
    data class Replace(val bytes: ByteArray) : RecipePhotoChange
}

class GetRecipesUseCase @Inject constructor(private val repository: RecipeRepository) {
    operator fun invoke(userId: String) = recipeFlow { repository.list(userId) }
}

class GetRecipeUseCase @Inject constructor(private val repository: RecipeRepository) {
    operator fun invoke(userId: String, id: String) = recipeFlow {
        checkNotNull(repository.get(userId, id)) { "Receita não encontrada" }
    }
}

class SaveRecipeUseCase @Inject constructor(
    private val repository: RecipeRepository,
    private val images: RecipeImageRepository,
) {
    operator fun invoke(recipe: Recipe, photo: RecipePhotoChange): Flow<Resource<Recipe>> = recipeFlow {
        val normalized = recipe.normalized()
        normalized.validationError()?.let { error(it) }
        check(repository.currentUserId() == recipe.userId && recipe.userId.isNotBlank()) { "Sua conta mudou. Abra a receita novamente" }
        var uploaded: String? = null
        var writeStarted = false
        val saved = try {
            val path = when (photo) {
                RecipePhotoChange.Keep -> recipe.imagePath
                RecipePhotoChange.Remove -> null
                is RecipePhotoChange.Replace -> images.upload(recipe.userId, recipe.id, photo.bytes).also { uploaded = it }
            }
            normalized.copy(imagePath = path).also {
                check(repository.currentUserId() == recipe.userId) { "Sua conta mudou" }
                writeStarted = true
                repository.save(it)
            }
        } catch (e: Exception) {
            // A cancelled await does not guarantee that the server-side write was cancelled.
            if (e !is CancellationException || !writeStarted) {
                uploaded?.let { path -> withContext(NonCancellable) { bestEffort { images.delete(recipe.userId, path) } } }
            }
            throw e
        }
        if (recipe.imagePath != null && recipe.imagePath != saved.imagePath) {
            withContext(NonCancellable) { bestEffort { images.delete(recipe.userId, recipe.imagePath) } }
        }
        saved
    }
}

class DeleteRecipeUseCase @Inject constructor(
    private val repository: RecipeRepository,
    private val images: RecipeImageRepository,
) {
    operator fun invoke(recipe: Recipe) = recipeFlow {
        repository.delete(recipe.userId, recipe.id)
        recipe.imagePath?.let { path ->
            withContext(NonCancellable) { bestEffort { images.delete(recipe.userId, path) } }
        }
    }
}

class FavoriteRecipeUseCase @Inject constructor(private val repository: RecipeRepository) {
    operator fun invoke(recipe: Recipe) = recipeFlow {
        repository.favorite(recipe.userId, recipe.id, !recipe.isFavorite)
    }
}

private suspend fun bestEffort(block: suspend () -> Unit) {
    try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { }
}

private fun <T> recipeFlow(block: suspend () -> T): Flow<Resource<T>> = flow {
    emit(Resource.Loading())
    try {
        emit(Resource.Success(block()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emit(Resource.Error(e.message ?: "Não foi possível concluir a operação"))
    }
}
