package com.devoncats.meditrack.presentation.caregiver

import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.MainActivity
import com.devoncats.meditrack.R
import com.devoncats.meditrack.clearSessionAndDeleteUsers
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.EmergencyContactEntity
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.seedCaregiverAndLogIn
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MissedDoseAlertFragmentTest {

    private val caregiverEmail = "missedalert-caregiver-test@meditrack.com"
    private val seniorWithContactEmail = "missedalert-senior-contact-test@meditrack.com"
    private val seniorNoContactEmail = "missedalert-senior-nocontact-test@meditrack.com"
    private val contactPhone = "3001234567"

    private var caregiverId: Long = -1

    @Before
    fun seedData(): Unit = runBlocking {
        caregiverId = seedCaregiverAndLogIn(caregiverEmail)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        val medicationDao = MediTrackDatabase.getInstance(context).medicationDao()
        val medicationLogDao = MediTrackDatabase.getInstance(context).medicationLogDao()
        val emergencyContactDao = MediTrackDatabase.getInstance(context).emergencyContactDao()

        userDao.findByEmail(seniorWithContactEmail)?.let { userDao.delete(it) }
        userDao.findByEmail(seniorNoContactEmail)?.let { userDao.delete(it) }

        val seniorWithContactId = userDao.insert(
            UserEntity(
                name = "Rosa Contact Test",
                email = seniorWithContactEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )
        emergencyContactDao.insert(
            EmergencyContactEntity(userId = seniorWithContactId, name = "Hijo de Rosa", phone = contactPhone)
        )
        val medicationWithContactId = medicationDao.insert(
            MedicationEntity(
                name = "Paracetamol Missed Test",
                dose = "500mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = seniorWithContactId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = medicationWithContactId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.MISSED
            )
        )

        val seniorNoContactId = userDao.insert(
            UserEntity(
                name = "Sin Contacto Test",
                email = seniorNoContactEmail,
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )
        val medicationNoContactId = medicationDao.insert(
            MedicationEntity(
                name = "Ibuprofeno Missed Test",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = seniorNoContactId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        medicationLogDao.insert(
            MedicationLogEntity(
                medicationId = medicationNoContactId,
                scheduledDatetime = System.currentTimeMillis(),
                confirmedAt = null,
                status = MedicationLogStatus.MISSED
            )
        )

        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.CALL_PHONE")
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        clearSessionAndDeleteUsers(caregiverEmail, seniorWithContactEmail, seniorNoContactEmail)
    }

    @Test
    fun alert_showsMedicationInfoAndEnablesCallButton_whenContactExists() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Paracetamol Missed Test")).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.missedDoseSeniorName)).check(matches(withText("Rosa Contact Test")))
            onView(withId(R.id.missedDoseMedicationName)).check(matches(withText("Paracetamol Missed Test")))
            onView(withId(R.id.callEmergencyContactButton)).check(matches(isEnabled()))
        }
    }

    @Test
    fun alert_disablesCallButtonAndShowsMessage_whenNoContactRegistered() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Ibuprofeno Missed Test")).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.callEmergencyContactButton)).check(matches(not(isEnabled())))
            onView(withId(R.id.missedDoseNoContactMessage)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun callButton_launchesActionCallWithRegisteredNumber() {
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_CALL)).respondWith(Instrumentation.ActivityResult(0, null))

            ActivityScenario.launch(MainActivity::class.java).use {
                onView(withText("Paracetamol Missed Test")).perform(click())
                Thread.sleep(500)

                onView(withId(R.id.callEmergencyContactButton)).perform(click())

                intended(hasAction(Intent.ACTION_CALL))
                intended(hasData("tel:$contactPhone"))
            }
        } finally {
            Intents.release()
        }
    }

    @Test
    fun dismissButton_navigatesBackWithoutCalling() {
        Intents.init()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                onView(withText("Paracetamol Missed Test")).perform(click())
                Thread.sleep(500)

                onView(withId(R.id.dismissButton)).perform(click())
                Thread.sleep(500)

                onView(withId(R.id.viewSeniorsButton)).check(matches(isDisplayed()))
                intended(hasAction(Intent.ACTION_CALL), times(0))
            }
        } finally {
            Intents.release()
        }
    }
}
