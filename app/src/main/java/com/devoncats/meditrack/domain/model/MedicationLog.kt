package com.devoncats.meditrack.domain.model

data class MedicationLog(
    val id: Long,
    val medicationId: Long,
    val scheduleId: Long? = null,
    val scheduledDatetime: Long,
    val confirmedAt: Long?,
    val status: MedicationLogStatus
)
