package com.devoncats.meditrack.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HHMM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

// "HH:mm" <-> LocalTime: the one place that knows Schedule.time's on-disk string format, so
// the domain model itself never has to.
fun LocalTime.toHHmm(): String = format(HHMM_FORMATTER)

fun String.toLocalTime(): LocalTime = LocalTime.parse(this, HHMM_FORMATTER)

object DateUtils {

    fun todayRangeMillis(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val today = LocalDate.now(zoneId)
        val startInclusive = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return startInclusive to endExclusive
    }

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime().format(HHMM_FORMATTER)
}
