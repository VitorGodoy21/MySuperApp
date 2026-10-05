package com.vfdeginformatica.mysuperapp.domain.model

import com.google.firebase.Timestamp
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Regras de ocorrência de transações recorrentes.
 *
 * Uma recorrente é um único documento-modelo que "ocorre" em todo mês M tal que
 * `recurrenceStart <= M <= recurrenceEnd` (fim nulo = sem fim) e M não está em
 * `excludedMonths`. Se houver `overrides[M]`, título/valor são substituídos.
 */
object RecurrenceRules {
    val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun formatMonth(month: YearMonth): String = month.format(MONTH_FORMAT)

    fun parseMonth(value: String?): YearMonth? = value?.let {
        runCatching { YearMonth.parse(it, MONTH_FORMAT) }.getOrNull()
    }

    fun monthOf(timestamp: Timestamp?, zone: ZoneId = ZoneId.systemDefault()): YearMonth? =
        timestamp?.toDate()?.toInstant()?.atZone(zone)?.let { YearMonth.from(it) }

    fun firstDayTimestamp(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Timestamp =
        Timestamp(Date.from(month.atDay(1).atStartOfDay(zone).toInstant()))
}

/** Primeiro mês efetivo da recorrência (compat: ausente ⇒ mês da transactionDate). */
val Transaction.effectiveRecurrenceStart: YearMonth?
    get() = recurrenceStart ?: RecurrenceRules.monthOf(date)

/** Indica se esta transação recorrente possui ocorrência no mês informado. */
fun Transaction.occursIn(month: YearMonth): Boolean {
    if (!isRecurring) return false
    val start = effectiveRecurrenceStart ?: return false
    if (month < start) return false
    val end = recurrenceEnd
    if (end != null && month > end) return false
    return month !in excludedMonths
}

/**
 * Materializa a ocorrência virtual desta recorrente para o mês informado,
 * aplicando o override (se houver) e marcando `occurrenceMonth`.
 *
 * A data da ocorrência mantém o dia original da transactionDate (limitado ao
 * tamanho do mês) para que a lista mensal fique ordenada de forma natural.
 */
fun Transaction.materializeFor(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Transaction {
    val override = overrides[month]
    val originalDay = date?.toDate()?.toInstant()?.atZone(zone)?.dayOfMonth ?: 1
    val day = originalDay.coerceAtMost(month.lengthOfMonth())
    val occurrenceDate = Timestamp(Date.from(month.atDay(day).atStartOfDay(zone).toInstant()))
    return copy(
        title = override?.title ?: title,
        amount = override?.amount ?: amount,
        date = occurrenceDate,
        occurrenceMonth = month
    )
}

/** Verdadeiro quando a ocorrência exibida teve título/valor ajustados só neste mês. */
val Transaction.hasOverrideForOccurrence: Boolean
    get() = occurrenceMonth != null && overrides.containsKey(occurrenceMonth)
