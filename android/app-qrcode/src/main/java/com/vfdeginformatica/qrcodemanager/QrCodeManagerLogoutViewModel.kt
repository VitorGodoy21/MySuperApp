package com.vfdeginformatica.qrcodemanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrCodeManagerLogoutViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _effect = MutableSharedFlow<LogoutEffect>()
    val effect: SharedFlow<LogoutEffect> = _effect.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            logoutUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _isLoading.value = true
                    is Resource.Success -> {
                        _isLoading.value = false
                        _effect.emit(LogoutEffect.NavigateToLogin)
                    }

                    is Resource.Error -> {
                        _isLoading.value = false
                        _effect.emit(LogoutEffect.ShowError(result.message ?: "Erro ao sair"))
                    }
                }
            }
        }
    }

    sealed class LogoutEffect {
        object NavigateToLogin : LogoutEffect()
        data class ShowError(val message: String) : LogoutEffect()
    }
}

