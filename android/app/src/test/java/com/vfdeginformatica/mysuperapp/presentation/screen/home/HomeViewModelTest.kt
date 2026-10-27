package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.MonthFinancialSummary
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.summary.GetMonthFinancialSummaryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.CountQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.summary.GetWorkoutHomeSummaryUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.summary.WorkoutHomeSummary
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase = mock()
    private val getMonthFinancialSummaryUseCase: GetMonthFinancialSummaryUseCase = mock()
    private val countQrCodesUseCase: CountQrCodesUseCase = mock()
    private val getWorkoutHomeSummaryUseCase: GetWorkoutHomeSummaryUseCase = mock()
    private val activity: FragmentActivity = mock()

    private val financeSummary = MonthFinancialSummary(YearMonth.of(2026, 10), totalIncome = 1000.0, cashExpense = 400.0)
    private val workoutSummary = WorkoutHomeSummary(null, 2, 90, 500)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        `when`(getMonthFinancialSummaryUseCase()).thenReturn(flowOf(Resource.Loading(), Resource.Success(financeSummary)))
        `when`(countQrCodesUseCase()).thenReturn(flowOf(Resource.Loading(), Resource.Success(3)))
        `when`(getWorkoutHomeSummaryUseCase()).thenReturn(flowOf(Resource.Loading(), Resource.Success(workoutSummary)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        resolveProtectedNavigationUseCase,
        getMonthFinancialSummaryUseCase,
        countQrCodesUseCase,
        getWorkoutHomeSummaryUseCase,
    )

    private fun allow(route: String) {
        `when`(resolveProtectedNavigationUseCase(route, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Allowed(route)))
    }

    @Test
    fun loadsAllSectionsOnInit() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(financeSummary, state.finance.summary)
        assertFalse(state.finance.isLoading)
        assertFalse(state.finance.isRevealed)
        assertEquals(3, state.smartTags.qrCount)
        assertEquals(workoutSummary, state.workout.summary)
    }

    @Test
    fun sectionErrorsAreIndependent() = runTest(testDispatcher) {
        `when`(countQrCodesUseCase()).thenReturn(flowOf(Resource.Error("falhou")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("falhou", state.smartTags.error)
        assertNull(state.smartTags.qrCount)
        assertNull(state.finance.error)
        assertEquals(financeSummary, state.finance.summary)
    }

    @Test
    fun revealFinanceRequiresBiometricAndNeverNavigates() = runTest(testDispatcher) {
        allow(HomeFinanceReveal.ROUTE)
        val viewModel = createViewModel()
        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(HomeEvent.OnRevealFinance(activity))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.finance.isRevealed)
        assertTrue(effects.isEmpty())

        viewModel.onEvent(HomeEvent.OnHideFinance)
        assertFalse(viewModel.uiState.value.finance.isRevealed)
    }

    @Test
    fun revealFinanceDeniedShowsToastAndStaysHidden() = runTest(testDispatcher) {
        `when`(resolveProtectedNavigationUseCase(HomeFinanceReveal.ROUTE, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Denied("Autenticação biométrica falhou")))
        val viewModel = createViewModel()
        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(HomeEvent.OnRevealFinance(activity))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.finance.isRevealed)
        assertEquals(listOf(HomeEffect.ShowToast("Autenticação biométrica falhou")), effects)
    }

    @Test
    fun financeAndSmartTagShortcutsAreBiometricProtected() = runTest(testDispatcher) {
        val expected = mapOf(
            HomeEvent.OnOpenFinance(activity) to Screen.FinancialScreen.route,
            HomeEvent.OnAddExpense(activity) to Screen.NewTransactionScreen.createRoute("EXPENSE"),
            HomeEvent.OnAddIncome(activity) to Screen.NewTransactionScreen.createRoute("INCOME"),
            HomeEvent.OnOpenQrCodes(activity) to Screen.QrCodeListScreen.route,
            HomeEvent.OnOpenNfc(activity) to SharedNfcRoutes.NFC_SCREEN,
        )
        expected.values.forEach(::allow)
        val viewModel = createViewModel()
        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        expected.keys.forEach(viewModel::onEvent)
        advanceUntilIdle()

        expected.values.forEach { verify(resolveProtectedNavigationUseCase).invoke(it, true, activity) }
        assertEquals(expected.values.map { HomeEffect.NavigateToMenuItem(it) }, effects)
    }

    @Test
    fun workoutShortcutsDoNotRequireBiometric() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(HomeEvent.OnOpenWorkoutHub)
        viewModel.onEvent(HomeEvent.OnOpenTodayWorkout("w1"))
        viewModel.onEvent(HomeEvent.OnOpenDiet)
        advanceUntilIdle()

        verify(resolveProtectedNavigationUseCase, never()).invoke(anyString(), anyBoolean(), any())
        assertEquals(
            listOf(
                HomeEffect.NavigateToMenuItem(Screen.WorkoutHubScreen.route),
                HomeEffect.NavigateToMenuItem(Screen.WorkoutViewScreen.createRoute("w1")),
                HomeEffect.NavigateToMenuItem(Screen.DietViewScreen.route),
            ),
            effects,
        )
    }
}
