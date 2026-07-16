package com.devoncats.meditrack.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.Schedule
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.model.WeekDays
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.getOrAwaitValue
import com.devoncats.meditrack.utils.PasswordHasher
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationRepositoryTest {

    private lateinit var database: MediTrackDatabase
    private lateinit var repository: MedicationRepository
    private var ownerUserId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicationRepositoryImpl(
            database.medicationDao(),
            database.scheduleDao(),
            database.medicationLogDao()
        )
        ownerUserId = database.userDao().insert(
            UserEntity(
                name = "Owner",
                username = "owner@meditrack.com",
                passwordHash = PasswordHasher.hash("password123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeMedicationsByOwner_reflectsInsertedDomainMedication() = runBlocking {
        assertEquals(0, repository.observeMedicationsByOwner(ownerUserId).getOrAwaitValue()!!.size)

        val medicationId = repository.insertMedication(
            Medication(
                id = 0,
                name = "Ibuprofeno",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = ownerUserId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )

        val medications = repository.observeMedicationsByOwner(ownerUserId).getOrAwaitValue()
        assertEquals(1, medications!!.size)
        assertEquals("Ibuprofeno", medications[0].name)
        assertEquals(medicationId, medications[0].id)
    }

    @Test
    fun scheduleAndLog_areCreatedAndUpdatedThroughRepository() = runBlocking {
        val medicationId = repository.insertMedication(
            Medication(
                id = 0,
                name = "Ibuprofeno",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = ownerUserId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )

        repository.insertSchedule(
            Schedule(
                id = 0,
                medicationId = medicationId,
                time = LocalTime.of(8, 0),
                daysOfWeek = WeekDays(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
            )
        )
        val schedules = repository.observeSchedulesByMedication(medicationId).getOrAwaitValue()
        assertEquals(1, schedules!!.size)

        val logId = repository.insertLog(
            MedicationLog(
                id = 0,
                medicationId = medicationId,
                scheduledDatetime = 1_000L,
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
        var logs = repository.observeLogsByMedication(medicationId).getOrAwaitValue()
        assertEquals(MedicationLogStatus.PENDING, logs!!.first { it.id == logId }.status)

        repository.updateLog(logs.first { it.id == logId }.copy(confirmedAt = 2_000L, status = MedicationLogStatus.CONFIRMED))

        logs = repository.observeLogsByMedication(medicationId).getOrAwaitValue()
        assertEquals(MedicationLogStatus.CONFIRMED, logs!!.first { it.id == logId }.status)
    }
}
