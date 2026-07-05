package com.devoncats.meditrack.presentation.camera

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.clearSessionAndDeleteUsers
import com.devoncats.meditrack.seedCaregiverAndLogIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CameraFragmentTest {

    private val caregiverEmail = "camera-caregiver-test@meditrack.com"

    @Before
    fun seedCaregiver(): Unit = runBlocking {
        seedCaregiverAndLogIn(caregiverEmail)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        clearSessionAndDeleteUsers(caregiverEmail)
    }

    private fun goToMedForm() {
        onView(withId(R.id.addMedicationFab)).perform(click())
    }

    @Test
    fun takePhoto_showsPreviewInMedForm_whenPermissionGranted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)

        ActivityScenario.launch(MainActivity::class.java).use {
            goToMedForm()

            onView(withId(R.id.takePhotoButton)).perform(click())
            Thread.sleep(1500)

            onView(withId(R.id.cameraPreviewView)).check(matches(isDisplayed()))

            onView(withId(R.id.captureButton)).perform(click())
            Thread.sleep(2000)

            onView(withId(R.id.photoPreviewImageView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun cameraFragment_showsExplanatoryMessage_whenPermissionNotGranted() {
        // CAMERA permission must not already be granted to this package when this test starts
        // (revoking it here via shell would kill the very process running this instrumentation).
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        ActivityScenario.launch(MainActivity::class.java).use {
            goToMedForm()

            onView(withId(R.id.takePhotoButton)).perform(click())

            // The system permission dialog is a separate Activity; deny it directly so
            // MainActivity resumes and Espresso can check app views again.
            val denyButton = device.wait(Until.findObject(By.textContains("Don")), 5000)
            denyButton?.click()
            Thread.sleep(500)

            onView(withId(R.id.cameraPermissionDeniedLayout)).check(matches(isDisplayed()))
            onView(withId(R.id.cameraPermissionMessageTextView)).check(matches(isDisplayed()))
        }
    }
}
