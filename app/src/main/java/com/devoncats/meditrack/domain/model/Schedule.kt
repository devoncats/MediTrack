package com.devoncats.meditrack.domain.model

import java.time.LocalTime

data class Schedule(
    val id: Long,
    val medicationId: Long,
    val time: LocalTime,
    val daysOfWeek: WeekDays
)
