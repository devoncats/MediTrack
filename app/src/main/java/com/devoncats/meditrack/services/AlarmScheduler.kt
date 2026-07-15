package com.devoncats.meditrack.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.domain.model.WeekDays
import com.devoncats.meditrack.utils.toLocalTime
import com.devoncats.meditrack.utils.toWeekDays
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun schedule(
        scheduleId: Long,
        medicationId: Long,
        time: LocalTime,
        daysOfWeek: WeekDays,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val triggerAtMillis = nextTriggerMillis(time, daysOfWeek, now) ?: return
        // Carrying the nominal trigger time lets MedicationAlarmReceiver stamp the log with the
        // intended dose time instead of the actual (possibly Doze-delayed) firing time.
        val pendingIntent = pendingIntentFor(scheduleId, medicationId, triggerAtMillis = triggerAtMillis)
        setExactAlarm(triggerAtMillis, pendingIntent)
        enqueueMissedDoseCheck(scheduleId, medicationId, triggerAtMillis)
    }

    fun postpone(
        scheduleId: Long,
        medicationId: Long,
        logId: Long,
        minutes: Long = POSTPONE_MINUTES,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val triggerAtMillis = postponeTriggerMillis(now, minutes)
        // Carrying the original logId lets MedicationAlarmReceiver recognize this re-fire
        // as a reminder repeat and reuse the existing log instead of inserting a duplicate.
        val pendingIntent = pendingIntentFor(scheduleId, medicationId, logId)
        setExactAlarm(triggerAtMillis, pendingIntent)
        enqueueMissedDoseCheck(scheduleId, medicationId, triggerAtMillis)
    }

    suspend fun rescheduleAll() {
        val scheduleDao = MediTrackDatabase.getInstance(context).scheduleDao()
        scheduleDao.getAll().forEach { entity ->
            schedule(entity.id, entity.medicationId, entity.time.toLocalTime(), entity.daysOfWeek.toWeekDays())
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
        workManager.cancelUniqueWork(missedDoseWorkName(scheduleId))
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun enqueueMissedDoseCheck(scheduleId: Long, medicationId: Long, triggerAtMillis: Long) {
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()) +
            TimeUnit.MINUTES.toMillis(MISSED_DOSE_CHECK_DELAY_MINUTES)
        val request = OneTimeWorkRequestBuilder<MissedDoseWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    MissedDoseWorker.KEY_MEDICATION_ID to medicationId,
                    MissedDoseWorker.KEY_SCHEDULE_ID to scheduleId
                )
            )
            .build()
        workManager.enqueueUniqueWork(missedDoseWorkName(scheduleId), ExistingWorkPolicy.REPLACE, request)
    }

    private fun pendingIntentFor(
        scheduleId: Long,
        medicationId: Long,
        logId: Long? = null,
        triggerAtMillis: Long? = null
    ): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            if (logId != null) putExtra(EXTRA_LOG_ID, logId)
            if (triggerAtMillis != null) putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            // Room ids are Long-autoincrement while PendingIntent request codes are Int; this
            // truncates. Accepted for this local, single-device app whose schedule count will
            // never realistically approach Int.MAX_VALUE rows.
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_MEDICATION_ID = "medicationId"
        const val EXTRA_LOG_ID = "logId"
        const val EXTRA_TRIGGER_AT_MILLIS = "triggerAtMillis"
        const val POSTPONE_MINUTES = 15L
        const val MISSED_DOSE_CHECK_DELAY_MINUTES = 30L

        internal fun missedDoseWorkName(scheduleId: Long) = "missed_dose_check_$scheduleId"

        internal fun postponeTriggerMillis(now: LocalDateTime, minutes: Long): Long =
            now.plusMinutes(minutes).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        internal fun nextTriggerMillis(time: LocalTime, daysOfWeek: WeekDays, now: LocalDateTime): Long? {
            val allowedDays = daysOfWeek.days
            if (allowedDays.isEmpty()) return null

            for (dayOffset in 0..7) {
                val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
                if (candidateDate.dayOfWeek !in allowedDays) continue
                val candidateDateTime = candidateDate.atTime(time)
                if (candidateDateTime.isAfter(now)) {
                    return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
            return null
        }
    }
}
