package com.devoncats.meditrack.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.devoncats.meditrack.domain.usecase.ConfirmDoseUseCase
import com.devoncats.meditrack.domain.usecase.PostponeDoseUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var confirmDoseUseCase: ConfirmDoseUseCase
    @Inject lateinit var postponeDoseUseCase: PostponeDoseUseCase

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
