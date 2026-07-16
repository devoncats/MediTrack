package com.devoncats.meditrack.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devoncats.meditrack.domain.usecase.EvaluateMissedDoseUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MissedDoseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val evaluateMissedDoseUseCase: EvaluateMissedDoseUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        if (medicationId == -1L || scheduleId == -1L) return Result.failure()

        evaluateMissedDoseUseCase(medicationId, scheduleId)

        return Result.success()
    }

    companion object {
        const val KEY_MEDICATION_ID = "medicationId"
        const val KEY_SCHEDULE_ID = "scheduleId"
    }
}
