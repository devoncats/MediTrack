package com.devoncats.meditrack.presentation.patient

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
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedListFragmentTest {

    private val testEmail = "medlist-test@meditrack.com"
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
                name = "MedList Test User",
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

    @Test
    fun emptyState_showsGuidanceMessage_whenNoMedications() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.emptyStateTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun medicationList_showsInsertedMedication() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            MediTrackDatabase.getInstance(context).medicationDao().insert(
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
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Paracetamol")).check(matches(isDisplayed()))
            onView(withText("500mg")).check(matches(isDisplayed()))
            onView(withId(R.id.emptyStateTextView)).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun fab_navigatesToMedForm() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addMedicationFab)).perform(click())

            onView(withId(R.id.medFormTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun medicationList_updatesAutomaticallyWhenLogIsConfirmed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var logId = 0L
        runBlocking {
            val id = MediTrackDatabase.getInstance(context).medicationDao().insert(
                MedicationEntity(
                    name = "Ibuprofeno",
                    dose = "400mg",
                    frequency = "Cada 12 horas",
                    instructions = null,
                    ownerUserId = userId,
                    photoUri = null,
                    createdAt = System.currentTimeMillis()
                )
            )
            logId = MediTrackDatabase.getInstance(context).medicationLogDao().insert(
                MedicationLogEntity(
                    medicationId = id,
                    scheduledDatetime = System.currentTimeMillis(),
                    confirmedAt = null,
                    status = MedicationLogStatus.PENDING
                )
            )
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText(R.string.med_status_pending)).check(matches(isDisplayed()))

            runBlocking {
                val logDao = MediTrackDatabase.getInstance(context).medicationLogDao()
                val log = logDao.findById(logId)!!
                logDao.update(log.copy(confirmedAt = System.currentTimeMillis(), status = MedicationLogStatus.CONFIRMED))
            }

            Thread.sleep(500)

            onView(withText(R.string.med_status_confirmed)).check(matches(isDisplayed()))
        }
    }
}
