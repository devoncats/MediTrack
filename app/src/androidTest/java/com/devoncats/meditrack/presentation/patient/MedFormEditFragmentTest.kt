package com.devoncats.meditrack.presentation.patient

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
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
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.services.MedicationAlarmReceiver
import com.devoncats.meditrack.utils.PasswordHasher
import com.google.android.material.R as MaterialR
import com.google.android.material.chip.Chip
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MedFormEditFragmentTest {

    private val testEmail = "medform-edit-test@meditrack.com"
    private lateinit var sessionManager: SessionManager
    private var userId: Long = 0
    private var medicationId: Long = 0
    private var oldScheduleId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByUsername(testEmail)?.let { userDao.delete(it) }
        userId = userDao.insert(
            UserEntity(
                name = "MedForm Edit Test User",
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
                instructions = "Con alimentos",
                ownerUserId = userId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        oldScheduleId = MediTrackDatabase.getInstance(context).scheduleDao().insert(
            ScheduleEntity(medicationId = medicationId, time = "08:00", daysOfWeek = "MON,WED")
        )
    }

    private fun clickChipCloseIcon(): ViewAction = object : ViewAction {
        override fun getConstraints(): org.hamcrest.Matcher<View> = isAssignableFrom(Chip::class.java)
        override fun getDescription() = "Click chip close icon"
        override fun perform(uiController: UiController, view: View) {
            (view as Chip).performCloseIconClick()
            uiController.loopMainThreadUntilIdle()
        }
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
    fun editForm_prefillsExistingMedicationFields() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            navigateToEditForm(scenario)

            onView(withId(R.id.medFormTitle)).check(matches(withText(R.string.med_form_edit_title)))
            onView(withId(R.id.nameEditText)).check(matches(withText("Paracetamol")))
            onView(withId(R.id.doseEditText)).check(matches(withText("500mg")))
            onView(withId(R.id.frequencyEditText)).check(matches(withText("Cada 8 horas")))
            onView(withId(R.id.instructionsEditText)).check(matches(withText("Con alimentos")))
            onView(withId(R.id.chipMonday)).check(matches(isChecked()))
            onView(withId(R.id.chipWednesday)).check(matches(isChecked()))
            onView(withText("08:00")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun editingSchedule_cancelsOldAlarmAndSchedulesNewOne() {
        // pre-existing alarm for the old schedule
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        com.devoncats.meditrack.services.AlarmScheduler(context).schedule(oldScheduleId, medicationId, "08:00", "MON,WED")
        assertNotNull(existingPendingIntent(oldScheduleId))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            navigateToEditForm(scenario)

            onView(withId(R.id.doseEditText)).perform(clearText(), typeText("1000mg"))
            closeSoftKeyboard()
            Thread.sleep(300)

            // remove the pre-filled 08:00 time chip and add a new one
            onView(withText("08:00")).perform(clickChipCloseIcon())
            onView(withId(R.id.addTimeButton)).perform(click())
            onView(withId(MaterialR.id.material_timepicker_ok_button)).perform(click())
            Thread.sleep(300)

            onView(withId(R.id.saveButton)).perform(click())
            Thread.sleep(1000)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
        }

        assertNull("old schedule's alarm should be cancelled", existingPendingIntent(oldScheduleId))

        runBlocking {
            val updatedMedication = MediTrackDatabase.getInstance(context).medicationDao().findById(medicationId)
            assertEquals("1000mg", updatedMedication?.dose)

            val schedules = MediTrackDatabase.getInstance(context).scheduleDao().getByMedication(medicationId)
            assertEquals(1, schedules.size)
            assertTrue(schedules[0].id != oldScheduleId)
            assertNotNull(existingPendingIntent(schedules[0].id))
        }
    }

    private fun navigateToEditForm(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(
                R.id.medFormFragment,
                bundleOf("medicationId" to medicationId)
            )
        }
    }

    private fun savePhotoOfSize(context: android.content.Context, size: Int): String {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val sourceFile = File(cameraDir, "source_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        sourceFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sourceFile)
        return FileStorageHelper(context).savePhoto(uri)
    }

    private class DrawableHasSize(private val expectedSize: Int) : ViewAssertion {
        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            if (noViewFoundException != null) throw noViewFoundException
            val bitmap = ((view as ImageView).drawable as? BitmapDrawable)?.bitmap
            assertNotNull("expected a photo bitmap to be set", bitmap)
            assertTrue(bitmap!!.width == expectedSize && bitmap.height == expectedSize)
        }
    }

    @Test
    fun editForm_prefillsExistingPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photoPath = savePhotoOfSize(context, 25)
        runBlocking {
            val medication = MediTrackDatabase.getInstance(context).medicationDao().findById(medicationId)!!
            MediTrackDatabase.getInstance(context).medicationDao().update(medication.copy(photoUri = photoPath))
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            navigateToEditForm(scenario)
            Thread.sleep(500)

            onView(withId(R.id.photoPreviewImageView)).check(matches(isDisplayed()))
            onView(withId(R.id.photoPreviewImageView)).check(DrawableHasSize(25))
            onView(withId(R.id.takePhotoButton)).check(matches(withText(R.string.med_form_retake_photo_button)))
        }
    }

    @Test
    fun editForm_replacingPhoto_deletesOldFileAndSavesNewOne() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val oldPhotoPath = savePhotoOfSize(context, 25)
        runBlocking {
            val medication = MediTrackDatabase.getInstance(context).medicationDao().findById(medicationId)!!
            MediTrackDatabase.getInstance(context).medicationDao().update(medication.copy(photoUri = oldPhotoPath))
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            navigateToEditForm(scenario)

            onView(withId(R.id.takePhotoButton)).perform(click())
            Thread.sleep(1500)
            onView(withId(R.id.captureButton)).perform(click())
            Thread.sleep(2000)

            onView(withId(R.id.saveButton)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            Thread.sleep(1000)

            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
        }

        runBlocking {
            val updatedMedication = MediTrackDatabase.getInstance(context).medicationDao().findById(medicationId)
            val newPhotoPath = updatedMedication?.photoUri
            assertNotNull("updated medication should still have a photoUri", newPhotoPath)
            assertNotEquals("photoUri should point to the newly captured photo", oldPhotoPath, newPhotoPath)
            assertFalse("old photo file should have been deleted", File(context.filesDir, oldPhotoPath).exists())
            assertTrue("new photo file should exist", File(context.filesDir, newPhotoPath!!).exists())
        }
    }
}
