package com.devoncats.meditrack.presentation.patient

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.getOrAwaitValue
import com.devoncats.meditrack.utils.PasswordHasher
import com.google.android.material.R as MaterialR
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MedFormFragmentTest {

    private val testEmail = "medform-test@meditrack.com"
    private lateinit var sessionManager: SessionManager
    private var userId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(testEmail)?.let { userDao.delete(it) }
        userId = userDao.insert(
            UserEntity(
                name = "MedForm Test User",
                email = testEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        sessionManager.saveSession(userId, UserRole.PATIENT.name)
    }

    @After
    fun tearDown() {
        sessionManager.clearSession()
    }

    private fun goToMedForm() {
        onView(withId(R.id.addMedicationFab)).perform(click())
    }

    @Test
    fun saveButton_isDisabledUntilRequiredFieldsDayAndTimeAreSet() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToMedForm()

            onView(withId(R.id.saveButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.nameEditText)).perform(typeText("Paracetamol"))
            onView(withId(R.id.doseEditText)).perform(typeText("500mg"))
            onView(withId(R.id.frequencyEditText)).perform(typeText("Cada 8 horas"))
            closeSoftKeyboard()
            Thread.sleep(300)
            onView(withId(R.id.saveButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.chipMonday)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.saveButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(300)

            onView(withId(R.id.saveButton)).check(matches(isEnabled()))
        }
    }

    @Test
    fun addingMedication_persistsItAndReturnsToMedicationList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToMedForm()

            onView(withId(R.id.nameEditText)).perform(typeText("Ibuprofeno"))
            onView(withId(R.id.doseEditText)).perform(typeText("400mg"))
            onView(withId(R.id.frequencyEditText)).perform(typeText("Cada 12 horas"))
            closeSoftKeyboard()
            Thread.sleep(300)
            onView(withId(R.id.chipMonday)).perform(click())
            onView(withId(R.id.chipWednesday)).perform(click())
            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(300)

            onView(withId(R.id.saveButton)).perform(click())

            Thread.sleep(1000)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
            onView(withText("Ibuprofeno")).check(matches(isDisplayed()))
        }

        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val medication = MediTrackDatabase.getInstance(context).medicationDao()
                .observeByOwner(userId).getOrAwaitValue()
                ?.first { it.name == "Ibuprofeno" }
            assertEquals("400mg", medication?.dose)

            val schedules = MediTrackDatabase.getInstance(context).scheduleDao().getByMedication(medication!!.id)
            assertEquals(1, schedules.size)
            assertEquals("MON,WED", schedules[0].daysOfWeek)
        }
    }

    @Test
    fun addingMedicationWithPhoto_persistsThePhotoToStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)

        ActivityScenario.launch(MainActivity::class.java).use {
            goToMedForm()

            onView(withId(R.id.nameEditText)).perform(typeText("Amoxicilina"))
            onView(withId(R.id.doseEditText)).perform(typeText("250mg"))
            onView(withId(R.id.frequencyEditText)).perform(typeText("Cada 8 horas"))
            closeSoftKeyboard()
            Thread.sleep(300)

            onView(withId(R.id.takePhotoButton)).perform(click())
            Thread.sleep(1500)
            onView(withId(R.id.captureButton)).perform(click())
            Thread.sleep(2000)

            onView(withId(R.id.photoPreviewImageView)).check(matches(isDisplayed()))

            onView(withId(R.id.chipMonday)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.saveButton)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            Thread.sleep(1000)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
        }

        runBlocking {
            val medication = MediTrackDatabase.getInstance(context).medicationDao()
                .observeByOwner(userId).getOrAwaitValue()
                ?.first { it.name == "Amoxicilina" }
            val photoUri = medication?.photoUri
            assertNotNull("saved medication should have a photoUri", photoUri)
            assertTrue(
                "photo file referenced by photoUri should exist on disk",
                File(context.filesDir, photoUri!!).exists()
            )
        }
    }
}
