package com.devoncats.meditrack.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.model.WeekDays
import com.devoncats.meditrack.utils.PasswordHasher
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

        alarmScheduler.schedule(scheduleId, medicationId = 1L, time = LocalTime.of(8, 0), daysOfWeek = WeekDays(DayOfWeek.entries.toSet()))

        assertNotNull(existingPendingIntent(scheduleId))

        alarmScheduler.cancel(scheduleId)
    }

    @Test
    fun cancel_removesThePendingIntent() {
        val scheduleId = 4_002L
        alarmScheduler.schedule(scheduleId, medicationId = 1L, time = LocalTime.of(8, 0), daysOfWeek = WeekDays(DayOfWeek.entries.toSet()))
        assertNotNull(existingPendingIntent(scheduleId))

        alarmScheduler.cancel(scheduleId)

        assertNull(existingPendingIntent(scheduleId))
    }

    @Test
    fun rescheduleAll_reprogramsEveryScheduleFoundInRoom(): Unit = runBlocking {
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        val username = "alarm-reschedule-test@meditrack.com"
        userDao.findByUsername(username)?.let { userDao.delete(it) }
        val userId = userDao.insert(
            UserEntity(
                name = "Reschedule Test User",
                username = username,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val medicationId = medicationDao.insert(
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
        val scheduleDao = MediTrackDatabase.getInstance(context).scheduleDao()
        val scheduleId1 = scheduleDao.insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )
        val scheduleId2 = scheduleDao.insert(
            ScheduleEntity(medicationId = medicationId, time = "20:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )

        // Simulate the alarms having been cleared, e.g. by a device reboot.
        alarmScheduler.cancel(scheduleId1)
        alarmScheduler.cancel(scheduleId2)
        assertNull(existingPendingIntent(scheduleId1))
        assertNull(existingPendingIntent(scheduleId2))

        alarmScheduler.rescheduleAll()

        assertNotNull(existingPendingIntent(scheduleId1))
        assertNotNull(existingPendingIntent(scheduleId2))

        alarmScheduler.cancel(scheduleId1)
        alarmScheduler.cancel(scheduleId2)
    }

    @Test
    fun schedule_enqueuesMissedDoseCheckWork_andCancelRemovesIt() {
        val scheduleId = 4_003L
        val workName = AlarmScheduler.missedDoseWorkName(scheduleId)
        val workManager = WorkManager.getInstance(context)
        alarmScheduler.cancel(scheduleId)

        alarmScheduler.schedule(scheduleId, medicationId = 1L, time = LocalTime.of(8, 0), daysOfWeek = WeekDays(DayOfWeek.entries.toSet()))

        val enqueuedInfos = workManager.getWorkInfosForUniqueWork(workName).get()
        assertTrue(
            "expected an enqueued missed-dose check work",
            enqueuedInfos.any { it.state == WorkInfo.State.ENQUEUED }
        )

        alarmScheduler.cancel(scheduleId)

        val afterCancelInfos = workManager.getWorkInfosForUniqueWork(workName).get()
        assertTrue(afterCancelInfos.all { it.state.isFinished })
    }

    @Test
    fun exactAlarm_firesWithinRnf01ToleranceOf30Seconds() {
        // Verifies the same platform mechanism AlarmScheduler.schedule() relies on
        // (AlarmManager.setExactAndAllowWhileIdle with RTC_WAKEUP) actually fires close
        // to the requested time on this device/API level (RNF-01).
        val action = "com.devoncats.meditrack.test.ALARM_FIRED"
        val latch = CountDownLatch(1)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        grantExactAlarmPermissionIfNeeded(alarmManager)

        var firedAtMillis = 0L
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                firedAtMillis = System.currentTimeMillis()
                latch.countDown()
            }
        }
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        try {
            val targetMillis = System.currentTimeMillis() + 3_000
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                4_099,
                Intent(action).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)

            assertTrue("alarm did not fire in time", latch.await(15, TimeUnit.SECONDS))
            val deltaSeconds = abs(firedAtMillis - targetMillis) / 1000.0
            assertTrue("expected margin < 30s, was ${deltaSeconds}s", deltaSeconds < 30)
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    /**
     * SCHEDULE_EXACT_ALARM (API 31+) is a special app-op permission, not a standard
     * "dangerous" runtime permission, so UiAutomation.grantRuntimePermission() does not
     * apply to it. It also does not survive the fresh install that happens on every
     * connectedAndroidTest run. Grant it the same way `adb shell appops set <pkg>
     * SCHEDULE_EXACT_ALARM allow` would, but from inside the test itself via
     * UiAutomation.executeShellCommand, so the test is self-sufficient.
     */
    private fun grantExactAlarmPermissionIfNeeded(alarmManager: AlarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) return

        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pfd = uiAutomation.executeShellCommand("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }

        var attempts = 0
        while (!alarmManager.canScheduleExactAlarms() && attempts < 10) {
            Thread.sleep(100)
            attempts++
        }
        assertTrue("failed to grant SCHEDULE_EXACT_ALARM via appops", alarmManager.canScheduleExactAlarms())
    }
}
