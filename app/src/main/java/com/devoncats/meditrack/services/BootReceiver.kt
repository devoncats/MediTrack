package com.devoncats.meditrack.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.domain.usecase.RescheduleAllAlarmsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = MediTrackDatabase.getInstance(context)
                val medicationRepository = MedicationRepositoryImpl(
                    database.medicationDao(),
                    database.scheduleDao(),
                    database.medicationLogDao()
                )
                RescheduleAllAlarmsUseCase(medicationRepository, AlarmScheduler(context))()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
