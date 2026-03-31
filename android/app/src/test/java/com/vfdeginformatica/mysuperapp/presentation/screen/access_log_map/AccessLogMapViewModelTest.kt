package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetAccessLogsWithLocationsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetCityAccessStatisticsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.MapViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AccessLogMapViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val getAccessLogsWithLocationsUseCase: GetAccessLogsWithLocationsUseCase = mock()
    private val getCityAccessStatisticsUseCase: GetCityAccessStatisticsUseCase = mock()

    private lateinit var viewModel: AccessLogMapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AccessLogMapViewModel(
            getAccessLogsWithLocationsUseCase,
            getCityAccessStatisticsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadAccessLogs_Success() = runTest {
        // Arrange
        val qrCodeId = "test_qr_001"
        val mockAccessLogs = listOf(
            AccessLog(
                id = "log_1",
                qrCodeId = qrCodeId,
                timestamp = Date(),
                latitude = -23.6939,
                longitude = -46.565,
                city = "São Bernardo do Campo",
                country = "BR",
                userAgent = "Mozilla/5.0",
                language = "pt-BR",
                timeZone = "America/Sao_Paulo",
                platform = "Android",
                screenWidth = 1080,
                screenHeight = 1920,
                pageUrl = "https://baila.space/qr/",
                pagePath = "/qr/"
            )
        )
        val mockCityStats = listOf(
            CityAccessStatistics(
                city = "São Bernardo do Campo",
                country = "BR",
                accessCount = 1,
                lastAccessTime = Date(),
                latitude = -23.6939,
                longitude = -46.565
            )
        )

        `when`(getAccessLogsWithLocationsUseCase.invoke(qrCodeId)).thenReturn(mockAccessLogs)
        `when`(getCityAccessStatisticsUseCase.invoke(qrCodeId)).thenReturn(mockCityStats)

        // Act
        viewModel.onEvent(AccessLogMapEvent.OnLoadAccessLogs(qrCodeId))
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.first()
        assert(!state.isLoading)
        assert(state.accessLogs.size == 1)
        assert(state.cityStatistics.size == 1)
    }

    @Test
    fun testSelectCity_FiltersCorrectly() = runTest {
        // Arrange
        val qrCodeId = "test_qr_001"
        val selectedCity = "São Bernardo do Campo"

        // Act
        viewModel.onEvent(AccessLogMapEvent.OnSelectCity(selectedCity))

        // Assert
        val state = viewModel.uiState.first()
        assert(state.selectedCity == selectedCity)
    }

    @Test
    fun testClearCityFilter() = runTest {
        // Arrange
        viewModel.onEvent(AccessLogMapEvent.OnSelectCity("Some City"))
        assert(viewModel.uiState.first().selectedCity == "Some City")

        // Act
        viewModel.onEvent(AccessLogMapEvent.OnClearCityFilter)

        // Assert
        val state = viewModel.uiState.first()
        assert(state.selectedCity == null)
    }

    @Test
    fun testToggleViewMode() = runTest {
        // Arrange
        assert(viewModel.uiState.first().viewMode == MapViewMode.MAP)

        // Act
        viewModel.onEvent(AccessLogMapEvent.OnToggleViewMode)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        var state = viewModel.uiState.first()
        assert(state.viewMode == MapViewMode.CITY_LIST)

        // Act - toggle back
        viewModel.onEvent(AccessLogMapEvent.OnToggleViewMode)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        state = viewModel.uiState.first()
        assert(state.viewMode == MapViewMode.MAP)
    }
}

