package com.devoncats.meditrack.services

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")
        Thread.sleep(300)
    }

    @Test
    fun showMedicationAlarmNotification_createsChannelAndIncludesConfirmAndPostponeActions() {
        val logId = 9_001L
        NotificationHelper(context).showMedicationAlarmNotification(
            logId = logId,
            scheduleId = 1L,
            medicationId = 2L,
            medicationName = "Ibuprofeno",
            dose = "400mg"
        )

        Thread.sleep(500)

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
        assertNotNull("channel should be created on first use", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)

        val posted = notificationManager.activeNotifications.first { it.id == logId.toInt() }.notification
        assertEquals(2, posted.actions?.size)
        val actionTitles = posted.actions!!.map { it.title.toString() }
        assertEquals(
            listOf(
                context.getString(R.string.notification_action_confirm),
                context.getString(R.string.notification_action_postpone)
            ),
            actionTitles
        )

        notificationManager.cancel(logId.toInt())
    }

    @Test
    fun showMedicationAlarmNotification_forSeniorPatient_omitsPostponeAction() {
        val logId = 9_002L
        NotificationHelper(context).showMedicationAlarmNotification(
            logId = logId,
            scheduleId = 1L,
            medicationId = 2L,
            medicationName = "Ibuprofeno",
            dose = "400mg",
            isSeniorPatient = true
        )

        Thread.sleep(500)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val posted = notificationManager.activeNotifications.first { it.id == logId.toInt() }.notification
        assertEquals(1, posted.actions?.size)
        assertEquals(
            context.getString(R.string.notification_action_confirm),
            posted.actions!![0].title.toString()
        )

        notificationManager.cancel(logId.toInt())
    }
}
