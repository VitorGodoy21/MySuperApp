package com.vfdeginformatica.mysuperapp.domain.model

data class NotificationSettings(
    val enabledAll: Boolean = true,
    val enabledAccess: Boolean = true,
    val enabledMuralComments: Boolean = true,
    val enabledFinancialReports: Boolean = true
)

