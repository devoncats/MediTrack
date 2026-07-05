package com.devoncats.meditrack.presentation.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterFragmentTest {

    private val newPatientEmail = "register-patient-test@meditrack.com"
    private val newCaregiverEmail = "register-caregiver-test@meditrack.com"
    private val existingEmail = "register-existing-test@meditrack.com"

    @Before
    fun cleanUpTestUsers(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        listOf(newPatientEmail, newCaregiverEmail, existingEmail).forEach { email ->
            userDao.findByEmail(email)?.let { userDao.delete(it) }
        }
        userDao.insert(
            UserEntity(
                name = "Existing User",
                email = existingEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
    }

    private fun goToRegister() {
        onView(withId(R.id.registerLinkTextView)).perform(click())
    }

    @Test
    fun registerButton_isDisabledUntilAllFieldsAndRoleAreSet() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToRegister()

            onView(withId(R.id.registerButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.nameEditText)).perform(typeText("Ana Perez"))
            onView(withId(R.id.emailEditText)).perform(typeText(newPatientEmail))
            onView(withId(R.id.passwordEditText)).perform(typeText("Password123!"))
            onView(withId(R.id.registerButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.patientRadioButton)).perform(click())
            onView(withId(R.id.registerButton)).check(matches(isEnabled()))
        }
    }

    @Test
    fun register_asPatient_savesUserWithPatientRoleAndRedirectsToLogin() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToRegister()

            onView(withId(R.id.nameEditText)).perform(typeText("Ana Perez"))
            onView(withId(R.id.emailEditText)).perform(typeText(newPatientEmail))
            onView(withId(R.id.passwordEditText)).perform(typeText("Password123!"))
            onView(withId(R.id.patientRadioButton)).perform(click())
            onView(withId(R.id.registerButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.loginButton)).check(matches(isDisplayed()))

            runBlocking {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val savedUser = MediTrackDatabase.getInstance(context).userDao().findByEmail(newPatientEmail)
                assertNotNull(savedUser)
                assertEquals(UserRole.PATIENT, savedUser!!.role)
            }
        }
    }

    @Test
    fun register_asCaregiver_savesUserWithCaregiverRole() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToRegister()

            onView(withId(R.id.nameEditText)).perform(typeText("Carlos Gomez"))
            onView(withId(R.id.emailEditText)).perform(typeText(newCaregiverEmail))
            onView(withId(R.id.passwordEditText)).perform(typeText("Password123!"))
            onView(withId(R.id.caregiverRadioButton)).perform(click())
            onView(withId(R.id.registerButton)).perform(click())

            Thread.sleep(500)

            runBlocking {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val savedUser = MediTrackDatabase.getInstance(context).userDao().findByEmail(newCaregiverEmail)
                assertNotNull(savedUser)
                assertEquals(UserRole.CAREGIVER, savedUser!!.role)
            }
        }
    }

    @Test
    fun register_withInvalidEmailFormat_showsError() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToRegister()

            onView(withId(R.id.nameEditText)).perform(typeText("Ana Perez"))
            onView(withId(R.id.emailEditText)).perform(typeText("not-an-email"))
            onView(withId(R.id.passwordEditText)).perform(typeText("Password123!"))
            onView(withId(R.id.patientRadioButton)).perform(click())
            onView(withId(R.id.registerButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.errorTextView))
                .check(matches(withText(R.string.register_error_invalid_email)))
        }
    }

    @Test
    fun register_withAlreadyRegisteredEmail_showsError() {
        ActivityScenario.launch(MainActivity::class.java).use {
            goToRegister()

            onView(withId(R.id.nameEditText)).perform(typeText("Ana Perez"))
            onView(withId(R.id.emailEditText)).perform(typeText(existingEmail))
            onView(withId(R.id.passwordEditText)).perform(typeText("Password123!"))
            onView(withId(R.id.patientRadioButton)).perform(click())
            onView(withId(R.id.registerButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.errorTextView))
                .check(matches(withText(R.string.register_error_email_taken)))
        }
    }
}
