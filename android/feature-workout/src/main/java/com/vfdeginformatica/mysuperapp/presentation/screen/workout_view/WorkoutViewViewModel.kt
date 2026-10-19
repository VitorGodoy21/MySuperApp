package com.vfdeginformatica.mysuperapp.presentation.screen.workout_view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.AddWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.DeleteWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutByIdUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.SaveWorkoutUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.applyDraft
import com.vfdeginformatica.mysuperapp.presentation.common.sanitizeIntInput
import com.vfdeginformatica.mysuperapp.presentation.common.sanitizeItemDraft
import com.vfdeginformatica.mysuperapp.presentation.common.toItemDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.VideoPlayback
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.SessionDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Read-only view of a workout plus the log of gym sessions where it was performed. */
@HiltViewModel
class WorkoutViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWorkoutByIdUseCase: GetWorkoutByIdUseCase,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val getWorkoutSessionsUseCase: GetWorkoutSessionsUseCase,
    private val addWorkoutSessionUseCase: AddWorkoutSessionUseCase,
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
    private val saveWorkoutUseCase: SaveWorkoutUseCase,
) : ViewModel() {

    companion object {
        const val WORKOUT_ID_ARG = "workoutId"
        const val MAX_DURATION_MINUTES = 600
        const val MAX_CALORIES = 5000
        const val DATE_FUTURE_ERROR = "Data não pode ser futura"
        const val DURATION_INVALID_ERROR = "Tempo deve ser entre 1 e 600 min"
        const val CALORIES_INVALID_ERROR = "Calorias devem ser entre 1 e 5000"
    }

    private val workoutId: String = savedStateHandle.get<String>(WORKOUT_ID_ARG).orEmpty()

    private val _uiState = MutableStateFlow(WorkoutViewUiState(workoutId = workoutId))
    val uiState: StateFlow<WorkoutViewUiState> = _uiState

    private val _effect = MutableSharedFlow<WorkoutViewEffect>()
    val effect: SharedFlow<WorkoutViewEffect> = _effect

    private var loadJob: Job? = null
    private var sessionsJob: Job? = null

    init {
        load()
    }

    fun onEvent(event: WorkoutViewEvent) {
        when (event) {
            WorkoutViewEvent.Reload -> load()

            is WorkoutViewEvent.PlayVideoClicked -> {
                val row = _uiState.value.itemRows.getOrNull(event.index) ?: return
                val videoId = row.videoId ?: return
                _uiState.update {
                    it.copy(playingVideo = VideoPlayback(row.exerciseName, videoId))
                }
            }

            WorkoutViewEvent.VideoDialogDismissed -> _uiState.update { it.copy(playingVideo = null) }

            is WorkoutViewEvent.ItemClicked -> _uiState.update { state ->
                if (state.isBusy) return@update state
                val item = state.workout?.items?.getOrNull(event.index) ?: return@update state
                state.copy(editingItemIndex = event.index, itemDraft = item.toItemDraft())
            }

            is WorkoutViewEvent.ItemDraftChanged -> _uiState.update {
                if (it.itemDraft == null) it else it.copy(itemDraft = sanitizeItemDraft(event.draft))
            }

            WorkoutViewEvent.ItemDraftConfirmed -> saveItemDraft()

            WorkoutViewEvent.ItemDraftDismissed -> _uiState.update {
                if (it.isSavingItem) it else it.copy(editingItemIndex = null, itemDraft = null)
            }

            WorkoutViewEvent.RegisterSessionClicked -> _uiState.update {
                it.copy(sessionDraft = validate(SessionDraft()))
            }

            WorkoutViewEvent.SessionDialogDismissed -> _uiState.update { it.copy(sessionDraft = null) }

            is WorkoutViewEvent.SessionDateChanged -> updateDraft {
                it.copy(date = event.date, showDatePicker = false)
            }

            is WorkoutViewEvent.SessionDatePickerToggled -> updateDraft {
                it.copy(showDatePicker = event.visible)
            }

            is WorkoutViewEvent.SessionDurationChanged -> updateDraft {
                it.copy(durationText = sanitizeIntInput(event.text, maxDigits = 3))
            }

            is WorkoutViewEvent.SessionCaloriesChanged -> updateDraft {
                it.copy(caloriesText = sanitizeIntInput(event.text, maxDigits = 4))
            }

            WorkoutViewEvent.SessionSaveClicked -> saveSession()

            is WorkoutViewEvent.DeleteSessionClicked -> _uiState.update { state ->
                state.copy(sessionPendingDelete = state.sessions.find { it.id == event.sessionId })
            }

            WorkoutViewEvent.DeleteSessionDismissed -> _uiState.update {
                it.copy(sessionPendingDelete = null)
            }

            WorkoutViewEvent.DeleteSessionConfirmed -> {
                val session = _uiState.value.sessionPendingDelete ?: return
                _uiState.update { it.copy(sessionPendingDelete = null) }
                deleteSession(session.id)
            }
        }
    }

    private fun load() {
        if (workoutId.isBlank()) {
            viewModelScope.launch {
                _effect.emit(WorkoutViewEffect.ShowToast("Treino não encontrado"))
                _effect.emit(WorkoutViewEffect.NavigateUp)
            }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            launch {
                getExercisesUseCase().collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(availableExercises = result.data.orEmpty()) }
                    }
                }
            }
            launch {
                getWorkoutByIdUseCase(workoutId).collect { result ->
                    when (result) {
                        is Resource.Loading -> Unit
                        is Resource.Success -> _uiState.update {
                            it.copy(isLoading = false, workout = result.data)
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false) }
                            _effect.emit(
                                WorkoutViewEffect.ShowToast(result.message ?: "Erro ao carregar treino")
                            )
                            _effect.emit(WorkoutViewEffect.NavigateUp)
                        }
                    }
                }
            }
        }
        loadSessions()
    }

    private fun loadSessions() {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            getWorkoutSessionsUseCase(workoutId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSessionsLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isSessionsLoading = false, sessions = result.data.orEmpty())
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSessionsLoading = false) }
                        _effect.emit(
                            WorkoutViewEffect.ShowToast(result.message ?: "Erro ao carregar registros")
                        )
                    }
                }
            }
        }
    }

    private fun updateDraft(transform: (SessionDraft) -> SessionDraft) {
        _uiState.update { state ->
            val draft = state.sessionDraft ?: return@update state
            state.copy(sessionDraft = validate(transform(draft)))
        }
    }

    private fun validate(draft: SessionDraft): SessionDraft {
        val duration = draft.durationText.toIntOrNull()
        val calories = draft.caloriesText.toIntOrNull()
        return draft.copy(
            dateError = if (draft.date.isAfter(LocalDate.now())) DATE_FUTURE_ERROR else null,
            durationError = when {
                draft.durationText.isBlank() -> null
                duration == null || duration !in 1..MAX_DURATION_MINUTES -> DURATION_INVALID_ERROR
                else -> null
            },
            caloriesError = when {
                draft.caloriesText.isBlank() -> null
                calories == null || calories !in 1..MAX_CALORIES -> CALORIES_INVALID_ERROR
                else -> null
            },
        )
    }

    private fun saveSession() {
        val state = _uiState.value
        val draft = state.sessionDraft ?: return
        if (!draft.isValid || state.isBusy) return

        val session = WorkoutSession(
            workoutId = workoutId,
            date = draft.date,
            durationMinutes = draft.durationText.toIntOrNull(),
            calories = draft.caloriesText.toIntOrNull(),
        )

        viewModelScope.launch {
            addWorkoutSessionUseCase(session).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSavingSession = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isSavingSession = false, sessionDraft = null) }
                        _effect.emit(WorkoutViewEffect.ShowToast("Treino registrado!"))
                        loadSessions()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSavingSession = false) }
                        _effect.emit(
                            WorkoutViewEffect.ShowToast(result.message ?: "Erro ao registrar treino")
                        )
                    }
                }
            }
        }
    }

    private fun saveItemDraft() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val index = state.editingItemIndex ?: return
        val draft = state.itemDraft ?: return
        if (state.isBusy) return
        val item = workout.items.getOrNull(index) ?: run {
            _uiState.update { it.copy(editingItemIndex = null, itemDraft = null) }
            return
        }
        val updated = workout.copy(
            items = workout.items.toMutableList().also { it[index] = item.applyDraft(draft) }
        )

        viewModelScope.launch {
            saveWorkoutUseCase(updated).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSavingItem = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isSavingItem = false,
                            editingItemIndex = null,
                            itemDraft = null,
                            workout = updated,
                        )
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSavingItem = false) }
                        _effect.emit(
                            WorkoutViewEffect.ShowToast(result.message ?: "Erro ao salvar exercício")
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
                        _effect.emit(WorkoutViewEffect.ShowToast("Registro excluído"))
                        loadSessions()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeletingSession = false) }
                        _effect.emit(
                            WorkoutViewEffect.ShowToast(result.message ?: "Erro ao excluir registro")
                        )
                    }
                }
            }
        }
    }
}
