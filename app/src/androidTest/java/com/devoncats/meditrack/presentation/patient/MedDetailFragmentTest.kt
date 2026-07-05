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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedDetailFragmentTest {

    private val testEmail = "meddetail-test@meditrack.com"
    private lateinit var sessionManager: SessionManager
    private var userId: Long = 0
    private var medicationId: Long = 0
    private var scheduleId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(testEmail)?.let { userDao.delete(it) }
        userId = userDao.insert(
            UserEntity(
                name = "MedDetail Test User",
                email = testEmail,
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
                instructions = "Con alimentos",
                ownerUserId = userId,
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
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.PENDING
            )
        )
        AlarmScheduler(context).schedule(scheduleId, medicationId, "08:00", "MON,TUE,WED,THU,FRI,SAT,SUN")
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
    fun detail_showsMedicationFieldsAndTodayHistory() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Paracetamol")).perform(click())

            onView(withId(R.id.medicationName)).check(matches(withText("Paracetamol")))
            onView(withId(R.id.medicationDose)).check(matches(withText("500mg")))
            onView(withId(R.id.medicationFrequency)).check(matches(withText("Cada 8 horas")))
            onView(withId(R.id.medicationInstructions)).check(matches(withText("Con alimentos")))
            onView(withId(R.id.logStatusChip)).check(matches(withText(R.string.med_status_pending)))
        }
    }

    @Test
    fun deletingMedication_cancelsAlarmsAndRemovesItFromTheList() {
        assertNotNullPendingIntent(scheduleId)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Paracetamol")).perform(click())

            onView(withId(R.id.deleteButton)).perform(click())
            onView(withText(R.string.med_detail_delete_confirm_positive)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.emptyStateTextView)).check(matches(isDisplayed()))
        }

        assertNull("alarm should be cancelled after deleting the medication", existingPendingIntent(scheduleId))

        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertTrue(MediTrackDatabase.getInstance(context).medicationDao().findById(medicationId) == null)
        }
    }

    private fun assertNotNullPendingIntent(scheduleId: Long) {
        org.junit.Assert.assertNotNull(existingPendingIntent(scheduleId))
    }
}
