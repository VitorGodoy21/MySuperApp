package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.GetBodyMeasurementsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutHubViewModel @Inject constructor(
    private val getBodyMeasurementsUseCase: GetBodyMeasurementsUseCase,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutHubUiState())
    val uiState: StateFlow<WorkoutHubUiState> = _uiState

    private val _effect = MutableSharedFlow<WorkoutHubEffect>()
    val effect: SharedFlow<WorkoutHubEffect> = _effect

    private var loadJob: Job? = null

    fun onEvent(event: WorkoutHubEvent) {
        when (event) {
            WorkoutHubEvent.Reload -> load()
            WorkoutHubEvent.BodyMeasurementsClicked -> emit(WorkoutHubEffect.NavigateToBodyMeasurements)
            WorkoutHubEvent.ExercisesClicked -> emit(WorkoutHubEffect.NavigateToExercises)
            WorkoutHubEvent.WorkoutsClicked -> emit(WorkoutHubEffect.NavigateToWorkouts)
            is WorkoutHubEvent.WorkoutClicked -> emit(WorkoutHubEffect.NavigateToWorkout(event.workoutId))
        }
    }

    private fun emit(effect: WorkoutHubEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            launch {
                getBodyMeasurementsUseCase().collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(latestMeasurement = result.data?.firstOrNull()) }
                    }
                }
            }
            launch {
                getExercisesUseCase().collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(exerciseCount = result.data.orEmpty().size) }
                    }
                }
            }
            launch {
                getWorkoutsUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> _uiState.update {
                            it.copy(isLoading = false, workouts = result.data.orEmpty())
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false) }
                            _effect.emit(
                                WorkoutHubEffect.ShowToast(result.message ?: "Erro ao carregar treinos")
                            )
                        }
                        is Resource.Loading -> Unit
                    }
                }
            }
        }
    }
}
