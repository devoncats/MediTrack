package com.devoncats.meditrack.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootReceiverTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun existingAlarmPendingIntent(scheduleId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

    @Test
    fun bootCompleted_reschedulesAlarmsClearedByTheReboot(): Unit = runBlocking {
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        val username = "boot-receiver-test@meditrack.com"
        userDao.findByUsername(username)?.let { userDao.delete(it) }
        val userId = userDao.insert(
            UserEntity(
                name = "Boot Receiver Test User",
                username = username,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        val medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Test Med",
                dose = "1",
                frequency = "diaria",
                instructions = null,
                ownerUserId = userId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        val scheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )

        // Simulate a device reboot: the system wipes every alarm registered via AlarmManager.
        AlarmScheduler(context).cancel(scheduleId)
        assertNull(existingAlarmPendingIntent(scheduleId))

        // BOOT_COMPLETED is a protected system broadcast; apps aren't allowed to send it, even
        // to their own receiver (sendBroadcast throws SecurityException). Invoke onReceive
        // directly instead, which is why BootReceiver treats goAsync()'s result as nullable.
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        Thread.sleep(1500)

        assertNotNull("expected the alarm to be rescheduled after boot", existingAlarmPendingIntent(scheduleId))

        AlarmScheduler(context).cancel(scheduleId)
    }
}
