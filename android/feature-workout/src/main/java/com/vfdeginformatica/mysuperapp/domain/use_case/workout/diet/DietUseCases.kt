package com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.model.DietRecord
import com.vfdeginformatica.mysuperapp.domain.repository.DietRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.writeFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetDietUseCase @Inject constructor(private val repository: DietRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Resource<DietRecord>> = repository.observeOwner().flatMapLatest {
        flow {
            emit(Resource.Loading())
            emit(repository.getDiet())
        }
    }
}

fun DietMeal.validationError(): String? = when {
    name.isBlank() -> "Informe o nome da refeição"
    time.isNotBlank() && !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(time.trim()) ->
        "Informe um horário válido (HH:mm)"
    else -> null
}

fun DietFood.validationError(): String? = when {
    name.isBlank() -> "Informe o nome do alimento"
    quantity.isBlank() -> "Informe a quantidade do alimento"
    else -> null
}

class SaveDietUseCase @Inject constructor(private val repository: DietRepository) {
    operator fun invoke(ownerId: String, diet: Diet): Flow<Resource<Unit>> = flow {
        val error = diet.meals.firstNotNullOfOrNull { meal ->
            meal.validationError() ?: meal.foods.firstNotNullOfOrNull { it.validationError() }
        }
        if (error != null) {
            emit(Resource.Error(error))
            return@flow
        }
        val normalized = diet.copy(
            notes = diet.notes.trim(),
            meals = diet.meals.map { meal ->
                meal.copy(
                    name = meal.name.trim(), time = meal.time.trim(), notes = meal.notes.trim(),
                    foods = meal.foods.map { it.copy(name = it.name.trim(), quantity = it.quantity.trim()) },
                )
            },
        )
        writeFlow("Erro ao salvar dieta") { repository.saveDiet(ownerId, normalized) }
            .collect { emit(it) }
    }
}

class DeleteDietUseCase @Inject constructor(private val repository: DietRepository) {
    operator fun invoke(ownerId: String): Flow<Resource<Unit>> =
        writeFlow("Erro ao excluir dieta") { repository.deleteDiet(ownerId) }
}
