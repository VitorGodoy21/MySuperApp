package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.lifecycle.ViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FinancialViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState

    private val _effect = MutableSharedFlow<FinancialEffect>()
    val effect: SharedFlow<FinancialEffect> = _effect

    fun onEvent(event: FinancialEvent) {
        // Handle events here
    }
}