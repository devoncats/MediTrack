package com.devoncats.meditrack.presentation.caregiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateSeniorPatientViewModelTest {

    @Test
    fun `buildUsername normalizes name and appends caregiverId`() {
        val username = CreateSeniorPatientViewModel.buildUsername("Maria Perez", 7L)

        assertEquals("pm_maria_perez_7", username)
    }

    @Test
    fun `buildUsername strips accents-free special characters and extra spaces`() {
        val username = CreateSeniorPatientViewModel.buildUsername("  Juan   Del Toro!! ", 3L)

        assertEquals("pm_juan_del_toro_3", username)
    }

    @Test
    fun `buildUsername is deterministic for the same inputs`() {
        val first = CreateSeniorPatientViewModel.buildUsername("Ana Ruiz", 1L)
        val second = CreateSeniorPatientViewModel.buildUsername("Ana Ruiz", 1L)

        assertEquals(first, second)
    }

    @Test
    fun `generatePin returns a six digit numeric string`() {
        repeat(50) {
            val pin = CreateSeniorPatientViewModel.generatePin()

            assertEquals(6, pin.length)
            assertTrue(pin.all { it.isDigit() })
        }
    }

    @Test
    fun `generatePin produces varying values across calls`() {
        val pins = (1..20).map { CreateSeniorPatientViewModel.generatePin() }.toSet()

        assertNotEquals(1, pins.size)
    }
}
