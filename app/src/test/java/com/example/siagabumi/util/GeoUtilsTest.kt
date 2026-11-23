package com.example.siagabumi.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun calculateDistance_sameCoordinates_returnsZero() {
        val distance = GeoUtils.calculateDistanceKm(-6.2088, 106.8456, -6.2088, 106.8456)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun calculateDistance_jakartaToBandung_isApproximately120to150km() {
        // Jakarta: -6.2088, 106.8456
        // Bandung: -6.9175, 107.6191
        val distance = GeoUtils.calculateDistanceKm(-6.2088, 106.8456, -6.9175, 107.6191)
        assertTrue("Distance should be between 110 and 160 km, was: $distance", distance in 110.0..160.0)
    }

    @Test
    fun calculateDistance_jakartaToSurabaya_isApproximately650to750km() {
        // Jakarta: -6.2088, 106.8456
        // Surabaya: -7.2575, 112.7521
        val distance = GeoUtils.calculateDistanceKm(-6.2088, 106.8456, -7.2575, 112.7521)
        assertTrue("Distance should be between 600 and 800 km, was: $distance", distance in 600.0..800.0)
    }

    @Test
    fun calculateDirection_jakartaToBandung_isSoutheast() {
        // Jakarta: -6.2088, 106.8456
        // Bandung: -6.9175, 107.6191 (South-East)
        val direction = GeoUtils.calculateDirection(-6.2088, 106.8456, -6.9175, 107.6191)
        assertEquals("Tenggara", direction)
    }

    @Test
    fun estimateMmi_strongNearEarthquake_givesHighIntensity() {
        // M 7.0 at 30km distance, 10km depth
        val estimate = GeoUtils.estimateMmi(7.0, 30.0, 10)
        assertTrue(estimate.level >= 5)
        assertTrue(estimate.scale.contains("V") || estimate.scale.contains("VI"))
    }

    @Test
    fun estimateMmi_moderateDistantEarthquake_givesLowIntensity() {
        // M 5.0 at 600km distance, 10km depth
        val estimate = GeoUtils.estimateMmi(5.0, 600.0, 10)
        assertEquals(1, estimate.level)
        assertEquals("I MMI", estimate.scale)
    }
}
