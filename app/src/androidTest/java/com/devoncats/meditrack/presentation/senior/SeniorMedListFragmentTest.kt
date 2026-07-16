package com.devoncats.meditrack.presentation.senior

import android.view.View
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
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
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SeniorMedListFragmentTest {

    private val seniorEmail = "senior-medlist-test@meditrack.com"
    private lateinit var sessionManager: SessionManager
    private var seniorId: Long = 0

    @Before
    fun seedSeniorAndLogIn(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByUsername(seniorEmail)?.let { userDao.delete(it) }
        seniorId = userDao.insert(
            UserEntity(
                name = "Rosa Senior Test",
                username = seniorEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = null
            )
        )
        sessionManager.saveSession(seniorId, UserRole.SENIOR_PATIENT.name)
    }

    @After
    fun clearSession() {
        sessionManager.clearSession()
    }

    private fun savePhotoOfSize(context: android.content.Context, size: Int): String {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val sourceFile = File(cameraDir, "source_${System.nanoTime()}.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        sourceFile.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sourceFile)
        return FileStorageHelper(context).savePhoto(uri)
    }

    private class MinTextSizeAssertion(private val minSp: Float) : ViewAssertion {
        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            if (noViewFoundException != null) throw noViewFoundException
            val textView = view as TextView
            val actualSp = textView.textSize / textView.resources.displayMetrics.scaledDensity
            assertTrue("expected textSize >= ${minSp}sp but was ${actualSp}sp", actualSp >= minSp - 0.01f)
        }
    }

    private class MinTouchTargetAssertion(private val minDp: Float) : ViewAssertion {
        override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
            if (noViewFoundException != null) throw noViewFoundException
            val density = view!!.resources.displayMetrics.density
            val widthDp = view.width / density
            val heightDp = view.height / density
            assertTrue("expected width >= ${minDp}dp but was ${widthDp}dp", widthDp >= minDp - 0.5f)
            assertTrue("expected height >= ${minDp}dp but was ${heightDp}dp", heightDp >= minDp - 0.5f)
        }
    }

    @Test
    fun emptyState_showsGuidanceMessage_whenNoMedications() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.emptyStateTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun medicationList_showsNameDoseScheduleAndStatus(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
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
        MediTrackDatabase.getInstance(context).scheduleDao().insert(
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

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.seniorMedListTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.seniorMedicationName)).check(matches(withText("Paracetamol")))
            onView(withId(R.id.seniorMedicationDose)).check(matches(withText("500mg")))
            onView(withId(R.id.seniorMedicationSchedule)).check(matches(withText("08:00")))
            onView(withId(R.id.seniorMedicationStatusChip)).check(matches(withText(R.string.med_status_pending)))
        }
    }

    @Test
    fun medicationWithPhoto_showsThumbnail(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photoPath = savePhotoOfSize(context, 15)
        MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Ibuprofeno",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = seniorId,
                photoUri = photoPath,
                createdAt = System.currentTimeMillis()
            )
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.seniorMedicationThumbnail)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun screen_hasNoAddEditOrDeleteAffordances_confirmingReadOnlyView(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MediTrackDatabase.getInstance(context).medicationDao().insert(
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

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addMedicationFab)).check(doesNotExist())
            onView(withId(R.id.editButton)).check(doesNotExist())
            onView(withId(R.id.deleteButton)).check(doesNotExist())
        }
    }

    @Test
    fun screen_meetsAccessibilityRequirements_minFontSizeAndTouchTargets(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MediTrackDatabase.getInstance(context).medicationDao().insert(
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

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.seniorMedicationName)).check(MinTextSizeAssertion(16f))
            onView(withId(R.id.seniorMedicationDose)).check(MinTextSizeAssertion(16f))
            onView(withId(R.id.seniorMedicationSchedule)).check(MinTextSizeAssertion(16f))
            onView(withId(R.id.seniorMedicationStatusChip)).check(MinTextSizeAssertion(16f))

            onView(withId(R.id.logoutButton)).check(MinTouchTargetAssertion(48f))
        }
    }
}
