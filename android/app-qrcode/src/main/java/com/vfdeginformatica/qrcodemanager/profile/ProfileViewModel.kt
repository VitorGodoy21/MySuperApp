package com.vfdeginformatica.qrcodemanager.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserSessionUseCase: GetUserSessionUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            getUserSessionUseCase().collect { session ->
                _uiState.update {
                    it.copy(
                        name = session.name.ifEmpty { null },
                        email = session.email.ifEmpty { null }
                    )
                }
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.OnToggleEdit -> _uiState.update {
                it.copy(
                    isEditing = true,
                    editName = it.name.orEmpty(),
                    editEmail = it.email.orEmpty(),
                    errorMessage = "",
                    successMessage = ""
                )
            }

            ProfileEvent.OnCancelEdit -> _uiState.update {
                it.copy(isEditing = false, errorMessage = "", successMessage = "")
            }

            is ProfileEvent.OnNameChanged -> _uiState.update { it.copy(editName = event.name) }
            is ProfileEvent.OnEmailChanged -> _uiState.update { it.copy(editEmail = event.email) }
            ProfileEvent.OnSave -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            updateUserProfileUseCase(state.editName, state.editEmail).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isSaving = true, errorMessage = "", successMessage = "")
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isEditing = false,
                                name = state.editName,
                                email = state.editEmail,
                                successMessage = "Perfil atualizado com sucesso!"
                            )
                        }
                        _effect.emit(ProfileEffect.ShowToast("Perfil atualizado com sucesso!"))
                    }

                    is Resource.Error -> _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = resource.message ?: "Erro ao salvar perfil"
                        )
                    }
                }
            }
        }
    }
}

