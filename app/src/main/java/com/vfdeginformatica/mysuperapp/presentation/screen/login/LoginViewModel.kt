package com.vfdeginformatica.mysuperapp.presentation.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LoginUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.validator.ValidateEmail
import com.vfdeginformatica.mysuperapp.domain.use_case.validator.ValidatePassword
import com.vfdeginformatica.mysuperapp.presentation.screen.login.contract.LoginEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.login.contract.LoginEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.login.contract.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> updateForm(email = event.value)
            is LoginEvent.PasswordChanged -> updateForm(password = event.value)
            is LoginEvent.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isLoginEnabled || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loginUseCase(
                state.email,
                state.password
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(LoginEffect.NavigateToHome)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            LoginEffect.ShowToast(
                                result.message ?: "Erro ao fazer o login."
                            )
                        )
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun updateForm(
        email: String? = null,
        password: String? = null
    ) {
        _uiState.update { old ->
            val new = old.copy(
                email = email ?: old.email,
                password = password ?: old.password
            )

            new.copy(
                isLoginEnabled = validateEmail(new.email) && validatePassword(new.password)
            )
        }
    }

}