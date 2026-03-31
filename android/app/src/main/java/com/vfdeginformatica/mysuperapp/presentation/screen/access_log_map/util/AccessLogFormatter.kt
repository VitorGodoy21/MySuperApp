package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for formatting access log data
 */
object AccessLogFormatter {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    fun formatTimestamp(date: Date): String {
        return dateFormat.format(date)
    }

    fun formatDateOnly(date: Date): String {
        return dateOnlyFormat.format(date)
    }

    fun getRelativeTime(date: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - date.time

        return when {
            diff < 60000 -> "Agora mesmo"
            diff < 3600000 -> "${diff / 60000} minutos atrás"
            diff < 86400000 -> "${diff / 3600000} horas atrás"
            diff < 604800000 -> "${diff / 86400000} dias atrás"
            else -> formatDateOnly(date)
        }
    }

    fun formatAccessCount(count: Int): String {
        return when {
            count == 1 -> "1 acesso"
            else -> "$count acessos"
        }
    }
}

