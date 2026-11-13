package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialReport
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.report.ListFinancialReportsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.report.RequestAiNarrativeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.report.RequestFinancialReportUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.monthLabel
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.NarrativeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class FinancialReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listFinancialReportsUseCase: ListFinancialReportsUseCase,
    private val requestFinancialReportUseCase: RequestFinancialReportUseCase,
    private val requestAiNarrativeUseCase: RequestAiNarrativeUseCase,
) : ViewModel() {

    companion object {
        const val ARG_MONTH = "month"
    }

    /** Mês pedido pela navegação; some depois que o usuário escolhe outro. */
    private var requestedMonth: YearMonth? = savedStateHandle.get<String>(ARG_MONTH)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

    private val _uiState = MutableStateFlow(
        FinancialReportUiState(selectedMonth = requestedMonth, currentMonth = YearMonth.now())
    )
    val uiState: StateFlow<FinancialReportUiState> = _uiState

    private val _effect = MutableSharedFlow<FinancialReportEffect>()
    val effect: SharedFlow<FinancialReportEffect> = _effect

    private var reports: List<FinancialReport> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onEvent(event: FinancialReportEvent) {
        when (event) {
            FinancialReportEvent.OnRefresh -> load()

            is FinancialReportEvent.OnMonthSelected -> {
                requestedMonth = null
                val report = reports.find(event.month)
                _uiState.update {
                    it.copy(selectedMonth = event.month, report = report, selectedNarrative = it.selectedNarrative.coerceFor(report))
                }
            }

            FinancialReportEvent.OnGenerateClicked -> generate()

            is FinancialReportEvent.OnTabSelected -> _uiState.update { it.copy(selectedTab = event.tab) }

            is FinancialReportEvent.OnNarrativeSourceSelected ->
                _uiState.update { it.copy(selectedNarrative = event.source.coerceFor(it.report)) }

            FinancialReportEvent.OnGenerateAiClicked -> {
                if (_uiState.value.report != null && !_uiState.value.isGeneratingAi) {
                    _uiState.update { it.copy(showAiConfirmation = true) }
                }
            }

            FinancialReportEvent.OnGenerateAiDismissed -> _uiState.update { it.copy(showAiConfirmation = false) }

            FinancialReportEvent.OnGenerateAiConfirmed -> {
                _uiState.update { it.copy(showAiConfirmation = false) }
                generateAi()
            }

            FinancialReportEvent.OnShareClicked -> withActiveNarrative { month, text ->
                _effect.emit(FinancialReportEffect.ShareText("Relatório financeiro — ${monthLabel(month)}", text))
            }

            FinancialReportEvent.OnCopyClicked -> withActiveNarrative { _, text ->
                _effect.emit(FinancialReportEffect.CopyText(text))
                _effect.emit(FinancialReportEffect.ShowToast("Texto copiado"))
            }
        }
    }

    private fun withActiveNarrative(block: suspend (YearMonth, String) -> Unit) {
        val state = _uiState.value
        val text = state.activeNarrative
        val month = state.report?.month
        viewModelScope.launch {
            if (text.isNullOrBlank() || month == null) {
                _effect.emit(FinancialReportEffect.ShowToast("Este relatório ainda não tem texto. Regere-o primeiro."))
            } else {
                block(month, text)
            }
        }
    }

    /** IA só pode ficar selecionada quando o relatório atual tem análise de IA. */
    private fun NarrativeSource.coerceFor(report: FinancialReport?): NarrativeSource =
        if (this == NarrativeSource.AI && report?.hasAiNarrative != true) NarrativeSource.TEMPLATE else this

    private fun generateAi() {
        val state = _uiState.value
        val month = state.report?.month ?: return
        if (state.isGeneratingAi) return
        viewModelScope.launch {
            requestAiNarrativeUseCase(month).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isGeneratingAi = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isGeneratingAi = false, selectedNarrative = NarrativeSource.AI) }
                        _effect.emit(FinancialReportEffect.ShowToast("Análise com IA pronta"))
                        load()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isGeneratingAi = false) }
                        _effect.emit(
                            FinancialReportEffect.ShowToast(resource.message ?: "Não foi possível gerar a análise com IA")
                        )
                    }
                }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            listFinancialReportsUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                    is Resource.Success -> {
                        reports = resource.data.orEmpty()
                        val months = reports.map { it.month }
                        val selected = _uiState.value.selectedMonth
                            ?.takeIf { requestedMonth != null || it in months }
                            ?: months.firstOrNull()
                        val selectedReport = reports.find(selected)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null,
                                availableMonths = months,
                                selectedMonth = selected,
                                report = selectedReport,
                                selectedNarrative = it.selectedNarrative.coerceFor(selectedReport),
                            )
                        }
                    }

                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = resource.message ?: "Erro ao carregar o relatório")
                    }
                }
            }
        }
    }

    private fun generate() {
        if (_uiState.value.isGenerating) return
        // Sem mês selecionado gera o corrente (parcial); meses futuros são recusados pelo use case.
        val month = _uiState.value.selectedMonth
        viewModelScope.launch {
            requestFinancialReportUseCase(month, _uiState.value.currentMonth).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isGenerating = true) }

                    is Resource.Success -> {
                        val generated = resource.data ?: month
                        requestedMonth = generated
                        _uiState.update { it.copy(isGenerating = false, selectedMonth = generated) }
                        _effect.emit(FinancialReportEffect.ShowToast("Relatório gerado"))
                        load()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isGenerating = false) }
                        _effect.emit(
                            FinancialReportEffect.ShowToast(resource.message ?: "Não foi possível gerar o relatório")
                        )
                    }
                }
            }
        }
    }

    private fun List<FinancialReport>.find(month: YearMonth?): FinancialReport? =
        month?.let { m -> firstOrNull { it.month == m } }
}
