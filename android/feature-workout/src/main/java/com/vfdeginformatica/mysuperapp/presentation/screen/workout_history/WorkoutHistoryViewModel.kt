package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.DeleteWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryUiState
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
class WorkoutHistoryViewModel @Inject constructor(
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val getRecentWorkoutSessionsUseCase: GetRecentWorkoutSessionsUseCase,
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
) : ViewModel() {

    companion object {
        const val WORKOUT_REMOVED_MESSAGE = "Este treino foi removido"
    }

    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState

    private val _effect = MutableSharedFlow<WorkoutHistoryEffect>()
    val effect: SharedFlow<WorkoutHistoryEffect> = _effect

    private var loadJob: Job? = null
    private var sessionsJob: Job? = null

    fun onEvent(event: WorkoutHistoryEvent) {
        when (event) {
            WorkoutHistoryEvent.Reload -> {
                loadWorkouts()
                loadSessions()
            }

            is WorkoutHistoryEvent.SessionClicked -> {
                val state = _uiState.value
                val session = state.sessions.find { it.id == event.sessionId } ?: return
                val exists = state.workouts.any { it.id == session.workoutId }
                viewModelScope.launch {
                    if (exists) _effect.emit(WorkoutHistoryEffect.NavigateToWorkout(session.workoutId))
                    else _effect.emit(WorkoutHistoryEffect.ShowToast(WORKOUT_REMOVED_MESSAGE))
                }
            }

            is WorkoutHistoryEvent.DeleteSessionClicked -> _uiState.update { state ->
                if (state.isBusy) state
                else state.copy(sessionPendingDelete = state.sessions.find { it.id == event.sessionId })
            }

            WorkoutHistoryEvent.DeleteSessionDismissed -> _uiState.update { it.copy(sessionPendingDelete = null) }

            WorkoutHistoryEvent.DeleteSessionConfirmed -> {
                val session = _uiState.value.sessionPendingDelete ?: return
                _uiState.update { it.copy(sessionPendingDelete = null) }
                deleteSession(session.id)
            }
        }
    }

    private fun loadWorkouts() {
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
                            WorkoutHistoryEffect.ShowToast(result.message ?: "Erro ao carregar treinos")
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
                            WorkoutHistoryEffect.ShowToast(result.message ?: "Erro ao carregar histórico")
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
                        _effect.emit(WorkoutHistoryEffect.ShowToast("Registro excluído"))
                        loadSessions()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeletingSession = false) }
                        _effect.emit(
                            WorkoutHistoryEffect.ShowToast(result.message ?: "Erro ao excluir registro")
                        )
                    }
                }
            }
        }
    }
}
