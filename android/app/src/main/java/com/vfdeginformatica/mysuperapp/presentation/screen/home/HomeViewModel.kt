package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.HomeMenuItem
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeUiState
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnMenuItemNavigate -> {
                viewModelScope.launch {
                    resolveProtectedNavigationUseCase(
                        route = event.route,
                        passwordRequired = event.passwordRequired,
                        activity = event.activity
                    ).collect { result ->
                        when (result) {
                            is ProtectedNavigationResult.Allowed -> {
                                _effect.emit(HomeEffect.NavigateToMenuItem(result.route))
                            }

                            is ProtectedNavigationResult.Denied -> {
                                _effect.emit(HomeEffect.ShowToast(result.message))
                            }
                        }
                    }
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
                    route = Screen.QrCodeListScreen.route,
                    backgroundColor = Color.DarkGray,
                    passwordRequired = true,
                    content = {}
                ),

                HomeMenuItem(
                    title = "NFC",
                    icon = Icons.Default.Nfc,
                    route = SharedNfcRoutes.NFC_SCREEN,
                    backgroundColor = Color.DarkGray,
                    passwordRequired = true,
                    content = {}
                )
            )
        )
    }
}