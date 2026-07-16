package com.devoncats.meditrack.presentation.patient

import android.app.PendingIntent
import android.content.Intent
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
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.MedicationAlarmReceiver
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlertFragmentTest {

    private val testEmail = "alert-fragment-test@meditrack.com"
    private lateinit var sessionManager: SessionManager
    private var userId: Long = 0
    private var medicationId: Long = 0
    private var scheduleId: Long = 0
    private var logId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByUsername(testEmail)?.let { userDao.delete(it) }
        userId = userDao.insert(
            UserEntity(
                name = "Alert Fragment Test User",
                username = testEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        sessionManager.saveSession(userId, UserRole.PATIENT.name)

        medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
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
        scheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN")
        )
        logId = MediTrackDatabase.getInstance(context).medicationLogDao().insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduleId = scheduleId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
    }

    @After
    fun tearDown() {
        sessionManager.clearSession()
    }

    private fun existingAlarmPendingIntent(scheduleId: Long): PendingIntent? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Test
    fun tappingPendingChip_opensAlertWithMedicationInfoAndConfirmingUpdatesListImmediately() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.statusChip)).check(matches(withText(R.string.med_status_pending)))
            onView(withId(R.id.statusChip)).perform(click())

            onView(withId(R.id.alertMedicationName)).check(matches(withText("Paracetamol")))
            onView(withId(R.id.alertScheduledTime)).check(matches(withText("08:00")))

            onView(withId(R.id.confirmButton)).perform(click())

            Thread.sleep(800)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.statusChip)).check(matches(withText(R.string.med_status_confirmed)))
        }

        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val log = MediTrackDatabase.getInstance(context).medicationLogDao().findById(logId)
            assertEquals(MedicationLogStatus.CONFIRMED, log?.status)
            assertNotNull(log?.confirmedAt)
        }

        // Confirming now arms the next occurrence (fix for the alarm not recurring); clean it up.
        AlarmScheduler(InstrumentationRegistry.getInstrumentation().targetContext).cancel(scheduleId)
    }

    @Test
    fun postponing_reschedulesAlarmAndLeavesLogUntouched() {
        AlarmScheduler(InstrumentationRegistry.getInstrumentation().targetContext).cancel(scheduleId)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.statusChip)).perform(click())

            onView(withId(R.id.postponeButton)).perform(click())

            Thread.sleep(800)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
        }

        assertNotNull("expected the alarm to be rescheduled", existingAlarmPendingIntent(scheduleId))

        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val log = MediTrackDatabase.getInstance(context).medicationLogDao().findById(logId)
            assertEquals(MedicationLogStatus.PENDING, log?.status)
        }

        AlarmScheduler(InstrumentationRegistry.getInstrumentation().targetContext).cancel(scheduleId)
    }

    @Test
    fun dismissing_closesWithoutChangingTheLog() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.statusChip)).perform(click())

            onView(withId(R.id.dismissButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.statusChip)).check(matches(withText(R.string.med_status_pending)))
        }

        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val log = MediTrackDatabase.getInstance(context).medicationLogDao().findById(logId)
            assertEquals(MedicationLogStatus.PENDING, log?.status)
        }
    }
}
