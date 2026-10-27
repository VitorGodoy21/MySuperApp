package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.summary.GetMonthFinancialSummaryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.CountQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.summary.GetWorkoutHomeSummaryUseCase
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sentinel route: revealing finance values on the Home requires biometrics but navigates nowhere. */
object HomeFinanceReveal {
    const val ROUTE = "home_finance_reveal"
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase,
    private val getMonthFinancialSummaryUseCase: GetMonthFinancialSummaryUseCase,
    private val countQrCodesUseCase: CountQrCodesUseCase,
    private val getWorkoutHomeSummaryUseCase: GetWorkoutHomeSummaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadAll()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnOpenFinance -> navigateProtected(Screen.FinancialScreen.route, event.activity)
            is HomeEvent.OnRevealFinance -> revealFinance(event.activity)
            HomeEvent.OnHideFinance -> _uiState.update { it.copy(finance = it.finance.copy(isRevealed = false)) }
            is HomeEvent.OnAddExpense ->
                navigateProtected(Screen.NewTransactionScreen.createRoute(TransactionType.EXPENSE.name), event.activity)
            is HomeEvent.OnAddIncome ->
                navigateProtected(Screen.NewTransactionScreen.createRoute(TransactionType.INCOME.name), event.activity)

            is HomeEvent.OnOpenQrCodes -> navigateProtected(Screen.QrCodeListScreen.route, event.activity)
            is HomeEvent.OnOpenNfc -> navigateProtected(SharedNfcRoutes.NFC_SCREEN, event.activity)

            HomeEvent.OnOpenWorkoutHub -> navigate(Screen.WorkoutHubScreen.route)
            is HomeEvent.OnOpenTodayWorkout -> navigate(Screen.WorkoutViewScreen.createRoute(event.workoutId))
            HomeEvent.OnOpenDiet -> navigate(Screen.DietViewScreen.route)

            HomeEvent.OnRefresh -> loadAll()
        }
    }

    private fun loadAll() {
        loadFinance()
        loadSmartTags()
        loadWorkout()
    }

    private fun loadFinance() {
        viewModelScope.launch {
            getMonthFinancialSummaryUseCase().collect { result ->
                _uiState.update { state ->
                    state.copy(
                        finance = when (result) {
                            is Resource.Loading -> state.finance.copy(isLoading = true, error = null)
                            is Resource.Success -> state.finance.copy(isLoading = false, error = null, summary = result.data)
                            is Resource.Error -> state.finance.copy(isLoading = false, error = result.message)
                        }
                    )
                }
            }
        }
    }

    private fun loadSmartTags() {
        viewModelScope.launch {
            countQrCodesUseCase().collect { result ->
                _uiState.update { state ->
                    state.copy(
                        smartTags = when (result) {
                            is Resource.Loading -> state.smartTags.copy(isLoading = true, error = null)
                            is Resource.Success -> state.smartTags.copy(isLoading = false, error = null, qrCount = result.data)
                            is Resource.Error -> state.smartTags.copy(isLoading = false, error = result.message)
                        }
                    )
                }
            }
        }
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            getWorkoutHomeSummaryUseCase().collect { result ->
                _uiState.update { state ->
                    state.copy(
                        workout = when (result) {
                            is Resource.Loading -> state.workout.copy(isLoading = true, error = null)
                            is Resource.Success -> state.workout.copy(isLoading = false, error = null, summary = result.data)
                            is Resource.Error -> state.workout.copy(isLoading = false, error = result.message)
                        }
                    )
                }
            }
        }
    }

    private fun revealFinance(activity: FragmentActivity?) {
        viewModelScope.launch {
            resolveProtectedNavigationUseCase(HomeFinanceReveal.ROUTE, true, activity).collect { result ->
                when (result) {
                    is ProtectedNavigationResult.Allowed ->
                        _uiState.update { it.copy(finance = it.finance.copy(isRevealed = true)) }
                    is ProtectedNavigationResult.Denied -> _effect.send(HomeEffect.ShowToast(result.message))
                }
            }
        }
    }

    private fun navigateProtected(route: String, activity: FragmentActivity?) {
        viewModelScope.launch {
            resolveProtectedNavigationUseCase(route, true, activity).collect { result ->
                when (result) {
                    is ProtectedNavigationResult.Allowed -> _effect.send(HomeEffect.NavigateToMenuItem(result.route))
                    is ProtectedNavigationResult.Denied -> _effect.send(HomeEffect.ShowToast(result.message))
                }
            }
        }
    }

    private fun navigate(route: String) {
        viewModelScope.launch { _effect.send(HomeEffect.NavigateToMenuItem(route)) }
    }
}
