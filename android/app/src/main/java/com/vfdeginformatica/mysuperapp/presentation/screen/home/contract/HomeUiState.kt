package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.model.MonthFinancialSummary
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.summary.WorkoutHomeSummary

data class FinanceSectionState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val summary: MonthFinancialSummary? = null,
    /** Values are masked until the user authenticates with biometrics. Never persisted. */
    val isRevealed: Boolean = false,
)

data class SmartTagsSectionState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val qrCount: Int? = null,
)

data class WorkoutSectionState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val summary: WorkoutHomeSummary? = null,
)

data class HomeUiState(
    /** Funções habilitadas no painel web; seções desabilitadas não são carregadas nem exibidas. */
    val flags: FeatureFlags = FeatureFlags(),
    val finance: FinanceSectionState = FinanceSectionState(),
    val smartTags: SmartTagsSectionState = SmartTagsSectionState(),
    val workout: WorkoutSectionState = WorkoutSectionState(),
)
