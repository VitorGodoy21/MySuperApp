package com.vfdeginformatica.mysuperapp.presentation.screen.notifications

import com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract.NotificationsEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract.NotificationsEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract.NotificationsUiState

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
        val current = _uiState.value.toSettings()
        when (event) {
            is NotificationsEvent.OnToggleAll -> save(
                NotificationSettings(
                    enabledAll = event.enabled,
                    enabledAccess = event.enabled,
                    enabledMuralComments = event.enabled,
                    enabledFinancialReports = event.enabled
                )
            )

            is NotificationsEvent.OnToggleAccess -> {
                if (!current.enabledAll) return
                save(current.copy(enabledAccess = event.enabled))
            }

            is NotificationsEvent.OnToggleMuralComments -> {
                if (!current.enabledAll) return
                save(current.copy(enabledMuralComments = event.enabled))
            }

            is NotificationsEvent.OnToggleFinancialReports -> {
                if (!current.enabledAll) return
                save(current.copy(enabledFinancialReports = event.enabled))
            }
        }
    }

    private fun NotificationsUiState.toSettings() = NotificationSettings(
        enabledAll = enabledAll,
        enabledAccess = enabledAccess,
        enabledMuralComments = enabledMuralComments,
        enabledFinancialReports = enabledFinancialReports
    )

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
                                enabledFinancialReports = data.enabledFinancialReports,
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
                                enabledFinancialReports = settings.enabledFinancialReports,
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


