package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal val ptBr: Locale = Locale.forLanguageTag("pt-BR")
internal val incomeColor = Color(0xFF2E7D32)

internal fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(ptBr).format(value)

internal fun formatPercent(value: Double): String = "%.0f%%".format(ptBr, value)

/** "Outubro 2026" */
internal fun monthLabel(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, ptBr)
        .replaceFirstChar { it.uppercase(ptBr) } + " " + month.year

/** "Out/26" */
internal fun shortMonthLabel(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.SHORT, ptBr)
        .replaceFirstChar { it.uppercase(ptBr) } + "/" + "%02d".format(month.year % 100)
