package com.devoncats.meditrack.presentation.caregiver

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardFragmentTest {

    private val caregiverEmail = "dashboard-caregiver-test@meditrack.com"
    private val seniorEmail = "dashboard-senior-test@meditrack.com"
    private val ownMedicationName = "Own Medication Test"
    private val seniorMedicationName = "Senior Medication Test"

    private var caregiverId: Long = -1
    private var seniorId: Long = -1

    @Before
    fun seedData(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MediTrackDatabase.getInstance(context)
        val userDao = database.userDao()
        val medicationDao = database.medicationDao()
        val medicationLogDao = database.medicationLogDao()

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
                name = "Senior Test",
                email = seniorEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )

        medicationDao.insert(
            MedicationEntity(
                name = ownMedicationName,
                dose = "10mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = caregiverId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        val seniorMedicationId = medicationDao.insert(
            MedicationEntity(
                name = seniorMedicationName,
                dose = "20mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = seniorId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = seniorMedicationId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.MISSED
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

    @Test
    fun dashboard_showsOwnMedicationAndMissedDoseAlertFromLinkedSenior() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText(ownMedicationName)).check(matches(isDisplayed()))
            onView(withText("Senior Test")).check(matches(isDisplayed()))
            onView(withText(seniorMedicationName)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun viewSeniorsButton_navigatesToSeniorList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.viewSeniorsButton)).perform(click())

            onView(withId(R.id.seniorListTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun addMedicationFab_navigatesToMedForm() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addMedicationFab)).perform(click())

            onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun missedDoseAlert_click_navigatesToMissedDoseAlertScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText(seniorMedicationName)).perform(click())

            onView(withText(R.string.placeholder_missed_dose_alert)).check(matches(isDisplayed()))
        }
    }
}
