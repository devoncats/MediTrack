package com.devoncats.meditrack.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.devoncats.meditrack.R

class NotificationHelper(private val context: Context) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMedicationAlarmNotification(
        logId: Long,
        scheduleId: Long,
        medicationId: Long,
        medicationName: String,
        dose: String,
        isSeniorPatient: Boolean = false
    ) {
        createChannelIfNeeded()

        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main_nav_graph)
            .setDestination(if (isSeniorPatient) R.id.seniorAlertFragment else R.id.alertFragment)
            .setArguments(bundleOf("scheduleId" to scheduleId))
            .createPendingIntent()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lucide_bell)
            .setContentTitle(context.getString(R.string.notification_medication_alarm_title, medicationName))
            .setContentText(context.getString(R.string.notification_medication_alarm_text, dose))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_lucide_check_circle,
                context.getString(R.string.notification_action_confirm),
                confirmActionPendingIntent(logId)
            )

        if (!isSeniorPatient) {
            notificationBuilder.addAction(
                R.drawable.ic_lucide_clock,
                context.getString(R.string.notification_action_postpone),
                postponeActionPendingIntent(logId, scheduleId, medicationId)
            )
        }

        NotificationManagerCompat.from(context).notify(logId.toInt(), notificationBuilder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMissedDoseCaregiverNotification(logId: Long, medicationId: Long, seniorName: String, medicationName: String) {
        createChannelIfNeeded()

        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main_nav_graph)
            .setDestination(R.id.missedDoseAlertFragment)
            .setArguments(bundleOf("logId" to logId))
            .createPendingIntent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lucide_alert_triangle)
            .setContentTitle(context.getString(R.string.notification_missed_dose_title, seniorName))
            .setContentText(context.getString(R.string.notification_missed_dose_text, medicationName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(MISSED_DOSE_NOTIFICATION_ID_OFFSET + medicationId.toInt(), notification)
    }

    private fun confirmActionPendingIntent(logId: Long): PendingIntent {
        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_CONFIRM
            putExtra(MedicationActionReceiver.EXTRA_LOG_ID, logId)
        }
        return PendingIntent.getBroadcast(
            context,
            (logId * 10 + 1).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun postponeActionPendingIntent(logId: Long, scheduleId: Long, medicationId: Long): PendingIntent {
        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_POSTPONE
            putExtra(MedicationActionReceiver.EXTRA_LOG_ID, logId)
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
        }
        return PendingIntent.getBroadcast(
            context,
            (logId * 10 + 2).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        private const val MISSED_DOSE_NOTIFICATION_ID_OFFSET = 1_000_000
    }
}
