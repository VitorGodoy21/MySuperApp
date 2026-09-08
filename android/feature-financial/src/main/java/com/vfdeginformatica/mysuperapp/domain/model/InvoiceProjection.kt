package com.vfdeginformatica.mysuperapp.domain.model

/**
 * Dados da projeção de um mês alvo:
 * - [invoiceTransactions]: compras/parcelas de crédito cuja fatura cai no mês alvo (invoiceMonth == alvo)
 * - [recurringTransactions]: transações recorrentes (receitas e despesas), projetadas todo mês
 */
data class InvoiceProjection(
    val invoiceTransactions: List<Transaction>,
    val recurringTransactions: List<Transaction>
)
