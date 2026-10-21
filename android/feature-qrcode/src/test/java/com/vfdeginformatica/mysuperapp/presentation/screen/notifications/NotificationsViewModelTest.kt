package com.vfdeginformatica.mysuperapp.presentation.screen.notifications

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetNotificationSettingsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.UpdateNotificationSettingsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.notifications.contract.NotificationsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class NotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: UserRepository = mock()

    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        `when`(repository.getNotificationSettings()).thenReturn(Resource.Success(NotificationSettings()))
        viewModel = NotificationsViewModel(
            GetNotificationSettingsUseCase(repository),
            UpdateNotificationSettingsUseCase(repository)
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleAllOffDisablesEverything() = runTest(testDispatcher) {
        val expected = NotificationSettings(false, false, false)
        `when`(repository.updateNotificationSettings(expected)).thenReturn(Resource.Success(Unit))

        viewModel.onEvent(NotificationsEvent.OnToggleAll(false))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.enabledAll)
        assertFalse(state.enabledAccess)
        assertFalse(state.enabledMuralComments)
        assertFalse(state.isSaving)
    }

    @Test
    fun individualToggleIgnoredWhenAllDisabled() = runTest(testDispatcher) {
        `when`(repository.updateNotificationSettings(NotificationSettings(false, false, false)))
            .thenReturn(Resource.Success(Unit))
        viewModel.onEvent(NotificationsEvent.OnToggleAll(false))
        advanceUntilIdle()

        viewModel.onEvent(NotificationsEvent.OnToggleAccess(true))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.enabledAccess)
    }

    @Test
    fun toggleAccessPersistsOnlyAccess() = runTest(testDispatcher) {
        val expected = NotificationSettings(enabledAll = true, enabledAccess = false, enabledMuralComments = true)
        `when`(repository.updateNotificationSettings(expected)).thenReturn(Resource.Success(Unit))

        viewModel.onEvent(NotificationsEvent.OnToggleAccess(false))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.enabledAll)
        assertFalse(state.enabledAccess)
        assertTrue(state.enabledMuralComments)
        assertEquals("Preferências atualizadas", state.successMessage)
    }

    @Test
    fun saveErrorKeepsPreviousStateAndShowsError() = runTest(testDispatcher) {
        `when`(repository.updateNotificationSettings(NotificationSettings(false, false, false)))
            .thenReturn(Resource.Error("falhou"))

        viewModel.onEvent(NotificationsEvent.OnToggleAll(false))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.enabledAll)
        assertEquals("falhou", state.errorMessage)
        assertFalse(state.isSaving)
    }
}
