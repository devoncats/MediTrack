package com.devoncats.meditrack.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.getOrAwaitValue
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationAlarmReceiverTest {

    private val testEmail = "alarm-receiver-test@meditrack.com"
    private var medicationId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // POST_NOTIFICATIONS is a runtime permission on API 33+ and does not survive the
        // fresh install that happens on every connectedAndroidTest run; grant it via
        // UiAutomation so this test is self-sufficient.
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")

        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(testEmail)?.let { userDao.delete(it) }
        val userId = userDao.insert(
            UserEntity(
                name = "Alarm Receiver Test User",
                email = testEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.PATIENT,
                caregiverId = null
            )
        )
        medicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Paracetamol",
                dose = "500mg",
                frequency = "Cada 8 horas",
                instructions = null,
                ownerUserId = userId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    @Test
    fun onReceive_createsPendingLogAndShowsNotificationWithNameAndDose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduleId = 5_001L

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
        }
        context.sendBroadcast(intent)

        Thread.sleep(1500)

        val logs = MediTrackDatabase.getInstance(context).medicationLogDao()
            .observeByMedication(medicationId)
            .getOrAwaitValue()
        val insertedLog = logs?.firstOrNull()

        assertNotNull("expected a MedicationLog to be created", insertedLog)
        assertEquals(MedicationLogStatus.PENDING, insertedLog!!.status)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val activeNotification = notificationManager.activeNotifications
            .firstOrNull { it.id == insertedLog.id.toInt() }
        assertNotNull("expected a notification to be posted", activeNotification)

        val extras = activeNotification!!.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        assertTrue("title should mention the medication name, was: $title", title.contains("Paracetamol"))
        assertTrue("text should mention the dose, was: $text", text.contains("500mg"))

        notificationManager.cancel(activeNotification.id)
    }

    @Test
    fun onReceive_forSeniorPatientOwnedMedication_notificationOmitsPostponeAction(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val seniorEmail = "alarm-receiver-senior-test@meditrack.com"
        val userDao = MediTrackDatabase.getInstance(context).userDao()
        userDao.findByEmail(seniorEmail)?.let { userDao.delete(it) }
        val seniorId = userDao.insert(
            UserEntity(
                name = "Alarm Receiver Senior Test",
                email = seniorEmail,
                passwordHash = PasswordHasher.hash("whatever123"),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = null
            )
        )
        val seniorMedicationId = MediTrackDatabase.getInstance(context).medicationDao().insert(
            MedicationEntity(
                name = "Ibuprofeno Senior",
                dose = "400mg",
                frequency = "Cada 12 horas",
                instructions = null,
                ownerUserId = seniorId,
                photoUri = null,
                createdAt = System.currentTimeMillis()
            )
        )
        val scheduleId = 5_002L

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_MEDICATION_ID, seniorMedicationId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
        }
        context.sendBroadcast(intent)

        Thread.sleep(1500)

        val logs = MediTrackDatabase.getInstance(context).medicationLogDao()
            .observeByMedication(seniorMedicationId)
            .getOrAwaitValue()
        val insertedLog = logs?.firstOrNull()
        assertNotNull("expected a MedicationLog to be created", insertedLog)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val activeNotification = notificationManager.activeNotifications
            .firstOrNull { it.id == insertedLog!!.id.toInt() }
        assertNotNull("expected a notification to be posted", activeNotification)

        assertEquals(1, activeNotification!!.notification.actions?.size)

        notificationManager.cancel(activeNotification.id)
    }
}
