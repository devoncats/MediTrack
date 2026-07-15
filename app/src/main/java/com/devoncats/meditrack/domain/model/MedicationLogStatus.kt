package com.devoncats.meditrack.domain.model

enum class MedicationLogStatus {
    PENDING,
    CONFIRMED,
    MISSED;

    companion object {
        // MISSED beats PENDING beats CONFIRMED: a senior's overall "today" status should
        // surface the worst outstanding issue, not just the latest dose.
        fun aggregate(statuses: List<MedicationLogStatus>): MedicationLogStatus? = when {
            MISSED in statuses -> MISSED
            PENDING in statuses -> PENDING
            statuses.isNotEmpty() -> CONFIRMED
            else -> null
        }
    }
}
