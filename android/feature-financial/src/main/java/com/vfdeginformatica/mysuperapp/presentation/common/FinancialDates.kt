package com.vfdeginformatica.mysuperapp.presentation.common

import com.google.firebase.Timestamp
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Converts the date chosen on a form into the `transactionDate` to persist.
 *
 * Today keeps the current time of day so newly created transactions keep their natural order in
 * the list; any other day is stored at local start of day.
 */
internal fun LocalDate.toTransactionTimestamp(
    clock: Clock = Clock.systemDefaultZone(),
): Timestamp {
    val zone: ZoneId = clock.zone
    val today = LocalDate.now(clock)
    val instant = if (this == today) {
        clock.instant()
    } else {
        atStartOfDay(zone).toInstant()
    }
    return Timestamp(Date.from(instant))
}
