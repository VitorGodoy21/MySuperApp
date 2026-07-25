package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LogoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuEffect
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuEvent
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuUiState
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.model.UiMenuItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerMenuViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val getUserSessionUseCase: GetUserSessionUseCase,
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDrawerMenuUiState())
    val uiState: StateFlow<AppDrawerMenuUiState> = _uiState

    private val _effect = MutableSharedFlow<AppDrawerMenuEffect>()
    val effect: SharedFlow<AppDrawerMenuEffect> = _effect

    fun onEvent(event: AppDrawerMenuEvent) {
        when (event) {
            is AppDrawerMenuEvent.Close -> _uiState.update {
                it.copy(
                    isOpen = false
                )
            }

            is AppDrawerMenuEvent.Open -> _uiState.update { it.copy(isOpen = true) }
            is AppDrawerMenuEvent.ClickItem -> {
                _uiState.update { it.copy(isOpen = false) }
                onClickItem(event.item)
            }

            is AppDrawerMenuEvent.OnResume -> _uiState.update { it.copy(closeDrawer = true) }
        }
    }

    init {
        viewModelScope.launch {
            getUserSessionUseCase.invoke().collect { userSession ->
                _uiState.update {
                    it.copy(
                        userName = userSession.name,
                        userEmail = userSession.email
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase.invoke().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _effect.emit(AppDrawerMenuEffect.NavigateToLogin)
                        _effect.emit(
                            AppDrawerMenuEffect.ShowToastMessage(
                                result.data ?: "Logout successful"
                            )
                        )
                    }

                    is Resource.Error<*> -> {
                        _effect.emit(
                            AppDrawerMenuEffect.ShowSnackBarMessage(
                                result.data ?: "Logout successful"
                            )
                        )
                    }

                    is Resource.Loading<*> -> {

                    }

                }
            }
        }
    }

    fun goHome() = viewModelScope.launch {
        _effect.emit(AppDrawerMenuEffect.NavigateToHome)
    }

    fun navigate(route: String) = viewModelScope.launch {
        _effect.emit(AppDrawerMenuEffect.Navigate(route))
    }

    /**
     * Navega para uma rota que pode exigir biometria, delegando a decisão
     * ao fluxo comum [ResolveProtectedNavigationUseCase] (mesmo usado na Home).
     */
    fun navigateWithBiometric(route: String, passwordRequired: Boolean = true, activity: FragmentActivity?) {
        viewModelScope.launch {
            resolveProtectedNavigationUseCase(route, passwordRequired, activity).collect { result ->
                when (result) {
                    is ProtectedNavigationResult.Allowed -> {
                        _effect.emit(AppDrawerMenuEffect.Navigate(result.route))
                    }

                    is ProtectedNavigationResult.Denied -> {
                        _effect.emit(AppDrawerMenuEffect.ShowToastMessage(result.message))
                    }
                }
            }
        }
    }

    private fun onClickItem(item: UiMenuItem) {
        viewModelScope.launch {
            _effect.emit(AppDrawerMenuEffect.Navigate(item.route))
        }
    }


}