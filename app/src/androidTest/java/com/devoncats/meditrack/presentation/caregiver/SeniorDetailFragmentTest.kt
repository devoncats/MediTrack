package com.devoncats.meditrack.presentation.caregiver

import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.clearText
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
import com.devoncats.meditrack.services.MedicationAlarmReceiver
import com.devoncats.meditrack.utils.PasswordHasher
import com.google.android.material.R as MaterialR
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeniorDetailFragmentTest {

    private val caregiverEmail = "seniordetail-caregiver-test@meditrack.com"
    private val seniorEmail = "seniordetail-senior-test@meditrack.com"
    private val seniorName = "Rosa Senior Detail Test"

    private var caregiverId: Long = -1
    private var seniorId: Long = -1

    @Before
    fun seedCaregiverAndSenior(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDao = MediTrackDatabase.getInstance(context).userDao()

        userDao.findByEmail(caregiverEmail)?.let { userDao.delete(it) }
        userDao.findByEmail(seniorEmail)?.let { userDao.delete(it) }

        caregiverId = userDao.insert(
            UserEntity(
                name = "Caregiver Test",
                email = caregiverEmail,
                passwordHash = PasswordHasher.hash("CaregiverPass123!"),
                role = UserRole.CAREGIVER,
                caregiverId = null
            )
        )
        seniorId = userDao.insert(
            UserEntity(
                name = seniorName,
                email = seniorEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )

        SessionManager(context).saveSession(caregiverId, UserRole.CAREGIVER.name)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SessionManager(context).clearSession()
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(seniorEmail)?.let { userDao.delete(it) }
        userDao.findByEmail(caregiverEmail)?.let { userDao.delete(it) }
    }

    private fun goToSeniorDetail() {
        onView(withId(R.id.viewSeniorsButton)).perform(click())
        onView(withText(seniorName)).perform(click())
    }

    private fun existingPendingIntent(scheduleId: Long): PendingIntent? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Test
    fun addingMedicationForSenior_assignsSeniorAsOwnerAndSchedulesAlarm(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToSeniorDetail()

            onView(withId(R.id.addMedicationFab)).perform(click())

            onView(withId(R.id.nameEditText)).perform(typeText("Paracetamol"))
            onView(withId(R.id.doseEditText)).perform(typeText("500mg"))
            onView(withId(R.id.frequencyEditText)).perform(typeText("Cada 8 horas"))
            closeSoftKeyboard()
            Thread.sleep(300)
            onView(withId(R.id.chipMonday)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(300)

            onView(withId(R.id.saveButton)).check(matches(isEnabled())).perform(click())

            Thread.sleep(1000)

            onView(withText("Paracetamol")).check(matches(isDisplayed()))
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val scheduleDao = MediTrackDatabase.getInstance(context).scheduleDao()

        val medication = medicationDao.observeByOwner(seniorId).getOrAwaitValue()
            ?.firstOrNull { it.name == "Paracetamol" }
        assertNotNull("medication should be owned by the senior, not the caregiver", medication)
        assertEquals(seniorId, medication!!.ownerUserId)

        val schedules = scheduleDao.getByMedication(medication.id)
        assertEquals(1, schedules.size)
        assertNotNull("an alarm should be scheduled for the senior's medication", existingPendingIntent(schedules[0].id))
    }

    @Test
    fun editingSeniorMedication_keepsSeniorAsOwner(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToSeniorDetail()

            onView(withId(R.id.addMedicationFab)).perform(click())
            onView(withId(R.id.nameEditText)).perform(typeText("Ibuprofeno"))
            onView(withId(R.id.doseEditText)).perform(typeText("400mg"))
            onView(withId(R.id.frequencyEditText)).perform(typeText("Cada 12 horas"))
            closeSoftKeyboard()
            Thread.sleep(300)
            onView(withId(R.id.chipMonday)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.saveButton)).check(matches(isEnabled())).perform(click())
            Thread.sleep(1000)

            onView(withText("Ibuprofeno")).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.editButton)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.doseEditText)).perform(clearText(), typeText("600mg"))
            closeSoftKeyboard()
            Thread.sleep(300)
            onView(withId(R.id.saveButton)).perform(click())
            Thread.sleep(1000)

            onView(withText("600mg")).check(matches(isDisplayed()))
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val medication = medicationDao.observeByOwner(seniorId).getOrAwaitValue()
            ?.firstOrNull { it.name == "Ibuprofeno" }
        assertNotNull("edited medication should still be owned by the senior", medication)
        assertEquals(seniorId, medication!!.ownerUserId)
        assertEquals("600mg", medication.dose)

        assertNull(
            "the edited medication should not have been reassigned to the caregiver",
            medicationDao.observeByOwner(caregiverId).getOrAwaitValue()?.firstOrNull { it.id == medication.id }
        )
    }

    @Test
    fun deletingSeniorMedication_removesItAndCancelsAlarm(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToSeniorDetail()

            onView(withId(R.id.addMedicationFab)).perform(click())
            onView(withId(R.id.nameEditText)).perform(typeText("Aspirina"))
            onView(withId(R.id.doseEditText)).perform(typeText("100mg"))
            onView(withId(R.id.frequencyEditText)).perform(typeText("Cada 24 horas"))
            closeSoftKeyboard()
            Thread.sleep(300)
            onView(withId(R.id.chipWednesday)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(300)
            onView(withId(R.id.saveButton)).check(matches(isEnabled())).perform(click())
            Thread.sleep(1000)

            onView(withText("Aspirina")).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.deleteButton)).perform(click())
            onView(withText(R.string.med_detail_delete_confirm_positive)).perform(click())
            Thread.sleep(1000)

            onView(withId(R.id.emptyStateTextView)).check(matches(isDisplayed()))
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val remaining = medicationDao.observeByOwner(seniorId).getOrAwaitValue()
            ?.firstOrNull { it.name == "Aspirina" }
        assertNull("deleted medication should no longer exist", remaining)
    }
}
