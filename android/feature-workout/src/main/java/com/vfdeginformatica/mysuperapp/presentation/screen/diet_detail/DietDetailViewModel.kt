package com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.DeleteDietUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.GetDietUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.SaveDietUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DietDetailViewModel @Inject constructor(
    private val getDiet: GetDietUseCase,
    private val saveDiet: SaveDietUseCase,
    private val deleteDiet: DeleteDietUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DietDetailUiState())
    val uiState: StateFlow<DietDetailUiState> = _uiState
    private val effects = Channel<DietDetailEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()
    private var loadJob: Job? = null
    private var generation = 0

    init { load() }

    fun onEvent(event: DietDetailEvent) {
        if (event == DietDetailEvent.Retry) {
            if (!_uiState.value.isSaving) load()
            return
        }
        if (!_uiState.value.canEdit) return
        when (event) {
            DietDetailEvent.Retry -> Unit
            is DietDetailEvent.NotesChanged -> edit { it.copy(notes = event.notes) }
            DietDetailEvent.AddMeal -> edit {
                it.copy(meals = it.meals + DietMeal(UUID.randomUUID().toString(), "Refeição ${it.meals.size + 1}"))
            }
            is DietDetailEvent.MealChanged -> changeMeal(event.meal.id) { event.meal.copy(foods = it.foods) }
            is DietDetailEvent.RemoveMeal -> edit { it.copy(meals = it.meals.filterNot { meal -> meal.id == event.id }) }
            is DietDetailEvent.MoveMeal -> edit {
                it.copy(meals = it.meals.moved(it.meals.indexOfFirst { meal -> meal.id == event.id }, event.offset))
            }
            is DietDetailEvent.AddFood -> changeMeal(event.mealId) {
                it.copy(foods = it.foods + DietFood(UUID.randomUUID().toString(), "", ""))
            }
            is DietDetailEvent.FoodChanged -> changeMeal(event.mealId) { meal ->
                meal.copy(foods = meal.foods.map { if (it.id == event.food.id) event.food else it })
            }
            is DietDetailEvent.RemoveFood -> changeMeal(event.mealId) {
                it.copy(foods = it.foods.filterNot { food -> food.id == event.foodId })
            }
            is DietDetailEvent.MoveFood -> changeMeal(event.mealId) { meal ->
                meal.copy(foods = meal.foods.moved(meal.foods.indexOfFirst { it.id == event.foodId }, event.offset))
            }
            DietDetailEvent.Save -> write(delete = false)
            DietDetailEvent.DeleteClicked -> if (_uiState.value.exists) {
                _uiState.update { it.copy(showDeleteConfirmation = true) }
            }
            DietDetailEvent.DeleteDismissed -> _uiState.update { it.copy(showDeleteConfirmation = false) }
            DietDetailEvent.DeleteConfirmed -> if (_uiState.value.showDeleteConfirmation) write(delete = true)
        }
    }

    private fun edit(transform: (Diet) -> Diet) {
        _uiState.update { it.copy(diet = transform(it.diet)) }
    }

    private fun changeMeal(id: String, transform: (DietMeal) -> DietMeal) =
        edit { it.copy(meals = it.meals.map { meal -> if (meal.id == id) transform(meal) else meal }) }

    private fun <T> List<T>.moved(from: Int, offset: Int): List<T> {
        val to = from + offset
        if (from !in indices || to !in indices) return this
        return toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getDiet().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        generation++
                        _uiState.value = DietDetailUiState()
                    }
                    is Resource.Success -> {
                        val record = result.data
                        _uiState.value = if (record == null) {
                            DietDetailUiState(isLoading = false, error = "Resposta inválida ao carregar dieta")
                        } else DietDetailUiState(
                            isLoading = false, ownerId = record.ownerId,
                            diet = record.diet ?: Diet(),
                            exists = record.diet != null,
                        )
                    }
                    is Resource.Error -> _uiState.value = DietDetailUiState(
                        isLoading = false, error = result.message ?: "Erro ao carregar dieta",
                    )
                }
            }
        }
    }

    private fun write(delete: Boolean) {
        val state = _uiState.value
        val owner = state.ownerId ?: return
        val expectedGeneration = generation
        _uiState.update { it.copy(isSaving = true, showDeleteConfirmation = false) }
        viewModelScope.launch {
            val operation = if (delete) deleteDiet(owner) else saveDiet(owner, state.diet)
            operation.collect { result ->
                if (generation != expectedGeneration) return@collect
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        effects.send(DietDetailEffect.NavigateUp)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false) }
                        effects.send(DietDetailEffect.ShowMessage(result.message ?: "Erro ao salvar dieta"))
                    }
                }
            }
        }
    }
}
