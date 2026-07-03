package com.devoncats.meditrack.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `verify returns true for the correct password`() {
        val hash = PasswordHasher.hash("Sup3rSecret!")

        assertTrue(PasswordHasher.verify("Sup3rSecret!", hash))
    }

    @Test
    fun `verify returns false for an incorrect password`() {
        val hash = PasswordHasher.hash("Sup3rSecret!")

        assertFalse(PasswordHasher.verify("wrong-password", hash))
    }

    @Test
    fun `hash produces a different value each time due to a random salt`() {
        val first = PasswordHasher.hash("Sup3rSecret!")
        val second = PasswordHasher.hash("Sup3rSecret!")

        assertNotEquals(first, second)
        assertTrue(PasswordHasher.verify("Sup3rSecret!", first))
        assertTrue(PasswordHasher.verify("Sup3rSecret!", second))
    }
}
