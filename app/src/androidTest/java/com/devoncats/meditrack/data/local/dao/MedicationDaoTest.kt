package com.devoncats.meditrack.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.getOrAwaitValue
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {

    private lateinit var database: MediTrackDatabase
    private lateinit var medicationDao: MedicationDao
    private var ownerUserId: Long = 0

    @Before
    fun createDatabase(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicationDao = database.medicationDao()

        ownerUserId = database.userDao().insert(
            UserEntity(
                name = "Owner",
                email = "owner@meditrack.com",
                passwordHash = PasswordHasher.hash("password123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun sampleMedication(name: String = "Paracetamol") = MedicationEntity(
        name = name,
        dose = "500mg",
        frequency = "Cada 8 horas",
        instructions = "Con alimentos",
        ownerUserId = ownerUserId,
        photoUri = null,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun insertUpdateAndDelete_fullCrudCycleWorks() = runBlocking {
        val id = medicationDao.insert(sampleMedication())

        val inserted = medicationDao.findById(id)
        assertEquals("Paracetamol", inserted?.name)

        medicationDao.update(inserted!!.copy(dose = "1000mg"))
        val updated = medicationDao.findById(id)
        assertEquals("1000mg", updated?.dose)

        medicationDao.delete(updated!!)
        assertNull(medicationDao.findById(id))
    }

    @Test
    fun observeByOwner_reflectsCurrentMedicationsForThatOwner() = runBlocking {
        assertTrue(medicationDao.observeByOwner(ownerUserId).getOrAwaitValue()!!.isEmpty())

        medicationDao.insert(sampleMedication())

        val updatedList = medicationDao.observeByOwner(ownerUserId).getOrAwaitValue()
        assertEquals(1, updatedList!!.size)
        assertEquals("Paracetamol", updatedList[0].name)
    }
}
