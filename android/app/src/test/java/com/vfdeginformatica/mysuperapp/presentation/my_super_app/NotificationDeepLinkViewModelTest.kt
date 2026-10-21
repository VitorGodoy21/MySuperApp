package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodeByIdUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.notification.PendingQrCodeNavigation
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationDeepLinkViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase = mock()
    private val getQrCodeByIdUseCase: GetQrCodeByIdUseCase = mock()
    private val activity: FragmentActivity = mock()
    private val pending = PendingQrCodeNavigation()

    private lateinit var viewModel: NotificationDeepLinkViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NotificationDeepLinkViewModel(pending, resolveProtectedNavigationUseCase, getQrCodeByIdUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun allowBiometric() {
        `when`(resolveProtectedNavigationUseCase(Screen.QrCodeListScreen.route, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Allowed(Screen.QrCodeListScreen.route)))
    }

    @Test
    fun allowedAndFoundNavigatesToQrCodeDetail() = runTest(testDispatcher) {
        allowBiometric()
        val qrCode = QrCode(id = "qr1", identifier = "meu-qr", redirectUrl = "https://x.y")
        `when`(getQrCodeByIdUseCase("qr1")).thenReturn(flowOf(Resource.Loading(), Resource.Success(qrCode)))
        val effects = mutableListOf<NotificationDeepLinkEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        pending.set("qr1")
        viewModel.consumePending(activity)
        advanceUntilIdle()

        assertEquals(listOf(NotificationDeepLinkEffect.OpenQrCode(qrCode)), effects)
        assertNull(pending.pendingQrCodeId.value)
    }

    @Test
    fun allowedButNotFoundFallsBackToListWithToast() = runTest(testDispatcher) {
        allowBiometric()
        `when`(getQrCodeByIdUseCase("missing")).thenReturn(flowOf(Resource.Loading(), Resource.Error("QR Code não encontrado")))
        val effects = mutableListOf<NotificationDeepLinkEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        pending.set("missing")
        viewModel.consumePending(activity)
        advanceUntilIdle()

        assertEquals(
            listOf(
                NotificationDeepLinkEffect.ShowToast("QR Code não encontrado"),
                NotificationDeepLinkEffect.Navigate(Screen.QrCodeListScreen.route)
            ),
            effects
        )
    }

    @Test
    fun deniedOnlyShowsToast() = runTest(testDispatcher) {
        `when`(resolveProtectedNavigationUseCase(Screen.QrCodeListScreen.route, true, activity))
            .thenReturn(flowOf(ProtectedNavigationResult.Denied("Autenticação biométrica falhou")))
        val effects = mutableListOf<NotificationDeepLinkEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        pending.set("qr1")
        viewModel.consumePending(activity)
        advanceUntilIdle()

        assertEquals(listOf(NotificationDeepLinkEffect.ShowToast("Autenticação biométrica falhou")), effects)
    }

    @Test
    fun consumeWithoutPendingIsNoOp() = runTest(testDispatcher) {
        val effects = mutableListOf<NotificationDeepLinkEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.consumePending(activity)
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
    }
}
