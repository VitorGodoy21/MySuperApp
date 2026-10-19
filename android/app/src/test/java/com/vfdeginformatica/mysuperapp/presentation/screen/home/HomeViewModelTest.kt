package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase = mock()
    private val activity: FragmentActivity = mock()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(resolveProtectedNavigationUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun financialQrAndNfcMenuItemsRequireBiometric() {
        val items = viewModel.uiState.value.items
        val protectedRoutes = listOf(
            Screen.FinancialScreen.route,
            Screen.QrCodeListScreen.route,
            SharedNfcRoutes.NFC_SCREEN
        )

        protectedRoutes.forEach { route ->
            val item = items.first { it.route == route }
            assertTrue("Route $route should require biometric", item.passwordRequired)
        }
    }

    @Test
    fun workoutMenuItemDoesNotRequireBiometric() {
        val item = viewModel.uiState.value.items.first { it.route == Screen.WorkoutHubScreen.route }
        assertEquals("Academia", item.title)
        assertFalse(item.passwordRequired)
    }

    @Test
    fun financialNavigationEmitsNavigateWhenBiometricAllowed() = runTest(testDispatcher) {
        val route = Screen.FinancialScreen.route
        `when`(resolveProtectedNavigationUseCase(route, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Allowed(route)))

        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(HomeEvent.OnMenuItemNavigate(route, passwordRequired = true, activity = activity))
        advanceUntilIdle()

        assertEquals(listOf(HomeEffect.NavigateToMenuItem(route)), effects)
    }

    @Test
    fun financialNavigationEmitsToastWhenBiometricDenied() = runTest(testDispatcher) {
        val route = Screen.FinancialScreen.route
        `when`(resolveProtectedNavigationUseCase(route, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Denied("Autenticação biométrica falhou")))

        val effects = mutableListOf<HomeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(HomeEvent.OnMenuItemNavigate(route, passwordRequired = true, activity = activity))
        advanceUntilIdle()

        assertEquals(listOf(HomeEffect.ShowToast("Autenticação biométrica falhou")), effects)
    }
}
