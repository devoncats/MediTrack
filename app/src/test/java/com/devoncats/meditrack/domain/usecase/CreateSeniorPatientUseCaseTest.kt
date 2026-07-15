package com.devoncats.meditrack.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateSeniorPatientUseCaseTest {

    @Test
    fun `buildUsername normalizes name and appends caregiverId`() {
        val username = CreateSeniorPatientUseCase.buildUsername("Maria Perez", 7L)

        assertEquals("pm_maria_perez_7", username)
    }

    @Test
    fun `buildUsername strips accents-free special characters and extra spaces`() {
        val username = CreateSeniorPatientUseCase.buildUsername("  Juan   Del Toro!! ", 3L)

        assertEquals("pm_juan_del_toro_3", username)
    }

    @Test
    fun `buildUsername is deterministic for the same inputs`() {
        val first = CreateSeniorPatientUseCase.buildUsername("Ana Ruiz", 1L)
        val second = CreateSeniorPatientUseCase.buildUsername("Ana Ruiz", 1L)

        assertEquals(first, second)
    }

    @Test
    fun `generatePin returns a six digit numeric string`() {
        repeat(50) {
            val pin = CreateSeniorPatientUseCase.generatePin()

            assertEquals(6, pin.length)
            assertTrue(pin.all { it.isDigit() })
        }
    }

    @Test
    fun `generatePin produces varying values across calls`() {
        val pins = (1..20).map { CreateSeniorPatientUseCase.generatePin() }.toSet()

        assertNotEquals(1, pins.size)
    }
}
