package com.vfdeginformatica.mysuperapp.data.remote.dto

/**
 * Documento bruto de `users/{uid}/financial_reports/{month}`. O relatório é um JSON aninhado
 * escrito pela Cloud Function; a conversão para o domínio fica em `FinancialReportMapper`.
 */
data class FinancialReportDto(
    val id: String,
    val data: Map<String, Any?>,
)
