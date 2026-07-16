package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import javax.inject.Inject

class ConfirmDoseUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(logId: Long, scheduleId: Long): Boolean {
        val log = medicationRepository.getLogById(logId) ?: return false
        medicationRepository.updateLog(
            log.copy(confirmedAt = System.currentTimeMillis(), status = MedicationLogStatus.CONFIRMED)
        )
        // Line up the next occurrence now that this dose is resolved; this also
        // supersedes the still-pending missed-dose check for this dose.
        medicationRepository.getScheduleById(scheduleId)?.let { schedule ->
            alarmScheduler.schedule(scheduleId, log.medicationId, schedule.time, schedule.daysOfWeek)
        }
        return true
    }
}
