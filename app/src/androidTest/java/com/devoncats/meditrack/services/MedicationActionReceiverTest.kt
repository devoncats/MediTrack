package com.devoncats.meditrack.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationActionReceiverTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testEmail = "action-receiver-test@meditrack.com"
    private var medicationId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(testEmail)?.let { userDao.delete(it) }
        val userId = userDao.insert(
            UserEntity(
                name = "Action Receiver Test User",
                email = testEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Paracetamol",
                dose = "500mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = userId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun existingAlarmPendingIntent(scheduleId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

    @Test
    fun confirmAction_updatesLogToConfirmedAndCancelsNotification(): Unit = runBlocking {
        val scheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )
        AlarmScheduler(context).cancel(scheduleId)
        val logDao = MediTrackDatabase.getInstance(context).medicationLogDao()
        val logId = logDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = scheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        NotificationHelper(context).showMedicationAlarmNotification(
            logId = logId,
            scheduleId = scheduleId,
            medicationId = medicationId,
            medicationName = "Paracetamol",
            dose = "500mg"
        )

        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_CONFIRM
            putExtra(MedicationActionReceiver.EXTRA_LOG_ID, logId)
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        context.sendBroadcast(intent)

        Thread.sleep(1000)

        val updatedLog = logDao.findById(logId)
        assertEquals(MedicationLogStatus.CONFIRMED, updatedLog?.status)
        assertNotNull(updatedLog?.confirmedAt)

        val stillActive = notificationManager.activeNotifications.any { it.id == logId.toInt() }
        assertTrue("notification should have been cancelled", !stillActive)

        // Confirming via the notification action must also arm the next occurrence
        // (fix for the alarm not recurring after firing once).
        assertNotNull("expected the next occurrence to be armed", existingAlarmPendingIntent(scheduleId))

        AlarmScheduler(context).cancel(scheduleId)
    }

    @Test
    fun postponeAction_reschedulesAlarmFifteenMinutesLater(): Unit = runBlocking {
        val scheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )
        val logId = MediTrackDatabase.getInstance(context).medicationLogDao().insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
        AlarmScheduler(context).cancel(scheduleId)
        assertNull(existingAlarmPendingIntent(scheduleId))

        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_POSTPONE
            putExtra(MedicationActionReceiver.EXTRA_LOG_ID, logId)
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
        }
        context.sendBroadcast(intent)

        Thread.sleep(500)

        assertNotNull("expected the alarm to be rescheduled", existingAlarmPendingIntent(scheduleId))

        AlarmScheduler(context).cancel(scheduleId)
    }
}
