package com.devoncats.meditrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MedicationLogStatusTest {

    @Test
    fun `aggregate returns null when there are no logs today`() {
        assertNull(MedicationLogStatus.aggregate(emptyList()))
    }

    @Test
    fun `aggregate returns MISSED when any log is missed`() {
        val statuses = listOf(MedicationLogStatus.CONFIRMED, MedicationLogStatus.MISSED, MedicationLogStatus.PENDING)

        assertEquals(MedicationLogStatus.MISSED, MedicationLogStatus.aggregate(statuses))
    }

    @Test
    fun `aggregate returns PENDING when no logs are missed but some are pending`() {
        val statuses = listOf(MedicationLogStatus.CONFIRMED, MedicationLogStatus.PENDING)

        assertEquals(MedicationLogStatus.PENDING, MedicationLogStatus.aggregate(statuses))
    }

    @Test
    fun `aggregate returns CONFIRMED when all logs are confirmed`() {
        val statuses = listOf(MedicationLogStatus.CONFIRMED, MedicationLogStatus.CONFIRMED)

        assertEquals(MedicationLogStatus.CONFIRMED, MedicationLogStatus.aggregate(statuses))
    }
}
