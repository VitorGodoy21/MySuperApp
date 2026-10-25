package com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.DeleteWorkoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutByIdUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.SaveWorkoutUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.applyDraft
import com.vfdeginformatica.mysuperapp.presentation.common.sanitizeItemDraft
import com.vfdeginformatica.mysuperapp.presentation.common.toItemDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.VideoPlayback
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWorkoutByIdUseCase: GetWorkoutByIdUseCase,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val saveWorkoutUseCase: SaveWorkoutUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
) : ViewModel() {

    companion object {
        const val WORKOUT_ID_ARG = "workoutId"
        const val MAX_NAME_LENGTH = 40
        const val MAX_NOTES_LENGTH = 200
        const val MAX_REPS_LENGTH = com.vfdeginformatica.mysuperapp.presentation.common.MAX_REPS_LENGTH
        const val MAX_ITEM_NOTES_LENGTH = com.vfdeginformatica.mysuperapp.presentation.common.MAX_ITEM_NOTES_LENGTH
        const val NAME_REQUIRED_ERROR = "Informe o nome do treino"
    }

    private val workoutId: String? =
        savedStateHandle.get<String>(WORKOUT_ID_ARG)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(WorkoutDetailUiState(workoutId = workoutId))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState

    private val _effect = MutableSharedFlow<WorkoutDetailEffect>()
    val effect: SharedFlow<WorkoutDetailEffect> = _effect

    init {
        loadExercises()
        if (workoutId != null) loadWorkout(workoutId)
    }

    fun onEvent(event: WorkoutDetailEvent) {
        when (event) {
            is WorkoutDetailEvent.NameChanged -> {
                val name = event.name.take(MAX_NAME_LENGTH)
                updateForm {
                    it.copy(
                        name = name,
                        nameError = if (name.isBlank() && it.name.isNotBlank()) NAME_REQUIRED_ERROR
                        else if (name.isNotBlank()) null else it.nameError,
                    )
                }
            }

            is WorkoutDetailEvent.DayChanged -> _uiState.update { it.copy(dayOfWeek = event.day) }

            is WorkoutDetailEvent.IconChanged -> _uiState.update { it.copy(icon = event.icon) }

            is WorkoutDetailEvent.NotesChanged -> _uiState.update {
                it.copy(notes = event.notes.take(MAX_NOTES_LENGTH))
            }

            WorkoutDetailEvent.AddExerciseClicked -> _uiState.update {
                it.copy(showExercisePicker = true, pickerQuery = "")
            }

            is WorkoutDetailEvent.PickerQueryChanged -> _uiState.update {
                it.copy(pickerQuery = event.query)
            }

            is WorkoutDetailEvent.ExercisePicked -> _uiState.update {
                it.copy(
                    items = reindex(it.items + WorkoutItem(event.exerciseId, order = it.items.size)),
                    showExercisePicker = false,
                    pickerQuery = "",
                )
            }

            WorkoutDetailEvent.PickerDismissed -> _uiState.update {
                it.copy(showExercisePicker = false, pickerQuery = "")
            }

            is WorkoutDetailEvent.RemoveItem -> _uiState.update {
                if (event.index !in it.items.indices) it
                else it.copy(items = reindex(it.items.filterIndexed { i, _ -> i != event.index }))
            }

            is WorkoutDetailEvent.MoveItemUp -> moveItem(event.index, event.index - 1)

            is WorkoutDetailEvent.MoveItemDown -> moveItem(event.index, event.index + 1)

            is WorkoutDetailEvent.EditItemClicked -> _uiState.update {
                val item = it.items.getOrNull(event.index) ?: return@update it
                it.copy(editingItemIndex = event.index, itemDraft = item.toItemDraft())
            }

            is WorkoutDetailEvent.ItemDraftChanged -> _uiState.update {
                if (it.itemDraft == null) it else it.copy(itemDraft = sanitizeItemDraft(event.draft))
            }

            WorkoutDetailEvent.ItemDraftConfirmed -> confirmItemDraft()

            WorkoutDetailEvent.ItemDraftDismissed -> _uiState.update {
                it.copy(editingItemIndex = null, itemDraft = null)
            }

            is WorkoutDetailEvent.PlayVideoClicked -> _uiState.update { state ->
                val row = state.itemRows.getOrNull(event.index)
                val videoId = row?.videoId ?: return@update state
                state.copy(playingVideo = VideoPlayback(row.exerciseName, videoId))
            }

            WorkoutDetailEvent.VideoDialogDismissed -> _uiState.update {
                it.copy(playingVideo = null)
            }

            WorkoutDetailEvent.SaveClicked -> save()

            WorkoutDetailEvent.DeleteClicked -> _uiState.update { it.copy(showDeleteConfirmation = true) }

            WorkoutDetailEvent.DeleteDismissed -> _uiState.update { it.copy(showDeleteConfirmation = false) }

            WorkoutDetailEvent.DeleteConfirmed -> {
                _uiState.update { it.copy(showDeleteConfirmation = false) }
                delete()
            }
        }
    }

    private fun updateForm(transform: (WorkoutDetailUiState) -> WorkoutDetailUiState) {
        _uiState.update { old ->
            val new = transform(old)
            new.copy(isSubmitEnabled = new.name.isNotBlank())
        }
    }

    private fun reindex(items: List<WorkoutItem>): List<WorkoutItem> =
        items.mapIndexed { index, item -> item.copy(order = index) }

    private fun moveItem(from: Int, to: Int) {
        _uiState.update { state ->
            if (from !in state.items.indices || to !in state.items.indices) return@update state
            val mutable = state.items.toMutableList()
            val tmp = mutable[from]
            mutable[from] = mutable[to]
            mutable[to] = tmp
            state.copy(items = reindex(mutable))
        }
    }

    private fun confirmItemDraft() {
        _uiState.update { state ->
            val index = state.editingItemIndex ?: return@update state
            val draft = state.itemDraft ?: return@update state
            val item = state.items.getOrNull(index) ?: return@update state.copy(
                editingItemIndex = null, itemDraft = null
            )
            val updated = item.applyDraft(draft)
            state.copy(
                items = state.items.toMutableList().also { it[index] = updated },
                editingItemIndex = null,
                itemDraft = null,
            )
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            getExercisesUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update {
                        it.copy(availableExercises = result.data.orEmpty())
                    }

                    is Resource.Error -> _effect.emit(
                        WorkoutDetailEffect.ShowToast(result.message ?: "Erro ao carregar exercícios")
                    )

                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private fun loadWorkout(id: String) {
        viewModelScope.launch {
            getWorkoutByIdUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> {
                        val workout = result.data
                        if (workout == null) {
                            _uiState.update { it.copy(isLoading = false) }
                            _effect.emit(WorkoutDetailEffect.ShowToast("Treino não encontrado"))
                        } else {
                            updateForm {
                                it.copy(
                                    isLoading = false,
                                    name = workout.name,
                                    dayOfWeek = workout.dayOfWeek,
                                    icon = workout.icon,
                                    notes = workout.notes,
                                    items = reindex(workout.items),
                                    nameError = null,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            WorkoutDetailEffect.ShowToast(result.message ?: "Erro ao carregar treino")
                        )
                    }
                }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isBusy) return
        if (!state.isSubmitEnabled) {
            _uiState.update { it.copy(nameError = NAME_REQUIRED_ERROR) }
            return
        }

        val workout = Workout(
            id = state.workoutId.orEmpty(),
            name = state.name.trim(),
            dayOfWeek = state.dayOfWeek,
            icon = state.icon,
            items = reindex(state.items),
            notes = state.notes.trim(),
        )

        viewModelScope.launch {
            saveWorkoutUseCase(workout).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(WorkoutDetailEffect.ShowToast("Treino salvo!"))
                        _effect.emit(WorkoutDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(
                            WorkoutDetailEffect.ShowToast(result.message ?: "Erro ao salvar treino")
                        )
                    }
                }
            }
        }
    }

    private fun delete() {
        val id = _uiState.value.workoutId ?: return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            deleteWorkoutUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isDeleting = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(WorkoutDetailEffect.ShowToast("Treino excluído"))
                        _effect.emit(WorkoutDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(
                            WorkoutDetailEffect.ShowToast(result.message ?: "Erro ao excluir treino")
                        )
                    }
                }
            }
        }
    }
}
