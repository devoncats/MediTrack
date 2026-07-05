package com.devoncats.meditrack.presentation.caregiver

import com.devoncats.meditrack.domain.model.MedicationLogStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeniorListViewModelTest {

    @Test
    fun `aggregateStatus returns null when there are no logs today`() {
        assertNull(SeniorListViewModel.aggregateStatus(emptyList()))
    }

    @Test
    fun `aggregateStatus returns MISSED when any log is missed`() {
        val statuses = listOf(MedicationLogStatus.CONFIRMED, MedicationLogStatus.MISSED, MedicationLogStatus.PENDING)

        assertEquals(MedicationLogStatus.MISSED, SeniorListViewModel.aggregateStatus(statuses))
    }

    @Test
    fun `aggregateStatus returns PENDING when no logs are missed but some are pending`() {
        val statuses = listOf(MedicationLogStatus.CONFIRMED, MedicationLogStatus.PENDING)

        assertEquals(MedicationLogStatus.PENDING, SeniorListViewModel.aggregateStatus(statuses))
    }

    @Test
    fun `aggregateStatus returns CONFIRMED when all logs are confirmed`() {
        val statuses = listOf(MedicationLogStatus.CONFIRMED, MedicationLogStatus.CONFIRMED)

        assertEquals(MedicationLogStatus.CONFIRMED, SeniorListViewModel.aggregateStatus(statuses))
    }
}
