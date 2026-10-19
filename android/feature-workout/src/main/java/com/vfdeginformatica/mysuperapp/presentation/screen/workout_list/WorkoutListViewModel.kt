package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract.WorkoutListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutListViewModel @Inject constructor(
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val getExercisesUseCase: GetExercisesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutListUiState())
    val uiState: StateFlow<WorkoutListUiState> = _uiState

    private val _effect = MutableSharedFlow<WorkoutListEffect>()
    val effect: SharedFlow<WorkoutListEffect> = _effect

    fun onEvent(event: WorkoutListEvent) {
        when (event) {
            WorkoutListEvent.Reload -> {
                loadWorkouts()
                loadExercises()
            }

            is WorkoutListEvent.WorkoutClicked -> viewModelScope.launch {
                _effect.emit(WorkoutListEffect.NavigateToDetail(event.workoutId))
            }

            WorkoutListEvent.NewClicked -> viewModelScope.launch {
                _effect.emit(WorkoutListEffect.NavigateToDetail(null))
            }
        }
    }

    private fun loadWorkouts() {
        viewModelScope.launch {
            getWorkoutsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            workouts = result.data.orEmpty(),
                            errorMessage = null,
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                        _effect.emit(
                            WorkoutListEffect.ShowToast(result.message ?: "Erro ao carregar treinos")
                        )
                    }
                }
            }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            getExercisesUseCase().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(exercises = result.data.orEmpty()) }
                }
            }
        }
    }
}
