package com.vfdeginformatica.qrcodemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.domain.use_case.login.IsLoggedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrCodeManagerSessionViewModel @Inject constructor(
    isLoggedUseCase: IsLoggedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeManagerSessionUiState())
    val uiState: StateFlow<QrCodeManagerSessionUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = QrCodeManagerSessionUiState(isLoading = true)
            isLoggedUseCase.invoke().collect { isLogged ->
                if (isLogged) {
                    _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoggedOut = true, isLoading = false) }
                }
            }
        }
    }
}

