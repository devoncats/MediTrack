package com.devoncats.meditrack.presentation.patient

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
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
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.services.MedicationAlarmReceiver
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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
        userDao.findByUsername(testEmail)?.let { userDao.delete(it) }
        userId = userDao.insert(
            UserEntity(
                name = "MedDetail Test User",
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

    @Test
    fun deletingMedicationWithPhoto_removesThePhotoFileFromDisk(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fileStorageHelper = FileStorageHelper(context)
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val sourceFile = File(cameraDir, "test_source_photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val photoUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sourceFile
        )
        val photoPath = fileStorageHelper.savePhoto(photoUri)

        val medicationWithPhotoId = MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Ibuprofeno con foto",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = userId,
                photoUri = photoPath,
                createdAt = System.currentTimeMillis()
            )
        )

        assertTrue("photo file should exist before deleting the medication", File(context.filesDir, photoPath).exists())

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Ibuprofeno con foto")).perform(click())

            onView(withId(R.id.deleteButton)).perform(click())
            onView(withText(R.string.med_detail_delete_confirm_positive)).perform(click())

            Thread.sleep(500)
        }

        assertNull(MediTrackDatabase.getInstance(context).medicationDao().findById(medicationWithPhotoId))
        assertFalse(
            "photo file should be deleted along with the medication (no orphan photos)",
            File(context.filesDir, photoPath).exists()
        )
    }

    private fun savePhotoOfSize(context: android.content.Context, size: Int): String {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val sourceFile = File(cameraDir, "source_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        sourceFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sourceFile)
        return FileStorageHelper(context).savePhoto(uri)
    }

    private class DrawableIsPlaceholder : ViewAssertion {
        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            if (noViewFoundException != null) throw noViewFoundException
            val imageView = view as ImageView
            // A real photo is always loaded as a BitmapDrawable; the placeholder is the
            // ic_lucide_image vector, so this distinguishes them without depending on the
            // vector's tinted constantState (which isn't guaranteed to be shared/identical
            // across separately-inflated instances).
            assertFalse(
                "expected the placeholder icon (not a photo bitmap) to be set",
                imageView.drawable is BitmapDrawable
            )
        }
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
    fun detail_showsPlaceholder_whenMedicationHasNoPhoto() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Paracetamol")).perform(click())

            onView(withId(R.id.medicationPhoto)).check(DrawableIsPlaceholder())
        }
    }

    @Test
    fun detail_showsActualPhoto_whenMedicationHasOne(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photoPath = savePhotoOfSize(context, 30)
        MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Ibuprofeno con foto detalle",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = userId,
                photoUri = photoPath,
                createdAt = System.currentTimeMillis()
            )
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Ibuprofeno con foto detalle")).perform(click())

            onView(withId(R.id.medicationPhoto)).check(DrawableHasSize(30))
        }
    }
}
