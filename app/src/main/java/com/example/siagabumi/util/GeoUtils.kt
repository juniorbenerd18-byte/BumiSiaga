package com.example.siagabumi.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

data class MmiEstimate(
    val scale: String,
    val descriptionId: String,
    val descriptionEn: String,
    val level: Int // 1 to 6
)

object GeoUtils {

    /**
     * Calculates great-circle distance between two geographic coordinates in kilometers
     * using the Haversine formula.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates cardinal direction from origin coordinate to target coordinate.
     */
    fun calculateDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0

        val directions = listOf("Utara", "Timur Laut", "Timur", "Tenggara", "Selatan", "Barat Daya", "Barat", "Barat Laut")
        val index = ((bearing + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    /**
     * Estimates perceived Modified Mercalli Intensity (MMI) at the user's location
     * based on earthquake magnitude, epicentral distance, and focal depth.
     */
    fun estimateMmi(magnitude: Double, distanceKm: Double, depthKm: Int): MmiEstimate {
        val hypoDistance = sqrt(distanceKm * distanceKm + depthKm * depthKm).coerceAtLeast(10.0)
        // Esteva / Gutenberg-Richter attenuation relationship
        val rawIntensity = 1.5 * magnitude - 3.5 * log10(hypoDistance) + 3.0

        return when {
            rawIntensity <= 1.5 -> MmiEstimate(
                scale = "I MMI",
                descriptionId = "Tidak dirasakan, hanya tercatat seismograf",
                descriptionEn = "Not felt, recorded only by seismographs",
                level = 1
            )
            rawIntensity <= 2.5 -> MmiEstimate(
                scale = "II MMI",
                descriptionId = "Guncangan lemah, dirasakan beberapa orang yang diam",
                descriptionEn = "Weak shaking, felt by few people at rest",
                level = 2
            )
            rawIntensity <= 3.5 -> MmiEstimate(
                scale = "III MMI",
                descriptionId = "Terasa nyata di dalam rumah, getaran seakan truk berlalu",
                descriptionEn = "Felt indoors like a passing heavy truck",
                level = 3
            )
            rawIntensity <= 4.5 -> MmiEstimate(
                scale = "IV MMI",
                descriptionId = "Dirasakan banyak orang, jendela & pintu berderik",
                descriptionEn = "Felt by many, windows and doors rattle",
                level = 4
            )
            rawIntensity <= 5.5 -> MmiEstimate(
                scale = "V MMI",
                descriptionId = "Guncangan kuat, barang berjatuhan, warga waspada",
                descriptionEn = "Strong shaking, small objects overturn, people alarmed",
                level = 5
            )
            else -> MmiEstimate(
                scale = "VI+ MMI",
                descriptionId = "Guncangan merusak, segera lindungi diri dan evakuasi",
                descriptionEn = "Destructive shaking, take cover and evacuate",
                level = 6
            )
        }
    }
}
