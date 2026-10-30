package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu

import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import com.vfdeginformatica.mysuperapp.domain.use_case.feature_flags.ObserveFeatureFlagsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LogoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuEffect
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerMenuViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val logoutUseCase: LogoutUseCase = mock()
    private val getUserSessionUseCase: GetUserSessionUseCase = mock()
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase = mock()
    private val observeFeatureFlagsUseCase: ObserveFeatureFlagsUseCase = mock()
    private val activity: FragmentActivity = mock()

    private lateinit var viewModel: AppDrawerMenuViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        `when`(getUserSessionUseCase()).thenReturn(flowOf(UserSession()))
        `when`(observeFeatureFlagsUseCase()).thenReturn(flowOf(FeatureFlags(finance = false)))
        viewModel = AppDrawerMenuViewModel(
            logoutUseCase,
            getUserSessionUseCase,
            resolveProtectedNavigationUseCase,
            observeFeatureFlagsUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesFeatureFlagsInState() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.flags.finance)
        assertTrue(viewModel.uiState.value.flags.workout)
    }

    @Test
    fun financialRoutesNavigateWhenBiometricAllowed() = runTest(testDispatcher) {
        val routes = listOf(
            Screen.FinancialScreen.route,
            Screen.CardListScreen.route,
            Screen.CategoryListScreen.route
        )
        routes.forEach { route ->
            `when`(resolveProtectedNavigationUseCase(route, true, activity))
                .thenReturn(flowOf(ProtectedNavigationResult.Allowed(route)))
        }

        val effects = mutableListOf<AppDrawerMenuEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        routes.forEach { viewModel.navigateWithBiometric(it, activity = activity) }
        advanceUntilIdle()

        assertEquals(routes.map { AppDrawerMenuEffect.Navigate(it) }, effects)
    }

    @Test
    fun financialRouteShowsToastWhenBiometricDenied() = runTest(testDispatcher) {
        val route = Screen.FinancialScreen.route
        `when`(resolveProtectedNavigationUseCase(route, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Denied("Autenticação biométrica falhou")))

        val effects = mutableListOf<AppDrawerMenuEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.navigateWithBiometric(route, activity = activity)
        advanceUntilIdle()

        assertEquals(listOf(AppDrawerMenuEffect.ShowToastMessage("Autenticação biométrica falhou")), effects)
    }
}
