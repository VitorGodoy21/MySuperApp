package com.vfdeginformatica.mysuperapp.presentation.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ptBr: Locale = Locale.forLanguageTag("pt-BR")

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr)
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", ptBr)
private val weekDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", ptBr)

internal fun formatDate(date: LocalDate): String = date.format(dateFormatter)

internal fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

/** Full pt-BR week day name, capitalized (e.g. "Segunda-feira"). */
internal fun formatWeekDay(date: LocalDate): String =
    date.format(weekDayFormatter).replaceFirstChar { it.titlecase(ptBr) }

internal fun formatKg(value: Double, decimals: Int = 1): String =
    String.format(ptBr, "%.${decimals}f kg", value)

internal fun formatCm(value: Double): String =
    if (value % 1.0 == 0.0) String.format(ptBr, "%.0f cm", value)
    else String.format(ptBr, "%.1f cm", value)

internal fun formatBmi(value: Double): String = String.format(ptBr, "%.1f", value)

/** e.g. "45 min", "1h", "1h 15min". */
internal fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}min"
    }
}

internal fun formatCalories(calories: Int): String = "$calories kcal"

/** Signed difference, e.g. "+1,2 kg" / "-0,5 kg"; null when zero. */
internal fun formatKgDelta(delta: Double): String? {
    if (delta == 0.0) return null
    val sign = if (delta > 0) "+" else "-"
    return sign + formatKg(kotlin.math.abs(delta))
}

internal fun bmiCategory(bmi: Double): String = when {
    bmi < 18.5 -> "Abaixo do peso"
    bmi < 25.0 -> "Peso normal"
    bmi < 30.0 -> "Sobrepeso"
    else -> "Obesidade"
}

/**
 * Parses user-typed decimals accepting both "72,5" and "72.5". Returns null when not a
 * finite non-negative number.
 */
internal fun parseDecimal(text: String): Double? {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    val value = normalized.toDoubleOrNull() ?: return null
    return if (value.isFinite() && value >= 0) value else null
}

/** Keeps digits plus at most one decimal separator with up to [maxDecimals] digits. */
internal fun sanitizeDecimalInput(text: String, maxDecimals: Int = 2, maxIntDigits: Int = 4): String {
    val sb = StringBuilder()
    var seenSeparator = false
    var intDigits = 0
    var decDigits = 0
    for (c in text) {
        when {
            c.isDigit() -> {
                if (!seenSeparator) {
                    if (intDigits < maxIntDigits) { sb.append(c); intDigits++ }
                } else if (decDigits < maxDecimals) {
                    sb.append(c); decDigits++
                }
            }
            (c == ',' || c == '.') && !seenSeparator && intDigits > 0 -> {
                sb.append(','); seenSeparator = true
            }
        }
    }
    return sb.toString()
}

internal fun sanitizeIntInput(text: String, maxDigits: Int = 4): String =
    text.filter { it.isDigit() }.take(maxDigits).trimStart('0').let { trimmed ->
        if (trimmed.isEmpty() && text.any { it.isDigit() }) "0" else trimmed
    }
