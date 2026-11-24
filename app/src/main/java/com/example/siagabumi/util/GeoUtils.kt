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
     * If user coordinates are (0.0, 0.0), it falls back to Jakarta coordinates (-6.2088, 106.8456)
     * to avoid calculating from the Atlantic Ocean Null Island.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val safeLat1 = if (lat1 == 0.0 && lon1 == 0.0) -6.2088 else lat1
        val safeLon1 = if (lat1 == 0.0 && lon1 == 0.0) 106.8456 else lon1
        val safeLat2 = if (lat2 == 0.0 && lon2 == 0.0) -6.2088 else lat2
        val safeLon2 = if (lat2 == 0.0 && lon2 == 0.0) 106.8456 else lon2

        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(safeLat2 - safeLat1)
        val dLon = Math.toRadians(safeLon2 - safeLon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(safeLat1)) * cos(Math.toRadians(safeLat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates cardinal direction from origin coordinate to target coordinate.
     */
    fun calculateDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val safeLat1 = if (lat1 == 0.0 && lon1 == 0.0) -6.2088 else lat1
        val safeLon1 = if (lat1 == 0.0 && lon1 == 0.0) 106.8456 else lon1
        val safeLat2 = if (lat2 == 0.0 && lon2 == 0.0) -6.2088 else lat2
        val safeLon2 = if (lat2 == 0.0 && lon2 == 0.0) 106.8456 else lon2

        val dLon = Math.toRadians(safeLon2 - safeLon1)
        val y = sin(dLon) * cos(Math.toRadians(safeLat2))
        val x = cos(Math.toRadians(safeLat1)) * sin(Math.toRadians(safeLat2)) -
                sin(Math.toRadians(safeLat1)) * cos(Math.toRadians(safeLat2)) * cos(dLon)
        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0

        val directions = listOf("Utara", "Timur Laut", "Timur", "Tenggara", "Selatan", "Barat Daya", "Barat", "Barat Laut")
        val index = ((bearing + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    /**
     * Resolves approximate geographic coordinates (lat, lon) for Indonesian administrative codes
     * or city names, preventing (0.0, 0.0) when manual location is selected.
     */
    fun getApproximateCoordinates(admCode: String, locationName: String? = null): Pair<Double, Double> {
        val nameLower = locationName?.lowercase().orEmpty()

        // 1. Specific major cities matching
        when {
            nameLower.contains("surakarta") || nameLower.contains("solo") -> return Pair(-7.5666, 110.8167)
            nameLower.contains("sukasari") || nameLower.contains("bandung") -> return Pair(-6.9175, 107.6191)
            nameLower.contains("jakarta") || nameLower.contains("kemayoran") || nameLower.contains("kebayoran") -> return Pair(-6.2088, 106.8456)
            nameLower.contains("surabaya") -> return Pair(-7.2575, 112.7521)
            nameLower.contains("semarang") -> return Pair(-6.9667, 110.4167)
            nameLower.contains("yogyakarta") || nameLower.contains("jogja") -> return Pair(-7.7956, 110.3695)
            nameLower.contains("denpasar") || nameLower.contains("bali") -> return Pair(-8.6705, 115.2126)
            nameLower.contains("medan") -> return Pair(3.5952, 98.6722)
            nameLower.contains("makassar") -> return Pair(-5.1477, 119.4327)
            nameLower.contains("palembang") -> return Pair(-2.9761, 104.7754)
            nameLower.contains("balikpapan") -> return Pair(-1.2654, 116.8312)
            nameLower.contains("samarinda") -> return Pair(-0.5022, 117.1536)
            nameLower.contains("bogor") -> return Pair(-6.5971, 106.8060)
            nameLower.contains("depok") -> return Pair(-6.4025, 106.7942)
            nameLower.contains("bekasi") -> return Pair(-6.2383, 106.9756)
            nameLower.contains("tangerang") -> return Pair(-6.1783, 106.6319)
            nameLower.contains("malang") -> return Pair(-7.9797, 112.6304)
            nameLower.contains("padang") -> return Pair(-0.9471, 100.4172)
            nameLower.contains("pekanbaru") -> return Pair(0.5071, 101.4478)
            nameLower.contains("banda aceh") -> return Pair(5.5483, 95.3238)
            nameLower.contains("manado") -> return Pair(1.4748, 124.8428)
            nameLower.contains("jayapura") -> return Pair(-2.5916, 140.6690)
            nameLower.contains("ambon") -> return Pair(-3.6547, 128.1906)
            nameLower.contains("mataram") || nameLower.contains("lombok") -> return Pair(-8.5768, 116.0963)
            nameLower.contains("kupang") -> return Pair(-10.1772, 123.6070)
            nameLower.contains("pontianak") -> return Pair(-0.0263, 109.3425)
            nameLower.contains("banjarmasin") -> return Pair(-3.3167, 114.5900)
        }

        // 2. Province prefix matching (first 2 digits of ADM code)
        val provPrefix = if (admCode.length >= 2) admCode.take(2) else ""
        return when (provPrefix) {
            "11" -> Pair(5.5483, 95.3238) // Aceh
            "12" -> Pair(3.5952, 98.6722) // Sumut
            "13" -> Pair(-0.9471, 100.4172) // Sumbar
            "14" -> Pair(0.5071, 101.4478) // Riau
            "15" -> Pair(-1.6101, 103.6131) // Jambi
            "16" -> Pair(-2.9761, 104.7754) // Sumsel
            "17" -> Pair(-3.7928, 102.2608) // Bengkulu
            "18" -> Pair(-5.4500, 105.2667) // Lampung
            "19" -> Pair(-2.1333, 106.1167) // Bangka Belitung
            "21" -> Pair(1.1301, 104.0529) // Kepri
            "31" -> Pair(-6.2088, 106.8456) // DKI Jakarta
            "32" -> Pair(-6.9175, 107.6191) // Jawa Barat
            "33" -> Pair(-6.9667, 110.4167) // Jawa Tengah
            "34" -> Pair(-7.7956, 110.3695) // DI Yogyakarta
            "35" -> Pair(-7.2575, 112.7521) // Jawa Timur
            "36" -> Pair(-6.1200, 106.1500) // Banten
            "51" -> Pair(-8.6705, 115.2126) // Bali
            "52" -> Pair(-8.5768, 116.0963) // NTB
            "53" -> Pair(-10.1772, 123.6070) // NTT
            "61" -> Pair(-0.0263, 109.3425) // Kalbar
            "62" -> Pair(-2.2136, 113.9108) // Kalteng
            "63" -> Pair(-3.3167, 114.5900) // Kalsel
            "64" -> Pair(-0.5022, 117.1536) // Kaltim
            "65" -> Pair(2.8375, 117.3653) // Kaltara
            "71" -> Pair(1.4748, 124.8428) // Sulut
            "72" -> Pair(-0.8917, 119.8707) // Sulteng
            "73" -> Pair(-5.1477, 119.4327) // Sulsel
            "74" -> Pair(-3.9985, 122.5126) // Sultra
            "75" -> Pair(0.5435, 123.0568) // Gorontalo
            "76" -> Pair(-2.6734, 118.8872) // Sulbar
            "81" -> Pair(-3.6547, 128.1906) // Maluku
            "82" -> Pair(0.7903, 127.3804) // Maluku Utara
            "91" -> Pair(-0.8615, 134.0620) // Papua Barat
            "92" -> Pair(-0.8762, 131.2558) // Papua Barat Daya
            "93" -> Pair(-8.4932, 140.4018) // Papua Selatan
            "94" -> Pair(-3.3644, 135.4967) // Papua Tengah
            "95" -> Pair(-4.0950, 138.9442) // Papua Pegunungan
            "96" -> Pair(-2.5916, 140.6690) // Papua
            else -> Pair(-6.2088, 106.8456) // Default Jakarta / Pusat Nasional
        }
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
