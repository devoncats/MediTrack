package com.devoncats.meditrack.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devoncats.meditrack.utils.toDayOfWeekOrNull
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(scheduleId: Long, medicationId: Long, time: String, daysOfWeek: String, now: LocalDateTime = LocalDateTime.now()) {
        val triggerAtMillis = nextTriggerMillis(time, daysOfWeek, now) ?: return
        val pendingIntent = pendingIntentFor(scheduleId, medicationId)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(scheduleId: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntentFor(scheduleId: Long, medicationId: Long): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_MEDICATION_ID = "medicationId"

        internal fun nextTriggerMillis(time: String, daysOfWeek: String, now: LocalDateTime): Long? {
            val localTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return null
            val allowedDays = daysOfWeek.split(",").mapNotNull { it.toDayOfWeekOrNull() }.toSet()
            if (allowedDays.isEmpty()) return null

            for (dayOffset in 0..7) {
                val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
                if (candidateDate.dayOfWeek !in allowedDays) continue
                val candidateDateTime = candidateDate.atTime(localTime)
                if (candidateDateTime.isAfter(now)) {
                    return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
            return null
        }
    }
}
