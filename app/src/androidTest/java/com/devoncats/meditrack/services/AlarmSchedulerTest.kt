package com.devoncats.meditrack.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val alarmScheduler = AlarmScheduler(context)

    private fun existingPendingIntent(scheduleId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

    @Test
    fun schedule_registersAPendingIntentInAlarmManager() {
        val scheduleId = 4_001L
        alarmScheduler.cancel(scheduleId)

        alarmScheduler.schedule(scheduleId, medicationId = 1L, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")

        assertNotNull(existingPendingIntent(scheduleId))

        alarmScheduler.cancel(scheduleId)
    }

    @Test
    fun cancel_removesThePendingIntent() {
        val scheduleId = 4_002L
        alarmScheduler.schedule(scheduleId, medicationId = 1L, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        assertNotNull(existingPendingIntent(scheduleId))

        alarmScheduler.cancel(scheduleId)

        assertNull(existingPendingIntent(scheduleId))
    }
}
