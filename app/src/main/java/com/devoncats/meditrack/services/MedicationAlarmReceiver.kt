package com.devoncats.meditrack.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        if (medicationId == -1L || scheduleId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = MediTrackDatabase.getInstance(context)
                val medication = database.medicationDao().findById(medicationId)
                if (medication != null) {
                    val logId = database.medicationLogDao().insert(
                        MedicationLogEntity(
                            medicationId = medicationId,
                            scheduledDatetime = System.currentTimeMillis(),
                            confirmedAt = null,
                            status = MedicationLogStatus.PENDING
                        )
                    )
                    val owner = database.userDao().findById(medication.ownerUserId)
                    NotificationHelper(context).showMedicationAlarmNotification(
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
