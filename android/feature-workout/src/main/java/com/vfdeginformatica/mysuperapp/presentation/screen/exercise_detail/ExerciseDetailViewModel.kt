package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.AddExerciseUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.DeleteExerciseUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetWorkoutsUsingExerciseUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.UpdateExerciseUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract.ExerciseDetailEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract.ExerciseDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract.ExerciseDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val addExerciseUseCase: AddExerciseUseCase,
    private val updateExerciseUseCase: UpdateExerciseUseCase,
    private val getWorkoutsUsingExerciseUseCase: GetWorkoutsUsingExerciseUseCase,
    private val deleteExerciseUseCase: DeleteExerciseUseCase,
) : ViewModel() {

    companion object {
        const val EXERCISE_ID_ARG = "exerciseId"
        const val MAX_NAME_LENGTH = 60
        const val MAX_DESCRIPTION_LENGTH = 200
        const val MAX_NOTES_LENGTH = 200
        const val NAME_REQUIRED_ERROR = "Informe o nome do exercício"
        const val DUPLICATE_WARNING = "Já existe um exercício com esse nome"
    }

    private val exerciseId: String? =
        savedStateHandle.get<String>(EXERCISE_ID_ARG)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(ExerciseDetailUiState(exerciseId = exerciseId))
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    private val _effect = MutableSharedFlow<ExerciseDetailEffect>()
    val effect: SharedFlow<ExerciseDetailEffect> = _effect

    /** All exercises, used for the duplicate-name check. */
    private var knownExercises: List<Exercise> = emptyList()

    init {
        loadExercises()
        if (exerciseId != null) loadUsage(exerciseId)
    }

    fun onEvent(event: ExerciseDetailEvent) {
        when (event) {
            is ExerciseDetailEvent.NameChanged -> {
                val name = event.name.take(MAX_NAME_LENGTH)
                updateForm {
                    it.copy(
                        name = name,
                        nameError = if (name.isBlank()) NAME_REQUIRED_ERROR else null,
                    )
                }
            }

            is ExerciseDetailEvent.MuscleGroupChanged -> updateForm {
                it.copy(muscleGroup = event.muscleGroup)
            }

            is ExerciseDetailEvent.DescriptionChanged -> updateForm {
                it.copy(description = event.description.take(MAX_DESCRIPTION_LENGTH))
            }

            is ExerciseDetailEvent.NotesChanged -> updateForm {
                it.copy(notes = event.notes.take(MAX_NOTES_LENGTH))
            }

            ExerciseDetailEvent.SaveClicked -> save()

            ExerciseDetailEvent.DeleteClicked -> _uiState.update {
                if (it.canDelete) it.copy(showDeleteConfirmation = true) else it
            }

            ExerciseDetailEvent.DeleteDismissed -> _uiState.update {
                it.copy(showDeleteConfirmation = false)
            }

            ExerciseDetailEvent.DeleteConfirmed -> {
                _uiState.update { it.copy(showDeleteConfirmation = false) }
                delete()
            }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            getExercisesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> {
                        knownExercises = result.data.orEmpty()
                        if (exerciseId == null) {
                            updateForm { it.copy(isLoading = false) }
                            return@collect
                        }
                        val exercise = knownExercises.find { it.id == exerciseId }
                        if (exercise == null) {
                            updateForm { it.copy(isLoading = false) }
                            _effect.emit(ExerciseDetailEffect.ShowToast("Exercício não encontrado"))
                        } else {
                            updateForm {
                                it.copy(
                                    isLoading = false,
                                    name = exercise.name,
                                    muscleGroup = exercise.muscleGroup,
                                    description = exercise.description,
                                    notes = exercise.notes,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            ExerciseDetailEffect.ShowToast(
                                result.message ?: "Erro ao carregar exercício"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadUsage(id: String) {
        viewModelScope.launch {
            getWorkoutsUsingExerciseUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> Unit

                    is Resource.Success -> _uiState.update {
                        it.copy(
                            usedInWorkouts = result.data.orEmpty()
                                .map { workout -> workout.name }
                                .sortedBy { name -> name.lowercase() }
                        )
                    }

                    is Resource.Error -> _effect.emit(
                        ExerciseDetailEffect.ShowToast(
                            result.message ?: "Erro ao verificar treinos"
                        )
                    )
                }
            }
        }
    }

    private fun updateForm(transform: (ExerciseDetailUiState) -> ExerciseDetailUiState) {
        _uiState.update { old ->
            val new = transform(old)
            val trimmed = new.name.trim()
            val isDuplicate = trimmed.isNotEmpty() && knownExercises.any {
                it.id != new.exerciseId && it.name.trim().equals(trimmed, ignoreCase = true)
            }
            new.copy(
                isSubmitEnabled = trimmed.isNotEmpty(),
                duplicateWarning = if (isDuplicate) DUPLICATE_WARNING else null,
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.isBusy) return

        val exercise = Exercise(
            id = state.exerciseId.orEmpty(),
            name = state.name.trim(),
            muscleGroup = state.muscleGroup,
            description = state.description.trim(),
            notes = state.notes.trim(),
        )

        viewModelScope.launch {
            val flow = if (state.isEditing) {
                updateExerciseUseCase(exercise)
            } else {
                addExerciseUseCase(exercise)
            }
            flow.collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(ExerciseDetailEffect.ShowToast("Exercício salvo!"))
                        _effect.emit(ExerciseDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(
                            ExerciseDetailEffect.ShowToast(
                                result.message ?: "Erro ao salvar exercício"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun delete() {
        val id = _uiState.value.exerciseId ?: return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            deleteExerciseUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isDeleting = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(ExerciseDetailEffect.ShowToast("Exercício excluído"))
                        _effect.emit(ExerciseDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(
                            ExerciseDetailEffect.ShowToast(
                                result.message ?: "Erro ao excluir exercício"
                            )
                        )
                    }
                }
            }
        }
    }
}
