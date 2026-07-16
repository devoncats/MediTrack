package com.devoncats.meditrack.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: MediTrackDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndFindByUsername_returnsUserWithHashedPassword() = runBlocking {
        val hashedPassword = PasswordHasher.hash("Sup3rSecret!")
        val user = UserEntity(
            name = "Jane Doe",
            username = "jane@meditrack.com",
            passwordHash = hashedPassword,
            role = UserRole.PATIENT,
            caregiverId = null
        )

        val id = userDao.insert(user)
        val found = userDao.findByUsername("jane@meditrack.com")

        assertNotNull(found)
        assertEquals(id, found?.id)
        assertEquals("jane@meditrack.com", found?.username)
        assertEquals(hashedPassword, found?.passwordHash)
        assertTrue(PasswordHasher.verify("Sup3rSecret!", found!!.passwordHash))
    }

    @Test
    fun deleteUser_removesItFromDatabase() = runBlocking {
        val user = UserEntity(
            name = "John Doe",
            username = "john@meditrack.com",
            passwordHash = PasswordHasher.hash("anotherPassword"),
            role = UserRole.CAREGIVER,
            caregiverId = null
        )
        val id = userDao.insert(user)
        val inserted = userDao.findById(id)!!

        userDao.delete(inserted)

        assertNull(userDao.findByUsername("john@meditrack.com"))
    }
}
