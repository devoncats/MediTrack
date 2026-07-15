package com.devoncats.meditrack.services

import com.devoncats.meditrack.domain.model.WeekDays
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmSchedulerNextTriggerTest {

    @Test
    fun `schedules for later today when the time has not passed yet`() {
        val now = LocalDateTime.of(2026, 7, 6, 7, 0) // Monday 07:00
        val result = AlarmScheduler.nextTriggerMillis(LocalTime.of(8, 0), WeekDays(setOf(DayOfWeek.MONDAY)), now)

        val expected = LocalDateTime.of(2026, 7, 6, 8, 0)
        assertEquals(expected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }

    @Test
    fun `schedules for next matching day when today's time has already passed`() {
        val now = LocalDateTime.of(2026, 7, 6, 9, 0) // Monday 09:00, past 08:00
        val result = AlarmScheduler.nextTriggerMillis(LocalTime.of(8, 0), WeekDays(setOf(DayOfWeek.MONDAY)), now)

        val expected = LocalDateTime.of(2026, 7, 13, 8, 0) // next Monday
        assertEquals(expected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }

    @Test
    fun `picks the nearest of multiple allowed days`() {
        val now = LocalDateTime.of(2026, 7, 6, 9, 0) // Monday 09:00
        val result = AlarmScheduler.nextTriggerMillis(
            LocalTime.of(8, 0),
            WeekDays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            now
        )

        val expected = LocalDateTime.of(2026, 7, 8, 8, 0) // Wednesday
        assertEquals(expected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }

    @Test
    fun `returns null when no valid days are provided`() {
        assertNull(AlarmScheduler.nextTriggerMillis(LocalTime.of(8, 0), WeekDays(emptySet()), LocalDateTime.now()))
    }

    @Test
    fun `postponeTriggerMillis adds the given number of minutes to now`() {
        val now = LocalDateTime.of(2026, 7, 6, 8, 0)
        val result = AlarmScheduler.postponeTriggerMillis(now, 15)

        val expected = LocalDateTime.of(2026, 7, 6, 8, 15)
        assertEquals(expected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), result)
    }
}
