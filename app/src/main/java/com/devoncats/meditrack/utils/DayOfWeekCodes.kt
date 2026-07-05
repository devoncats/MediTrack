package com.devoncats.meditrack.utils

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
