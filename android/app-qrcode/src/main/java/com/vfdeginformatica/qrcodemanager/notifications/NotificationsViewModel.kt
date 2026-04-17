package com.vfdeginformatica.qrcodemanager.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetNotificationSettingsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.UpdateNotificationSettingsUseCase
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
class NotificationsViewModel @Inject constructor(
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NotificationsEffect>()
    val effect: SharedFlow<NotificationsEffect> = _effect.asSharedFlow()

    init {
        load()
    }

    fun onEvent(event: NotificationsEvent) {
        when (event) {
            is NotificationsEvent.OnToggleAll -> {
                val updated = if (event.enabled) {
                    NotificationSettings(
                        enabledAll = true,
                        enabledAccess = true,
                        enabledMuralComments = true
                    )
                } else {
                    NotificationSettings(
                        enabledAll = false,
                        enabledAccess = false,
                        enabledMuralComments = false
                    )
                }
                save(updated)
            }

            is NotificationsEvent.OnToggleAccess -> {
                val state = _uiState.value
                if (!state.enabledAll) return
                save(
                    NotificationSettings(
                        enabledAll = state.enabledAll,
                        enabledAccess = event.enabled,
                        enabledMuralComments = state.enabledMuralComments
                    )
                )
            }

            is NotificationsEvent.OnToggleMuralComments -> {
                val state = _uiState.value
                if (!state.enabledAll) return
                save(
                    NotificationSettings(
                        enabledAll = state.enabledAll,
                        enabledAccess = state.enabledAccess,
                        enabledMuralComments = event.enabled
                    )
                )
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            getNotificationSettingsUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(
                            isLoading = true,
                            errorMessage = ""
                        )
                    }

                    is Resource.Success -> {
                        val data = resource.data ?: NotificationSettings()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                enabledAll = data.enabledAll,
                                enabledAccess = data.enabledAccess,
                                enabledMuralComments = data.enabledMuralComments,
                                errorMessage = ""
                            )
                        }
                    }

                    is Resource.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Erro ao carregar notificações"
                        )
                    }
                }
            }
        }
    }

    private fun save(settings: NotificationSettings) {
        viewModelScope.launch {
            updateNotificationSettingsUseCase(settings).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isSaving = true, errorMessage = "", successMessage = "")
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                enabledAll = settings.enabledAll,
                                enabledAccess = settings.enabledAccess,
                                enabledMuralComments = settings.enabledMuralComments,
                                successMessage = "Preferências atualizadas"
                            )
                        }
                        _effect.emit(NotificationsEffect.ShowToast("Preferências atualizadas"))
                    }

                    is Resource.Error -> _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = resource.message ?: "Erro ao atualizar notificações"
                        )
                    }
                }
            }
        }
    }
}

