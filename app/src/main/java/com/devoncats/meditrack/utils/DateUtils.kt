package com.devoncats.meditrack.utils

import java.time.LocalDate
import java.time.ZoneId

object DateUtils {
    fun todayRangeMillis(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val today = LocalDate.now(zoneId)
        val startInclusive = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return startInclusive to endExclusive
    }
}
