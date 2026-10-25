package com.vfdeginformatica.mysuperapp.presentation.screen.diet_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.GetDietUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract.DietViewEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract.DietViewUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DietViewViewModel @Inject constructor(
    private val getDiet: GetDietUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DietViewUiState())
    val uiState: StateFlow<DietViewUiState> = _uiState
    private var loadJob: Job? = null

    fun onEvent(event: DietViewEvent) {
        when (event) {
            DietViewEvent.Reload -> {
                loadJob?.cancel()
                loadJob = viewModelScope.launch {
                    getDiet().collect { result ->
                        _uiState.value = when (result) {
                            is Resource.Loading -> DietViewUiState()
                            is Resource.Success -> DietViewUiState(isLoading = false, diet = result.data?.diet)
                            is Resource.Error -> DietViewUiState(
                                isLoading = false, error = result.message ?: "Erro ao carregar dieta",
                            )
                        }
                    }
                }
            }
        }
    }
}
