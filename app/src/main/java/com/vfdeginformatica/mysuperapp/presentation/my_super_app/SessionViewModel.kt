package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.domain.use_case.IsLoggedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    isLoggedUseCase: IsLoggedUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = SessionUiState(isLoading = true)
            isLoggedUseCase.invoke().collect { isLogged ->
                if (isLogged) {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoggedOut = true,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

}