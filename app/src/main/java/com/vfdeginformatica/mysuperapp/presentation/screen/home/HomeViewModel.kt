package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.HomeMenuItem
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
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnMenuItemNavigate -> {
                viewModelScope.launch {
                    _effect.emit(HomeEffect.NavigateToMenuItem(event.route))
                }
            }
        }
    }

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
                    backgroundColor = Color.Cyan,
                    content = {}
                ),

                HomeMenuItem(
                    title = "Academia",
                    icon = Icons.Default.Anchor,
                    route = Screen.FinancialScreen.route,
                    backgroundColor = Color.Magenta,
                    content = {}
                )
            )
        )
    }
}