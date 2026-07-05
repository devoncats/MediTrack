package com.devoncats.meditrack.services

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmSchedulerNextTriggerTest {

    @Test
    fun `schedules for later today when the time has not passed yet`() {
        val now = LocalDateTime.of(2026, 7, 6, 7, 0) // Monday 07:00
        val result = AlarmScheduler.nextTriggerMillis("08:00", "MON", now)

        val expected = LocalDateTime.of(2026, 7, 6, 8, 0)
        assertEquals(expected.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }

    @Test
    fun `schedules for next matching day when today's time has already passed`() {
        val now = LocalDateTime.of(2026, 7, 6, 9, 0) // Monday 09:00, past 08:00
        val result = AlarmScheduler.nextTriggerMillis("08:00", "MON", now)

        val expected = LocalDateTime.of(2026, 7, 13, 8, 0) // next Monday
        assertEquals(expected.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }

    @Test
    fun `picks the nearest of multiple allowed days`() {
        val now = LocalDateTime.of(2026, 7, 6, 9, 0) // Monday 09:00
        val result = AlarmScheduler.nextTriggerMillis("08:00", "MON,WED,FRI", now)

        val expected = LocalDateTime.of(2026, 7, 8, 8, 0) // Wednesday
        assertEquals(expected.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }

    @Test
    fun `returns null for invalid time format`() {
        assertNull(AlarmScheduler.nextTriggerMillis("not-a-time", "MON", LocalDateTime.now()))
    }

    @Test
    fun `returns null when no valid days are provided`() {
        assertNull(AlarmScheduler.nextTriggerMillis("08:00", "", LocalDateTime.now()))
    }
}
