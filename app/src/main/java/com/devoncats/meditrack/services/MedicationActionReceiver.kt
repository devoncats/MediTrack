package com.devoncats.meditrack.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.domain.usecase.ConfirmDoseUseCase
import com.devoncats.meditrack.domain.usecase.PostponeDoseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra(EXTRA_LOG_ID, -1L)
        if (logId == -1L) return

        when (intent.action) {
            ACTION_CONFIRM -> confirmDose(context, intent, logId)
            ACTION_POSTPONE -> postponeDose(context, intent, logId)
        }
    }

    private fun confirmDose(context: Context, intent: Intent, logId: Long) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = MediTrackDatabase.getInstance(context)
                val medicationRepository = MedicationRepositoryImpl(
                    database.medicationDao(),
                    database.scheduleDao(),
                    database.medicationLogDao()
                )
                val confirmDoseUseCase = ConfirmDoseUseCase(medicationRepository, AlarmScheduler(context))
                confirmDoseUseCase(logId, scheduleId)
                NotificationManagerCompat.from(context).cancel(logId.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postponeDose(context: Context, intent: Intent, logId: Long) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        if (scheduleId == -1L || medicationId == -1L) return

        val postponeDoseUseCase = PostponeDoseUseCase(AlarmScheduler(context))
        postponeDoseUseCase(scheduleId, medicationId, logId)
        NotificationManagerCompat.from(context).cancel(logId.toInt())
    }

    companion object {
        const val ACTION_CONFIRM = "com.devoncats.meditrack.action.CONFIRM_DOSE"
        const val ACTION_POSTPONE = "com.devoncats.meditrack.action.POSTPONE_DOSE"
        const val EXTRA_LOG_ID = "logId"
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_MEDICATION_ID = "medicationId"
    }
}
