package com.devoncats.meditrack

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySessionTest {

    private val testEmail = "session-test@meditrack.com"
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(testEmail)?.let { userDao.delete(it) }
        userDao.insert(
            UserEntity(
                name = "Session Test User",
                email = testEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
    }

    @After
    fun tearDown() {
        sessionManager.clearSession()
    }

    @Test
    fun activeSession_skipsLoginAndShowsRoleGraphDirectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = runBlocking { MediTrackDatabase.getInstance(context).userDao().findByEmail(testEmail)!! }
        sessionManager.saveSession(user.id, UserRole.PATIENT.name)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.medListTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun logout_clearsSessionAndRedirectsToLogin() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val user = MediTrackDatabase.getInstance(context).userDao().findByEmail(testEmail)!!
            sessionManager.saveSession(user.id, UserRole.PATIENT.name)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.logoutButton)).perform(click())

            Thread.sleep(300)

            onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
            assertFalse(sessionManager.isLoggedIn())
        }
    }

    @Test
    fun invalidRoleInSession_clearsSessionAndShowsLogin() {
        sessionManager.saveSession(userId = 999L, role = "NOT_A_REAL_ROLE")

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
            assertFalse(sessionManager.isLoggedIn())
        }
    }
}
