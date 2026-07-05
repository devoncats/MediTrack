package com.devoncats.meditrack.domain.model

data class MissedDoseAlert(
    val logId: Long,
    val seniorName: String,
    val medicationName: String,
    val scheduledDatetime: Long
)
