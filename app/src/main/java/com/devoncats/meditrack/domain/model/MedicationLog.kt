package com.devoncats.meditrack.domain.model

data class MedicationLog(
    val id: Long,
    val medicationId: Long,
    val scheduledDatetime: Long,
    val confirmedAt: Long?,
    val status: MedicationLogStatus
)
