package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract.ExerciseListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val getExercisesUseCase: GetExercisesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseListUiState())
    val uiState: StateFlow<ExerciseListUiState> = _uiState

    private val _effect = MutableSharedFlow<ExerciseListEffect>()
    val effect: SharedFlow<ExerciseListEffect> = _effect

    init {
        loadExercises()
    }

    fun onEvent(event: ExerciseListEvent) {
        when (event) {
            ExerciseListEvent.Reload -> loadExercises()

            is ExerciseListEvent.QueryChanged -> _uiState.update { it.copy(query = event.query) }

            is ExerciseListEvent.ExerciseClicked -> viewModelScope.launch {
                _effect.emit(ExerciseListEffect.NavigateToDetail(event.exerciseId))
            }

            ExerciseListEvent.NewClicked -> viewModelScope.launch {
                _effect.emit(ExerciseListEffect.NavigateToDetail(null))
            }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            getExercisesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            exercises = result.data.orEmpty(),
                            errorMessage = null,
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                        _effect.emit(
                            ExerciseListEffect.ShowToast(
                                result.message ?: "Erro ao carregar exercícios"
                            )
                        )
                    }
                }
            }
        }
    }
}
