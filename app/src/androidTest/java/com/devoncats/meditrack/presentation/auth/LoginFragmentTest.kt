package com.devoncats.meditrack.presentation.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFragmentTest {

    private val testEmail = "login-test@meditrack.com"
    private val testPassword = "TestPass123!"

    @Before
    fun seedTestUser(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByUsername(testEmail)?.let { userDao.delete(it) }
        userDao.insert(
            UserEntity(
                name = "Login Test User",
                username = testEmail,
                passwordHash = PasswordHasher.hash(testPassword),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
    }

    @After
    fun clearSessionCreatedByLogin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SessionManager(context).clearSession()
    }

    @Test
    fun loginButton_isDisabledUntilBothFieldsAreFilled() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.loginButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.emailEditText)).perform(typeText(testEmail))
            onView(withId(R.id.loginButton)).check(matches(not(isEnabled())))

            onView(withId(R.id.passwordEditText)).perform(typeText(testPassword))
            onView(withId(R.id.loginButton)).check(matches(isEnabled()))
        }
    }

    @Test
    fun wrongPassword_showsInvalidCredentialsError() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.emailEditText)).perform(typeText(testEmail))
            onView(withId(R.id.passwordEditText)).perform(typeText("wrong-password"))
            onView(withId(R.id.loginButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.errorTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun correctCredentials_navigatesAwayFromLogin() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.emailEditText)).perform(typeText(testEmail))
            onView(withId(R.id.passwordEditText)).perform(typeText(testPassword))
            onView(withId(R.id.loginButton)).perform(click())

            Thread.sleep(500)

            onView(withId(R.id.loginButton)).check(doesNotExist())
        }
    }
}
