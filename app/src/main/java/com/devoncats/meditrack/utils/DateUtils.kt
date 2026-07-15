package com.devoncats.meditrack.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    fun todayRangeMillis(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val today = LocalDate.now(zoneId)
        val startInclusive = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return startInclusive to endExclusive
    }

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime().format(TIME_FORMATTER)
}
