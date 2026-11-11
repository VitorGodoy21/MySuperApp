package com.vfdeginformatica.mysuperapp.domain.model

import com.google.firebase.Timestamp
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

/**
 * Ordenação padrão das listas de transações: data decrescente (mais recentes no topo).
 * Ordenação estável — itens com a mesma data mantêm a ordem de entrada.
 */
fun List<Transaction>.sortedByDateDesc(): List<Transaction> =
    sortedByDescending { it.date?.seconds ?: Long.MIN_VALUE }

/**
 * Re-data uma parcela (`installmentGroupId != null`) para o mês da fatura em que ela é
 * exibida, mantendo o dia da compra (limitado ao tamanho do mês), para que a lista do mês
 * ordene e mostre a parcela junto das demais transações daquele mês. Demais transações
 * são devolvidas inalteradas. Nunca é persistido: a edição só grava título/valor/categorias.
 */
fun Transaction.materializeInstallmentFor(
    month: YearMonth,
    zone: ZoneId = ZoneId.systemDefault()
): Transaction {
    if (installmentGroupId == null || isRecurring) return this
    val originalDay = date?.toDate()?.toInstant()?.atZone(zone)?.dayOfMonth ?: 1
    val day = originalDay.coerceAtMost(month.lengthOfMonth())
    return copy(date = Timestamp(Date.from(month.atDay(day).atStartOfDay(zone).toInstant())))
}
