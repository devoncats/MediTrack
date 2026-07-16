package com.devoncats.meditrack.utils

import com.devoncats.meditrack.domain.model.WeekDays
import java.time.DayOfWeek

fun DayOfWeek.toCode(): String = when (this) {
    DayOfWeek.MONDAY -> "MON"
    DayOfWeek.TUESDAY -> "TUE"
    DayOfWeek.WEDNESDAY -> "WED"
    DayOfWeek.THURSDAY -> "THU"
    DayOfWeek.FRIDAY -> "FRI"
    DayOfWeek.SATURDAY -> "SAT"
    DayOfWeek.SUNDAY -> "SUN"
}

fun String.toDayOfWeekOrNull(): DayOfWeek? = when (trim().uppercase()) {
    "MON" -> DayOfWeek.MONDAY
    "TUE" -> DayOfWeek.TUESDAY
    "WED" -> DayOfWeek.WEDNESDAY
    "THU" -> DayOfWeek.THURSDAY
    "FRI" -> DayOfWeek.FRIDAY
    "SAT" -> DayOfWeek.SATURDAY
    "SUN" -> DayOfWeek.SUNDAY
    else -> null
}

// CSV <-> WeekDays: the one place that knows Schedule.daysOfWeek's on-disk string format
// ("MON,TUE,..."), so the domain model itself never has to.
fun WeekDays.toCsv(): String = days.joinToString(",") { it.toCode() }

fun String.toWeekDays(): WeekDays = WeekDays(split(",").mapNotNull { it.toDayOfWeekOrNull() }.toSet())
