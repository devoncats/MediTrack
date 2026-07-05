package com.devoncats.meditrack.domain.model

data class Schedule(
    val id: Long,
    val medicationId: Long,
    val time: String,
    val daysOfWeek: String
)
