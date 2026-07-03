package com.devoncats.meditrack.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
    }

    @After
    fun tearDown() {
        sessionManager.clearSession()
    }

    @Test
    fun savedSession_persistsAcrossNewSessionManagerInstances() {
        sessionManager.saveSession(userId = 42L, role = "CAREGIVER")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val newInstance = SessionManager(context)

        assertTrue(newInstance.isLoggedIn())
        assertEquals(42L, newInstance.getUserId())
        assertEquals("CAREGIVER", newInstance.getRole())
    }

    @Test
    fun clearSession_removesAllStoredData() {
        sessionManager.saveSession(userId = 7L, role = "PATIENT")

        sessionManager.clearSession()

        assertEquals(-1L, sessionManager.getUserId())
        assertEquals("", sessionManager.getRole())
    }

    @Test
    fun isLoggedIn_returnsFalseAfterClearSession() {
        sessionManager.saveSession(userId = 1L, role = "SENIOR_PATIENT")
        assertTrue(sessionManager.isLoggedIn())

        sessionManager.clearSession()

        assertFalse(sessionManager.isLoggedIn())
    }
}
