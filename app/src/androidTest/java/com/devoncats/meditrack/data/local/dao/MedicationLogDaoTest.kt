package com.devoncats.meditrack.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
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
class MedicationLogDaoTest {

    private lateinit var database: MediTrackDatabase
    private lateinit var medicationLogDao: MedicationLogDao
    private var medicationId: Long = 0

    @Before
    fun createDatabase(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicationLogDao = database.medicationLogDao()

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

    @Test
    fun insertUpdateAndDelete_fullCrudCycleWorks() = runBlocking {
        val id = medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduledDatetime = 1_000L,
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )

        val inserted = medicationLogDao.findById(id)
        assertEquals(MedicationLogStatus.PENDING, inserted?.status)

        medicationLogDao.update(inserted!!.copy(confirmedAt = 2_000L, status = MedicationLogStatus.CONFIRMED))
        val updated = medicationLogDao.findById(id)
        assertEquals(MedicationLogStatus.CONFIRMED, updated?.status)
        assertEquals(2_000L, updated?.confirmedAt)

        medicationLogDao.delete(updated!!)
        assertNull(medicationLogDao.findById(id))
    }

    @Test
    fun observeByMedication_reflectsLogCreationAndUpdate() = runBlocking {
        val id = medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduledDatetime = 1_000L,
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )

        val afterInsert = medicationLogDao.observeByMedication(medicationId).getOrAwaitValue()
        assertEquals(1, afterInsert!!.size)
        assertEquals(MedicationLogStatus.PENDING, afterInsert[0].status)

        medicationLogDao.update(afterInsert[0].copy(confirmedAt = 2_000L, status = MedicationLogStatus.CONFIRMED))

        val afterUpdate = medicationLogDao.observeByMedication(medicationId).getOrAwaitValue()
        assertEquals(MedicationLogStatus.CONFIRMED, afterUpdate!!.first { it.id == id }.status)
    }
}
