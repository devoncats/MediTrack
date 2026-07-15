package com.devoncats.meditrack.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.data.repository.UserRepositoryImpl
import com.devoncats.meditrack.domain.usecase.EvaluateMissedDoseUseCase

class MissedDoseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        if (medicationId == -1L || scheduleId == -1L) return Result.failure()

        val database = MediTrackDatabase.getInstance(applicationContext)
        val medicationRepository = MedicationRepositoryImpl(
            database.medicationDao(),
            database.scheduleDao(),
            database.medicationLogDao()
        )
        val userRepository = UserRepositoryImpl(database.userDao())
        val evaluateMissedDoseUseCase = EvaluateMissedDoseUseCase(
            medicationRepository,
            userRepository,
            AlarmScheduler(applicationContext),
            NotificationHelper(applicationContext)
        )
        evaluateMissedDoseUseCase(medicationId, scheduleId)

        return Result.success()
    }

    companion object {
        const val KEY_MEDICATION_ID = "medicationId"
        const val KEY_SCHEDULE_ID = "scheduleId"
    }
}
