package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.use_case.feature_flags.ObserveFeatureFlagsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.feature_flags.RefreshFeatureFlagsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.IsLoggedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    isLoggedUseCase: IsLoggedUseCase,
    observeFeatureFlagsUseCase: ObserveFeatureFlagsUseCase,
    private val refreshFeatureFlagsUseCase: RefreshFeatureFlagsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    /** Funções habilitadas no painel web; usado para bloquear destinos do NavHost. */
    val featureFlags: StateFlow<FeatureFlags> = observeFeatureFlagsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeatureFlags())

    init {
        viewModelScope.launch {
            _uiState.value = SessionUiState(isLoading = true)
            isLoggedUseCase.invoke().collect { isLogged ->
                if (isLogged) {
                    // Cobre o login feito com o app já aberto (onResume não dispara de novo).
                    launch { refreshFeatureFlagsUseCase() }
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