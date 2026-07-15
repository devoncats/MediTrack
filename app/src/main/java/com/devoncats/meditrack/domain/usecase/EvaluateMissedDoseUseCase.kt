package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.NotificationHelper

class EvaluateMissedDoseUseCase(
    private val medicationRepository: MedicationRepository,
    private val userRepository: UserRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke(medicationId: Long, scheduleId: Long) {
        val medication = medicationRepository.getMedicationById(medicationId) ?: return
        val schedule = medicationRepository.getScheduleById(scheduleId) ?: return

        // Scoped to this schedule (not just the medication) so a medication with multiple
        // schedules never has one schedule's on-time dose marked missed by another's check.
        val pendingLog = medicationRepository.getLatestPendingLogForSchedule(scheduleId)
        if (pendingLog != null) {
            medicationRepository.updateLog(pendingLog.copy(status = MedicationLogStatus.MISSED))

            val owner = userRepository.findById(medication.ownerUserId)
            if (owner?.role == UserRole.SENIOR_PATIENT) {
                // Academic workaround for CU-012: without FCM/backend, we can't push a
                // notification to the caregiver's own device, so this simulates that cross-alert
                // as a local notification on this same device.
                notificationHelper.showMissedDoseCaregiverNotification(
                    logId = pendingLog.id,
                    medicationId = medicationId,
                    seniorName = owner.name,
                    medicationName = medication.name
                )
            }
        }

        // AlarmManager only ever holds this schedule's next single trigger, so whether this
        // dose just got marked missed or had already been confirmed, line up the next
        // occurrence (and its paired missed-dose check) now that this one is resolved.
        alarmScheduler.schedule(scheduleId, medicationId, schedule.time, schedule.daysOfWeek)
    }
}
