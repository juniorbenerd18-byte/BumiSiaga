package com.example.siagabumi.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheTtlTest {

    private fun isCacheValid(lastUpdated: Long?, ttlMillis: Long): Boolean {
        if (lastUpdated == null) return false
        return (System.currentTimeMillis() - lastUpdated) < ttlMillis
    }

    @Test
    fun gempaTtl_fiveMinutes_evaluatesCorrectly() {
        val ttlMillis = 5 * 60 * 1000L
        val now = System.currentTimeMillis()

        // 1 minute ago -> valid
        assertTrue(isCacheValid(now - (1 * 60 * 1000L), ttlMillis))

        // 4 minutes 59 seconds ago -> valid
        assertTrue(isCacheValid(now - (4 * 60 * 1000L + 59 * 1000L), ttlMillis))

        // 5 minutes 1 second ago -> expired
        assertFalse(isCacheValid(now - (5 * 60 * 1000L + 1000L), ttlMillis))

        // 10 minutes ago -> expired
        assertFalse(isCacheValid(now - (10 * 60 * 1000L), ttlMillis))

        // null -> expired / invalid
        assertFalse(isCacheValid(null, ttlMillis))
    }

    @Test
    fun cuacaTtl_sixHours_evaluatesCorrectly() {
        val ttlMillis = 6 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        // 3 hours ago -> valid
        assertTrue(isCacheValid(now - (3 * 60 * 60 * 1000L), ttlMillis))

        // 6 hours 1 second ago -> expired
        assertFalse(isCacheValid(now - (6 * 60 * 60 * 1000L + 1000L), ttlMillis))
    }
}
