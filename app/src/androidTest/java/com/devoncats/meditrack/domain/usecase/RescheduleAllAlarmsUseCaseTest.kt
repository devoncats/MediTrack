package com.devoncats.meditrack.domain.usecase

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.MedicationAlarmReceiver
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RescheduleAllAlarmsUseCaseTest {

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
    fun invoke_reprogramsEveryScheduleFoundInRoom(): Unit = runBlocking {
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

        val medicationRepository = MedicationRepositoryImpl(
            MediTrackDatabase.getInstance(context).medicationDao(),
            scheduleDao,
            MediTrackDatabase.getInstance(context).medicationLogDao()
        )
        RescheduleAllAlarmsUseCase(medicationRepository, alarmScheduler)()

        assertNotNull(existingPendingIntent(scheduleId1))
        assertNotNull(existingPendingIntent(scheduleId2))

        alarmScheduler.cancel(scheduleId1)
        alarmScheduler.cancel(scheduleId2)
    }
}
