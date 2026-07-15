package com.devoncats.meditrack.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole

class MissedDoseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        if (medicationId == -1L || scheduleId == -1L) return Result.failure()

        val database = MediTrackDatabase.getInstance(applicationContext)
        val medication = database.medicationDao().findById(medicationId) ?: return Result.success()
        val schedule = database.scheduleDao().findById(scheduleId) ?: return Result.success()
        val logDao = database.medicationLogDao()

        // Scoped to this schedule (not just the medication) so a medication with multiple
        // schedules never has one schedule's on-time dose marked missed by another's check.
        val pendingLog = logDao.findLatestPendingBySchedule(scheduleId)
        if (pendingLog != null) {
            logDao.update(pendingLog.copy(status = MedicationLogStatus.MISSED))

            val owner = database.userDao().findById(medication.ownerUserId)
            if (owner?.role == UserRole.SENIOR_PATIENT) {
                // Academic workaround for CU-012: without FCM/backend, we can't push a
                // notification to the caregiver's own device, so this simulates that cross-alert
                // as a local notification on this same device.
                NotificationHelper(applicationContext).showMissedDoseCaregiverNotification(
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
        AlarmScheduler(applicationContext).schedule(scheduleId, medicationId, schedule.time, schedule.daysOfWeek)

        return Result.success()
    }

    companion object {
        const val KEY_MEDICATION_ID = "medicationId"
        const val KEY_SCHEDULE_ID = "scheduleId"
    }
}
