package com.vfdeginformatica.qrcodemanager.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrCodeManagerDrawerUiState(
    val isOpen: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null
)

@HiltViewModel
class QrCodeManagerDrawerViewModel @Inject constructor(
    private val getUserSessionUseCase: GetUserSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeManagerDrawerUiState())
    val uiState: StateFlow<QrCodeManagerDrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getUserSessionUseCase().collect { session ->
                _uiState.update {
                    it.copy(
                        userName = session.name.ifEmpty { null },
                        userEmail = session.email.ifEmpty { null }
                    )
                }
            }
        }
    }

    fun toggleDrawer() = _uiState.update { it.copy(isOpen = !it.isOpen) }
    fun closeDrawer() = _uiState.update { it.copy(isOpen = false) }
}

