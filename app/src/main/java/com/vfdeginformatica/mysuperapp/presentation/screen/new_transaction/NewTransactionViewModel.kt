package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction

import androidx.lifecycle.ViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.NewTransactionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NewTransactionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState

    private val _effect = MutableSharedFlow<NewTransactionEffect>()
    val effect: SharedFlow<NewTransactionEffect> = _effect

    fun onEvent(event: NewTransactionEvent) {
        // Handle events here
    }
}