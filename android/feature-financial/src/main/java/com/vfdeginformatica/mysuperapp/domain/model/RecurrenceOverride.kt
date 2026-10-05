package com.vfdeginformatica.mysuperapp.domain.model

/** Ajuste pontual de uma ocorrência de transação recorrente em um mês específico. */
data class RecurrenceOverride(
    val amount: Double,
    val title: String
)

/** Escopo de uma edição/exclusão aplicada a uma transação recorrente. */
enum class RecurrenceEditScope {
    /** Afeta somente o mês selecionado. */
    THIS_MONTH,

    /** Afeta o mês selecionado e todos os seguintes; os anteriores são mantidos. */
    THIS_AND_FUTURE
}
