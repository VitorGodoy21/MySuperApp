package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.model.ReportDelta
import com.vfdeginformatica.mysuperapp.domain.model.ReportFigures
import com.vfdeginformatica.mysuperapp.domain.model.ReportFutureInstallments
import com.vfdeginformatica.mysuperapp.domain.model.ReportTrigger
import com.vfdeginformatica.mysuperapp.domain.repository.FinancialReportRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.report.ListFinancialReportsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.report.RequestAiNarrativeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.report.RequestFinancialReportUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.NarrativeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialReportViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeReportRepository : FinancialReportRepository {
        var reports: MutableList<FinancialReport> = mutableListOf()
        var listError: Exception? = null
        var requestResult: (YearMonth?) -> Resource<YearMonth> = { Resource.Success(it ?: YearMonth.now()) }
        val requested = mutableListOf<YearMonth?>()

        override suspend fun getReport(month: YearMonth): FinancialReport? = reports.firstOrNull { it.month == month }
        override suspend fun listReports(): List<FinancialReport> {
            listError?.let { throw it }
            return reports.sortedByDescending { it.month }
        }

        override suspend fun requestReport(month: YearMonth?): Resource<YearMonth> {
            requested += month
            return requestResult(month)
        }

        var aiResult: (YearMonth) -> Resource<Unit> = { Resource.Success(Unit) }
        val aiRequested = mutableListOf<YearMonth>()

        override suspend fun requestAiNarrative(month: YearMonth): Resource<Unit> {
            aiRequested += month
            return aiResult(month)
        }
    }

    private val repository = FakeReportRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun report(
        month: YearMonth,
        balance: Double = 0.0,
        narrative: String? = null,
        aiNarrative: String? = null,
    ) = FinancialReport(
        month = month, partial = false, generatedAt = null, trigger = ReportTrigger.SCHEDULED,
        summary = ReportFigures(transactionCount = 1, balance = balance),
        previousMonth = null, average3Months = null, invoices = emptyList(), categories = emptyList(),
        futureInstallments = ReportFutureInstallments(emptyList(), 0.0, 0, emptyList()), alerts = emptyList(),
        narrative = narrative, aiNarrative = aiNarrative,
    )

    private fun viewModel(month: String? = null) = FinancialReportViewModel(
        savedStateHandle = SavedStateHandle(if (month != null) mapOf("month" to month) else emptyMap()),
        listFinancialReportsUseCase = ListFinancialReportsUseCase(repository),
        requestFinancialReportUseCase = RequestFinancialReportUseCase(repository),
        requestAiNarrativeUseCase = RequestAiNarrativeUseCase(repository),
    )

    @Test
    fun selectsMostRecentReportWhenNoMonthRequested() = runTest(dispatcher) {
        repository.reports += listOf(report(YearMonth.of(2026, 10), 1.0), report(YearMonth.of(2026, 11), 2.0))

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), state.availableMonths)
        assertEquals(YearMonth.of(2026, 11), state.selectedMonth)
        assertEquals(2.0, state.report!!.summary.balance, 0.0)
    }

    @Test
    fun requestedMonthWithoutReportShowsEmptyStateForThatMonth() = runTest(dispatcher) {
        repository.reports += report(YearMonth.of(2026, 11))

        val vm = viewModel("2026-09")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(YearMonth.of(2026, 9), state.selectedMonth)
        assertNull(state.report)
        assertTrue(state.isEmpty)
        assertTrue(state.canGenerateSelected)
    }

    @Test
    fun monthSelectionSwitchesReport() = runTest(dispatcher) {
        repository.reports += listOf(report(YearMonth.of(2026, 10), 1.0), report(YearMonth.of(2026, 11), 2.0))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(FinancialReportEvent.OnMonthSelected(YearMonth.of(2026, 10)))

        assertEquals(1.0, vm.uiState.value.report!!.summary.balance, 0.0)
    }

    @Test
    fun listErrorIsExposedAndRetryReloads() = runTest(dispatcher) {
        repository.listError = IllegalStateException("offline")
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("offline", vm.uiState.value.errorMessage)

        repository.listError = null
        repository.reports += report(YearMonth.of(2026, 11))
        vm.onEvent(FinancialReportEvent.OnRefresh)
        advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertEquals(YearMonth.of(2026, 11), vm.uiState.value.selectedMonth)
    }

    @Test
    fun generateWithoutReportsRequestsCurrentMonthAndReloads() = runTest(dispatcher) {
        val current = YearMonth.now()
        repository.requestResult = { month ->
            repository.reports += report(current, 5.0)
            Resource.Success(month ?: current)
        }
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)

        val toasts = mutableListOf<String>()
        val job = launch { vm.effect.collect { if (it is FinancialReportEffect.ShowToast) toasts += it.message } }
        vm.onEvent(FinancialReportEvent.OnGenerateClicked)
        advanceUntilIdle()

        assertEquals(listOf<YearMonth?>(null), repository.requested)
        assertFalse(vm.uiState.value.isGenerating)
        assertEquals(current, vm.uiState.value.selectedMonth)
        assertEquals(5.0, vm.uiState.value.report!!.summary.balance, 0.0)
        assertEquals(listOf("Relatório gerado"), toasts)
        job.cancel()
    }

    @Test
    fun generateErrorKeepsStateAndToasts() = runTest(dispatcher) {
        repository.reports += report(YearMonth.of(2026, 11))
        repository.requestResult = { Resource.Error("Aguarde um instante") }
        val vm = viewModel()
        advanceUntilIdle()

        val toasts = mutableListOf<String>()
        val job = launch { vm.effect.collect { if (it is FinancialReportEffect.ShowToast) toasts += it.message } }
        vm.onEvent(FinancialReportEvent.OnGenerateClicked)
        advanceUntilIdle()

        assertEquals(listOf<YearMonth?>(YearMonth.of(2026, 11)), repository.requested)
        assertFalse(vm.uiState.value.isGenerating)
        assertEquals(YearMonth.of(2026, 11), vm.uiState.value.report!!.month)
        assertEquals(listOf("Aguarde um instante"), toasts)
        job.cancel()
    }

    @Test
    fun aiConfirmedGeneratesReloadsAndSelectsAiNarrative() = runTest(dispatcher) {
        val month = YearMonth.of(2026, 11)
        repository.reports += report(month, narrative = "# Padrão")
        repository.aiResult = {
            repository.reports[0] = report(month, narrative = "# Padrão", aiNarrative = "# IA")
            Resource.Success(Unit)
        }
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(NarrativeSource.TEMPLATE, vm.uiState.value.selectedNarrative)

        vm.onEvent(FinancialReportEvent.OnGenerateAiClicked)
        assertTrue(vm.uiState.value.showAiConfirmation)
        vm.onEvent(FinancialReportEvent.OnGenerateAiConfirmed)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showAiConfirmation)
        assertFalse(vm.uiState.value.isGeneratingAi)
        assertEquals(listOf(month), repository.aiRequested)
        assertEquals(NarrativeSource.AI, vm.uiState.value.selectedNarrative)
        assertEquals("# IA", vm.uiState.value.activeNarrative)
    }

    @Test
    fun aiErrorToastsAndKeepsTemplateNarrative() = runTest(dispatcher) {
        repository.reports += report(YearMonth.of(2026, 11), narrative = "# Padrão")
        repository.aiResult = { Resource.Error("Limite atingido") }
        val vm = viewModel()
        advanceUntilIdle()

        val toasts = mutableListOf<String>()
        val job = launch { vm.effect.collect { if (it is FinancialReportEffect.ShowToast) toasts += it.message } }
        vm.onEvent(FinancialReportEvent.OnGenerateAiConfirmed)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isGeneratingAi)
        assertEquals(NarrativeSource.TEMPLATE, vm.uiState.value.selectedNarrative)
        assertEquals("# Padrão", vm.uiState.value.activeNarrative)
        assertEquals(listOf("Limite atingido"), toasts)
        job.cancel()
    }

    @Test
    fun aiSelectionFallsBackToTemplateWhenMonthHasNoAiNarrative() = runTest(dispatcher) {
        repository.reports += listOf(
            report(YearMonth.of(2026, 10), narrative = "# Out"),
            report(YearMonth.of(2026, 11), narrative = "# Nov", aiNarrative = "# Nov IA"),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(FinancialReportEvent.OnNarrativeSourceSelected(NarrativeSource.AI))
        assertEquals("# Nov IA", vm.uiState.value.activeNarrative)

        vm.onEvent(FinancialReportEvent.OnMonthSelected(YearMonth.of(2026, 10)))
        assertEquals(NarrativeSource.TEMPLATE, vm.uiState.value.selectedNarrative)
        assertEquals("# Out", vm.uiState.value.activeNarrative)
    }

    @Test
    fun shareEmitsActiveNarrativeOrToastsWhenMissing() = runTest(dispatcher) {
        repository.reports += listOf(
            report(YearMonth.of(2026, 10)),
            report(YearMonth.of(2026, 11), narrative = "# Nov"),
        )
        val vm = viewModel()
        advanceUntilIdle()

        val effects = mutableListOf<FinancialReportEffect>()
        val job = launch { vm.effect.collect { effects += it } }

        vm.onEvent(FinancialReportEvent.OnShareClicked)
        advanceUntilIdle()
        val share = effects.single() as FinancialReportEffect.ShareText
        assertEquals("# Nov", share.text)
        assertTrue(share.subject.startsWith("Relatório financeiro"))

        effects.clear()
        vm.onEvent(FinancialReportEvent.OnMonthSelected(YearMonth.of(2026, 10)))
        vm.onEvent(FinancialReportEvent.OnShareClicked)
        advanceUntilIdle()
        assertTrue(effects.single() is FinancialReportEffect.ShowToast)
        job.cancel()
    }

    @Test
    fun formatDeltaUsesArrowsAndAbsoluteValues() {
        assertEquals("—", formatDelta(ReportDelta.EMPTY))
        assertTrue(formatDelta(ReportDelta(-150.0, -12.4)).startsWith("▼ "))
        assertTrue(formatDelta(ReportDelta(150.0, null)).startsWith("▲ "))
        assertFalse(formatDelta(ReportDelta(-150.0, -12.4)).contains("-"))
    }
}
