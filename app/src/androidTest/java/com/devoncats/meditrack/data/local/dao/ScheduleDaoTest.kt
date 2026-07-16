package com.devoncats.meditrack.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.getOrAwaitValue
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {

    private lateinit var database: MediTrackDatabase
    private lateinit var scheduleDao: ScheduleDao
    private var medicationId: Long = 0

    @Before
    fun createDatabase(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduleDao = database.scheduleDao()

        val ownerUserId = database.userDao().insert(
            UserEntity(
                name = "Owner",
                username = "owner@meditrack.com",
                passwordHash = PasswordHasher.hash("password123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        medicationId = database.medicationDao().insert(
            MedicationEntity(
                name = "Paracetamol",
                dose = "500mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = ownerUserId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun sampleSchedule(time: String = "08:00") = ScheduleEntity(
        medicationId = medicationId,
        time = time,
        daysOfWeek = "MON,WED,FRI"
    )

    @Test
    fun insertUpdateAndDelete_fullCrudCycleWorks() = runBlocking {
        val id = scheduleDao.insert(sampleSchedule())

        val inserted = scheduleDao.findById(id)
        assertEquals("08:00", inserted?.time)

        scheduleDao.update(inserted!!.copy(time = "09:00"))
        val updated = scheduleDao.findById(id)
        assertEquals("09:00", updated?.time)

        scheduleDao.delete(updated!!)
        assertNull(scheduleDao.findById(id))
    }

    @Test
    fun observeByMedication_reflectsCurrentSchedulesForThatMedication() = runBlocking {
        scheduleDao.insert(sampleSchedule("08:00"))
        scheduleDao.insert(sampleSchedule("20:00"))

        val schedules = scheduleDao.observeByMedication(medicationId).getOrAwaitValue()
        assertEquals(2, schedules!!.size)
        assertEquals(2, scheduleDao.getByMedication(medicationId).size)
    }
}
