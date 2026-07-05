package com.devoncats.meditrack.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.devoncats.meditrack.R

class NotificationHelper(private val context: Context) {

    fun showMedicationAlarmNotification(
        logId: Long,
        scheduleId: Long,
        medicationId: Long,
        medicationName: String,
        dose: String
    ) {
        createChannelIfNeeded()

        val contentIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main_nav_graph)
            .setDestination(R.id.alertFragment)
            .setArguments(bundleOf("scheduleId" to scheduleId))
            .createPendingIntent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_medication_alarm_title, medicationName))
            .setContentText(context.getString(R.string.notification_medication_alarm_text, dose))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                context.getString(R.string.notification_action_confirm),
                confirmActionPendingIntent(logId)
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                context.getString(R.string.notification_action_postpone),
                postponeActionPendingIntent(logId, scheduleId, medicationId)
            )
            .build()

        NotificationManagerCompat.from(context).notify(logId.toInt(), notification)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
    }
}
