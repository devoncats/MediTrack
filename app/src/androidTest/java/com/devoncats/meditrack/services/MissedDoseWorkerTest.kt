package com.devoncats.meditrack.services

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MissedDoseWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")
    }

    private suspend fun seedUserAndMedication(email: String, role: UserRole, name: String): Pair<Long, Long> {
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(email)?.let { userDao.delete(it) }
        val userId = userDao.insert(
            UserEntity(
                name = name,
                email = email,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = role,
                caregiverId = null
            )
        )
        val medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
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
        return userId to medicationId
    }

    private suspend fun seedSchedule(medicationId: Long): Long =
        MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )

    @Test
    fun doWork_marksPendingLogAsMissed_forPatientOwner(): Unit = runBlocking {
        val (_, medicationId) = seedUserAndMedication(
            "missed-worker-patient@meditrack.com",
            UserRole.PATIENT,
            "Patient Owner"
        )
        val scheduleId = seedSchedule(medicationId)
        val logId = MediTrackDatabase.getInstance(context).medicationLogDao().insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = scheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )

        val worker = TestListenableWorkerBuilder<MissedDoseWorker>(context)
            .setInputData(
                workDataOf(
                    MissedDoseWorker.KEY_MEDICATION_ID to medicationId,
                    MissedDoseWorker.KEY_SCHEDULE_ID to scheduleId
                )
            )
            .build()

        val result = worker.startWork().get()

        assertTrue(result is ListenableWorker.Result.Success)
        val log = MediTrackDatabase.getInstance(context).medicationLogDao().findById(logId)
        assertEquals(MedicationLogStatus.MISSED, log?.status)

        AlarmScheduler(context).cancel(scheduleId)
    }

    @Test
    fun doWork_notifiesCaregiver_whenOwnerIsSeniorPatient(): Unit = runBlocking {
        val (_, medicationId) = seedUserAndMedication(
            "missed-worker-senior@meditrack.com",
            UserRole.SENIOR_PATIENT,
            "Abuela Rosa"
        )
        val scheduleId = seedSchedule(medicationId)
        MediTrackDatabase.getInstance(context).medicationLogDao().insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = scheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )

        val worker = TestListenableWorkerBuilder<MissedDoseWorker>(context)
            .setInputData(
                workDataOf(
                    MissedDoseWorker.KEY_MEDICATION_ID to medicationId,
                    MissedDoseWorker.KEY_SCHEDULE_ID to scheduleId
                )
            )
            .build()

        worker.startWork().get()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationId = 1_000_000 + medicationId.toInt()
        val posted = notificationManager.activeNotifications.any { it.id == notificationId }
        assertTrue("expected a missed-dose notification for the caregiver", posted)

        notificationManager.cancel(notificationId)
        AlarmScheduler(context).cancel(scheduleId)
    }

    @Test
    fun doWork_doesNothing_whenNoPendingLogExists(): Unit = runBlocking {
        val (_, medicationId) = seedUserAndMedication(
            "missed-worker-no-log@meditrack.com",
            UserRole.PATIENT,
            "No Log Owner"
        )
        val scheduleId = seedSchedule(medicationId)

        val worker = TestListenableWorkerBuilder<MissedDoseWorker>(context)
            .setInputData(
                workDataOf(
                    MissedDoseWorker.KEY_MEDICATION_ID to medicationId,
                    MissedDoseWorker.KEY_SCHEDULE_ID to scheduleId
                )
            )
            .build()

        val result = worker.startWork().get()

        assertTrue(result is ListenableWorker.Result.Success)

        AlarmScheduler(context).cancel(scheduleId)
    }

    @Test
    fun doWork_onlyEvaluatesThePendingLogForItsOwnSchedule(): Unit = runBlocking {
        // Regression test for a medication with two schedules close together: the worker
        // for one schedule must never mark the other schedule's still-on-time dose as missed.
        val (_, medicationId) = seedUserAndMedication(
            "missed-worker-multi-schedule@meditrack.com",
            UserRole.PATIENT,
            "Multi Schedule Owner"
        )
        val morningScheduleId = seedSchedule(medicationId)
        val eveningScheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "20:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )
        val logDao = MediTrackDatabase.getInstance(context).medicationLogDao()
        val morningLogId = logDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = morningScheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
        val eveningLogId = logDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = eveningScheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )

        val worker = TestListenableWorkerBuilder<MissedDoseWorker>(context)
            .setInputData(
                workDataOf(
                    MissedDoseWorker.KEY_MEDICATION_ID to medicationId,
                    MissedDoseWorker.KEY_SCHEDULE_ID to morningScheduleId
                )
            )
            .build()

        worker.startWork().get()

        assertEquals(MedicationLogStatus.MISSED, logDao.findById(morningLogId)?.status)
        assertEquals(
            "the evening schedule's still-pending dose must not be touched",
            MedicationLogStatus.PENDING,
            logDao.findById(eveningLogId)?.status
        )

        AlarmScheduler(context).cancel(morningScheduleId)
        AlarmScheduler(context).cancel(eveningScheduleId)
    }
}
