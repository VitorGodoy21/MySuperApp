package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.HomeMenuItem
import com.vfdeginformatica.mysuperapp.domain.use_case.AuthenticateWithBiometricUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authenticateWithBiometricUseCase: AuthenticateWithBiometricUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnMenuItemNavigate -> {
                viewModelScope.launch {
                    // Se requer senha/biometria, SEMPRE é necessário validar
                    if (event.passwordRequired) {
                        // Se a activity for nula, não libera acesso
                        if (event.activity == null) {
                            _effect.emit(HomeEffect.ShowToast("Erro: Não foi possível obter o contexto da aplicação"))
                        } else {
                            authenticateWithBiometric(event.route, event.activity)
                        }
                    } else {
                        // Apenas itens sem proteção podem ser acessados diretamente
                        _effect.emit(HomeEffect.NavigateToMenuItem(event.route))
                    }
                }
            }
        }
    }

    private fun authenticateWithBiometric(route: String, activity: FragmentActivity) {
        viewModelScope.launch {
            authenticateWithBiometricUseCase(activity).collect { isAuthenticated ->
                if (isAuthenticated) {
                    _effect.emit(HomeEffect.NavigateToMenuItem(route))
                } else {
                    _effect.emit(HomeEffect.ShowToast("Autenticação biométrica falhou"))
                }
            }
        }
    }

    // ...existing code...
    init {
        getMenuItems()
    }

    private fun getMenuItems() {
        _uiState.value = HomeUiState(
            items = listOf(
                HomeMenuItem(
                    title = "Finanças",
                    icon = Icons.Default.AllInclusive,
                    route = Screen.FinancialScreen.route,
                    backgroundColor = Color.DarkGray,
                    content = {}
                ),

                HomeMenuItem(
                    title = "QRCode",
                    icon = Icons.Default.QrCode2,
                    route = Screen.FinancialScreen.route,
                    backgroundColor = Color.DarkGray,
                    passwordRequired = true,
                    content = {}
                )
            )
        )
    }
}