package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.DeleteWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
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
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val getRecentWorkoutSessionsUseCase: GetRecentWorkoutSessionsUseCase,
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
) : ViewModel() {

    companion object {
        const val WORKOUT_REMOVED_MESSAGE = "Este treino foi removido"
    }

    private val _uiState = MutableStateFlow(WorkoutHubUiState())
    val uiState: StateFlow<WorkoutHubUiState> = _uiState

    private val _effect = MutableSharedFlow<WorkoutHubEffect>()
    val effect: SharedFlow<WorkoutHubEffect> = _effect

    private var loadJob: Job? = null
    private var sessionsJob: Job? = null

    fun onEvent(event: WorkoutHubEvent) {
        when (event) {
            WorkoutHubEvent.DietClicked -> viewModelScope.launch {
                _effect.emit(WorkoutHubEffect.NavigateToDiet)
            }

            WorkoutHubEvent.Reload -> {
                load()
                loadSessions()
            }

            is WorkoutHubEvent.WorkoutClicked -> viewModelScope.launch {
                _effect.emit(WorkoutHubEffect.NavigateToWorkout(event.workoutId))
            }

            is WorkoutHubEvent.SessionClicked -> {
                val state = _uiState.value
                val session = state.sessions.find { it.id == event.sessionId } ?: return
                val exists = state.workouts.any { it.id == session.workoutId }
                viewModelScope.launch {
                    if (exists) _effect.emit(WorkoutHubEffect.NavigateToWorkout(session.workoutId))
                    else _effect.emit(WorkoutHubEffect.ShowToast(WORKOUT_REMOVED_MESSAGE))
                }
            }

            is WorkoutHubEvent.DeleteSessionClicked -> _uiState.update { state ->
                if (state.isBusy) state
                else state.copy(sessionPendingDelete = state.sessions.find { it.id == event.sessionId })
            }

            WorkoutHubEvent.DeleteSessionDismissed -> _uiState.update { it.copy(sessionPendingDelete = null) }

            WorkoutHubEvent.DeleteSessionConfirmed -> {
                val session = _uiState.value.sessionPendingDelete ?: return
                _uiState.update { it.copy(sessionPendingDelete = null) }
                deleteSession(session.id)
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getWorkoutsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, workouts = result.data.orEmpty())
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            WorkoutHubEffect.ShowToast(result.message ?: "Erro ao carregar treinos")
                        )
                    }
                }
            }
        }
    }

    private fun loadSessions() {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            getRecentWorkoutSessionsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSessionsLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isSessionsLoading = false, sessions = result.data.orEmpty())
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSessionsLoading = false) }
                        _effect.emit(
                            WorkoutHubEffect.ShowToast(result.message ?: "Erro ao carregar histórico")
                        )
                    }
                }
            }
        }
    }

    private fun deleteSession(id: String) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            deleteWorkoutSessionUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isDeletingSession = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isDeletingSession = false) }
                        _effect.emit(WorkoutHubEffect.ShowToast("Registro excluído"))
                        loadSessions()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeletingSession = false) }
                        _effect.emit(
                            WorkoutHubEffect.ShowToast(result.message ?: "Erro ao excluir registro")
                        )
                    }
                }
            }
        }
    }
}
