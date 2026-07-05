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
        if (medicationId == -1L) return Result.failure()

        val database = MediTrackDatabase.getInstance(applicationContext)
        val logDao = database.medicationLogDao()
        val log = logDao.findLatestPendingByMedication(medicationId) ?: return Result.success()

        logDao.update(log.copy(status = MedicationLogStatus.MISSED))

        val medication = database.medicationDao().findById(medicationId) ?: return Result.success()
        val owner = database.userDao().findById(medication.ownerUserId) ?: return Result.success()

        if (owner.role == UserRole.SENIOR_PATIENT) {
            // Academic workaround for CU-012: without FCM/backend, we can't push a
            // notification to the caregiver's own device, so this simulates that cross-alert
            // as a local notification on this same device.
            NotificationHelper(applicationContext).showMissedDoseCaregiverNotification(
                logId = log.id,
                medicationId = medicationId,
                seniorName = owner.name,
                medicationName = medication.name
            )
        }

        return Result.success()
    }

    companion object {
        const val KEY_MEDICATION_ID = "medicationId"
    }
}
