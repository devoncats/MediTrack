package com.devoncats.meditrack.presentation.caregiver

import android.app.PendingIntent
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.clearSessionAndDeleteUsers
import com.devoncats.meditrack.seedCaregiverAndLogIn
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.MedicationAlarmReceiver
import com.devoncats.meditrack.utils.PasswordHasher
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This emulator image duplicates every screen's decor view into a second, unfocused root
 * (an accessibility/overlay shadow, not a real second Activity), which makes plain
 * Espresso onView()/onView().inRoot() ambiguous for anything matched by text or id. Assertions
 * and clicks here go straight through the ActivityScenario's own Activity reference instead,
 * which is unambiguous by construction. The confirmation AlertDialog is a separate window that
 * isn't duplicated by this glitch, so it's still driven through plain Espresso.
 */
@RunWith(AndroidJUnit4::class)
class SeniorListFragmentTest {

    private val caregiverEmail = "seniorlist-caregiver-test@meditrack.com"
    private val seniorMissedEmail = "seniorlist-senior-missed-test@meditrack.com"
    private val seniorNoLogsEmail = "seniorlist-senior-nologs-test@meditrack.com"

    private var caregiverId: Long = -1

    @Before
    fun seedCaregiverAndSession(): Unit = runBlocking {
        caregiverId = seedCaregiverAndLogIn(caregiverEmail)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        clearSessionAndDeleteUsers(caregiverEmail, seniorMissedEmail, seniorNoLogsEmail)
    }

    private fun findTextRecursively(view: View, text: String): Boolean {
        if (view is TextView && view.text.toString() == text) return true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (findTextRecursively(view.getChildAt(i), text)) return true
            }
        }
        return false
    }

    private fun ActivityScenario<MainActivity>.assertShowsText(text: String) {
        var found = false
        onActivity { activity -> found = findTextRecursively(activity.window.decorView, text) }
        assertTrue("expected to find text \"$text\"", found)
    }

    private fun ActivityScenario<MainActivity>.assertViewDisplayed(id: Int) {
        var displayed = false
        onActivity { activity -> displayed = activity.findViewById<View>(id)?.isShown == true }
        assertTrue("expected view $id to be displayed", displayed)
    }

    private fun ActivityScenario<MainActivity>.clickViewById(id: Int) {
        onActivity { activity -> activity.findViewById<View>(id).performClick() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(300)
    }

    private fun ActivityScenario<MainActivity>.goToSeniorList() {
        clickViewById(R.id.viewSeniorsButton)
    }

    @Test
    fun seniorList_showsLinkedSeniorsWithTodayStatus(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val medicationLogDao = MediTrackDatabase.getInstance(context).medicationLogDao()

        val missedSeniorId = userDao.insert(
            UserEntity(
                name = "Senior Missed Test",
                email = seniorMissedEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )
        userDao.insert(
            UserEntity(
                name = "Senior NoLogs Test",
                email = seniorNoLogsEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )
        val medicationId = medicationDao.insert(
            MedicationEntity(
                name = "Senior Missed Medication",
                dose = "10mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = missedSeniorId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.MISSED
            )
        )

        val missedStatusText = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.med_status_missed)
        val noDosesText = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.senior_status_no_doses_today)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.goToSeniorList()

            scenario.assertShowsText("Senior Missed Test")
            scenario.assertShowsText("Senior NoLogs Test")
            scenario.assertShowsText(missedStatusText)
            scenario.assertShowsText(noDosesText)
        }
    }

    @Test
    fun addSeniorFab_navigatesToCreateSeniorPatientForm() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.goToSeniorList()
            scenario.clickViewById(R.id.addSeniorFab)

            scenario.assertViewDisplayed(R.id.nameEditText)
        }
    }

    private fun existingAlarmPendingIntent(context: android.content.Context, scheduleId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

    @Test
    fun deleteSenior_withConfirmation_cascadesMedicationsSchedulesAndLogs(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val scheduleDao = MediTrackDatabase.getInstance(context).scheduleDao()
        val medicationLogDao = MediTrackDatabase.getInstance(context).medicationLogDao()

        val seniorId = userDao.insert(
            UserEntity(
                name = "Senior Delete Test",
                email = seniorMissedEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )
        // A real photo file and a real armed alarm, so we can verify the ViewModel tears down
        // the state that lives *outside* Room (files on disk, AlarmManager/WorkManager) too —
        // CASCADE alone only cleans up DB rows.
        val photosDir = File(context.filesDir, "medications").apply { mkdirs() }
        val photoFile = File(photosDir, "senior_delete_test_photo.jpg")
        photoFile.writeBytes(byteArrayOf(1, 2, 3))
        val photoUri = "medications/${photoFile.name}"

        val medicationId = medicationDao.insert(
            MedicationEntity(
                name = "Senior Delete Medication",
                dose = "10mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = seniorId,
                photoUri = photoUri,
                createdAt = System.currentTimeMillis()
            )
        )
        val scheduleId = scheduleDao.insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,TUE")
        )
        val logId = medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = medicationId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.MISSED
            )
        )
        AlarmScheduler(context).schedule(scheduleId, medicationId, "08:00", "MON,TUE")
        assertTrue("expected the photo file to exist before deletion", photoFile.exists())
        assertTrue(
            "expected the alarm to be armed before deletion",
            existingAlarmPendingIntent(context, scheduleId) != null
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.goToSeniorList()

            scenario.assertShowsText("Senior Delete Test")
            scenario.clickViewById(R.id.deleteSeniorButton)

            onView(withText(R.string.senior_delete_confirm_positive)).perform(click())

            Thread.sleep(800)

            scenario.assertViewDisplayed(R.id.emptyStateTextView)
        }

        assertNull(userDao.findById(seniorId))
        assertNull(medicationDao.findById(medicationId))
        assertNull(scheduleDao.findById(scheduleId))
        assertNull(medicationLogDao.findById(logId))
        assertFalse("expected the photo file to be deleted", photoFile.exists())
        assertNull(
            "expected the alarm to be cancelled",
            existingAlarmPendingIntent(context, scheduleId)
        )
    }
}
