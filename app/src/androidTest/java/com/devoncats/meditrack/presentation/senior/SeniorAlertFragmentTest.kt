package com.devoncats.meditrack.presentation.senior

import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.getOrAwaitValue
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeniorAlertFragmentTest {

    private val seniorEmail = "senior-alert-test@meditrack.com"
    private lateinit var sessionManager: SessionManager
    private var seniorId: Long = 0
    private var medicationId: Long = 0
    private var scheduleId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByUsername(seniorEmail)?.let { userDao.delete(it) }
        seniorId = userDao.insert(
            UserEntity(
                name = "Rosa Senior Alert Test",
                username = seniorEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = null
            )
        )
        sessionManager.saveSession(seniorId, UserRole.SENIOR_PATIENT.name)

        medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Paracetamol",
                dose = "500mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = seniorId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        scheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )
        MediTrackDatabase.getInstance(context).medicationLogDao().insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = scheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
        AlarmScheduler(context).schedule(scheduleId, medicationId, "08:00", "MON,TUE,WED,THU,FRI,SAT,SUN")
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AlarmScheduler(context).cancel(scheduleId)
        sessionManager.clearSession()
    }

    private fun navigateToAlert(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(
                R.id.seniorAlertFragment,
                bundleOf("scheduleId" to scheduleId)
            )
        }
    }

    @Test
    fun alert_showsMedicationInfoAndHasNoPostponeOption() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            navigateToAlert(scenario)
            Thread.sleep(500)

            onView(withId(R.id.seniorAlertMedicationName)).check(matches(withText("Paracetamol")))
            onView(withId(R.id.seniorAlertDose)).check(matches(withText("500mg")))
            onView(withId(R.id.seniorAlertScheduledTime)).check(matches(withText("08:00")))
            onView(withId(R.id.seniorAlertConfirmButton)).check(matches(isDisplayed()))
            onView(withId(R.id.postponeButton)).check(doesNotExist())
        }
    }

    @Test
    fun confirmingDose_updatesLogToConfirmedAndSupersedesTheMissedDoseWorkerWithTheNextOccurrence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workName = AlarmScheduler.missedDoseWorkName(scheduleId)
        val workManager = WorkManager.getInstance(context)

        val enqueuedInfos = workManager.getWorkInfosForUniqueWork(workName).get()
        assertTrue(
            "expected an enqueued missed-dose check work before confirming",
            enqueuedInfos.any { it.state == WorkInfo.State.ENQUEUED }
        )
        val originalWorkId = enqueuedInfos.first { it.state == WorkInfo.State.ENQUEUED }.id

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            navigateToAlert(scenario)
            Thread.sleep(500)

            onView(withId(R.id.seniorAlertConfirmButton)).perform(click())
            Thread.sleep(1000)

            onView(withId(R.id.seniorMedListTitle)).check(matches(isDisplayed()))
        }

        val logs = MediTrackDatabase.getInstance(context).medicationLogDao()
            .observeByMedication(medicationId).getOrAwaitValue()
        assertEquals(MedicationLogStatus.CONFIRMED, logs?.firstOrNull()?.status)

        // Confirming resolves today's dose (superseding the original check work) and, per the
        // fix for the alarm not recurring, immediately arms the next occurrence's own check.
        // ExistingWorkPolicy.REPLACE drops the superseded WorkSpec outright, so the original
        // work id no longer shows up at all — a fresh, different id must be enqueued instead.
        val afterConfirmInfos = workManager.getWorkInfosForUniqueWork(workName).get()
        assertTrue(
            "expected the original missed-dose check work to no longer be enqueued",
            afterConfirmInfos.none { it.id == originalWorkId && it.state == WorkInfo.State.ENQUEUED }
        )
        assertTrue(
            "expected a freshly armed missed-dose check for the next occurrence",
            afterConfirmInfos.any { it.id != originalWorkId && it.state == WorkInfo.State.ENQUEUED }
        )

        AlarmScheduler(context).cancel(scheduleId)
    }
}
