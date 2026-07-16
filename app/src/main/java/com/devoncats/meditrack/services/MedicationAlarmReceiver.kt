package com.devoncats.meditrack.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        // Present only when this fire is a postponed repeat of an already-shown reminder
        // (see AlarmScheduler.postpone); absent on the original scheduled fire.
        val postponedLogId = intent.getLongExtra(AlarmScheduler.EXTRA_LOG_ID, -1L).takeIf { it != -1L }
        // The intended dose time, not the (possibly Doze-delayed) actual firing time; falls
        // back to "now" only for callers that predate this extra (e.g. a stale PendingIntent).
        val nominalScheduledDatetime = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT_MILLIS, -1L)
            .takeIf { it != -1L } ?: System.currentTimeMillis()
        if (medicationId == -1L || scheduleId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication != null) {
                    val logId = if (postponedLogId != null) {
                        val existingLog = medicationRepository.getLogById(postponedLogId)
                        // Dose was confirmed while the postponed alarm was in flight, or the
                        // log no longer exists: nothing to remind about anymore.
                        if (existingLog == null || existingLog.status != MedicationLogStatus.PENDING) {
                            return@launch
                        }
                        postponedLogId
                    } else {
                        medicationRepository.insertLog(
                            MedicationLog(
                                id = 0,
                                medicationId = medicationId,
                                scheduleId = scheduleId,
                                scheduledDatetime = nominalScheduledDatetime,
                                confirmedAt = null,
                                status = MedicationLogStatus.PENDING
                            )
                        )
                    }
                    val owner = userRepository.findById(medication.ownerUserId)
                    notificationHelper.showMedicationAlarmNotification(
                        logId = logId,
                        scheduleId = scheduleId,
                        medicationId = medicationId,
                        medicationName = medication.name,
                        dose = medication.dose,
                        isSeniorPatient = owner?.role == UserRole.SENIOR_PATIENT
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
