package com.devoncats.meditrack.presentation.caregiver

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.clearSessionAndDeleteUsers
import com.devoncats.meditrack.seedCaregiverAndLogIn
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateSeniorPatientFragmentTest {

    private val caregiverEmail = "create-senior-caregiver-test@meditrack.com"
    private val seniorName = "Rosa Martinez"
    private var caregiverId: Long = -1

    private class CaptureText : ViewAction {
        var text: String? = null
        override fun getConstraints(): Matcher<View> = isAssignableFrom(TextView::class.java)
        override fun getDescription() = "capture TextView text"
        override fun perform(uiController: UiController, view: View) {
            text = (view as TextView).text.toString()
        }
    }

    @Before
    fun seedCaregiverAndSession(): Unit = runBlocking {
        caregiverId = seedCaregiverAndLogIn(caregiverEmail)
    }

    @After
    fun clearSessionAndCaregiver(): Unit = runBlocking {
        clearSessionAndDeleteUsers(caregiverEmail)
    }

    @Test
    fun createSeniorPatient_generatesLoginableCredentials() {
        val capture = CaptureText()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addSeniorPatientButton)).perform(click())

            onView(withId(R.id.nameEditText)).perform(typeText(seniorName))
            onView(withId(R.id.generateButton)).perform(click())

            Thread.sleep(500)

            onView(withId(android.R.id.message)).check(matches(isDisplayed()))
            onView(withId(android.R.id.message)).perform(capture)

            val message = capture.text ?: error("dialog message was not captured")
            val username = Regex("Usuario: (\\S+)").find(message)?.groupValues?.get(1)
                ?: error("username not found in dialog message: $message")
            val pin = Regex("PIN: (\\d{6})").find(message)?.groupValues?.get(1)
                ?: error("pin not found in dialog message: $message")

            assertEquals("pm_rosa_martinez_$caregiverId", username)

            val savedSenior = runBlocking {
                MediTrackDatabase.getInstance(
                    InstrumentationRegistry.getInstrumentation().targetContext
                ).userDao().findByEmail(username)
            }
            assertNotNull(savedSenior)
            assertEquals(UserRole.SENIOR_PATIENT, savedSenior!!.role)
            assertEquals(caregiverId, savedSenior.caregiverId)
            assertNotEquals(pin, savedSenior.passwordHash)
            org.junit.Assert.assertTrue(PasswordHasher.verify(pin, savedSenior.passwordHash))

            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.create_senior_dialog_positive))
                .perform(click())

            onView(withId(R.id.logoutButton)).perform(click())

            Thread.sleep(300)

            onView(withId(R.id.emailEditText)).perform(typeText(username))
            onView(withId(R.id.passwordEditText)).perform(typeText(pin))
            onView(withId(R.id.loginButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.loginButton)).check(doesNotExist())

            runBlocking {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                MediTrackDatabase.getInstance(context).userDao().findByEmail(username)?.let {
                    SessionManager(context).clearSession()
                    MediTrackDatabase.getInstance(context).userDao().delete(it)
                }
            }
        }
    }
}
