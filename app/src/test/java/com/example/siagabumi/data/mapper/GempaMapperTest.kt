package com.example.siagabumi.data.mapper

import com.example.siagabumi.data.remote.dto.AutoGempaResponseDto
import com.example.siagabumi.data.remote.dto.GempaListResponseDto
import com.example.siagabumi.domain.model.GempaCategory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GempaMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun autogempa_jsonParsingAndMapping_isCorrect() {
        val sampleJson = """
            {"Infogempa":{"gempa":{"Tanggal":"22 Nov 2026","Jam":"14:54:39 WIB","DateTime":"2026-11-22T07:54:39+00:00","Coordinates":"3.12,126.57","Lintang":"3.12 LU","Bujur":"126.57 BT","Magnitude":"5.4","Kedalaman":"10 km","Wilayah":"Pusat gempa berada di laut 98 km BaratDaya Melonguane","Potensi":"Gempa ini dirasakan untuk diteruskan pada masyarakat","Dirasakan":"II Tahuna","Shakemap":"20261122145439.mmi.jpg"}}}
        """.trimIndent()

        val response = json.decodeFromString<AutoGempaResponseDto>(sampleJson)
        val dto = response.infoGempa?.gempa
        assertNotNull(dto)

        val domain = GempaMapper.toDomain(dto!!, GempaCategory.TERBARU)

        assertEquals("2026-11-22T07:54:39+00:00_3.12,126.57", domain.id)
        assertEquals(5.4, domain.magnitude, 0.001)
        assertEquals(10, domain.kedalamanKm)
        assertEquals(3.12, domain.latitude, 0.001)
        assertEquals(126.57, domain.longitude, 0.001)
        assertEquals("Pusat gempa berada di laut 98 km BaratDaya Melonguane", domain.wilayah)
        assertEquals("II Tahuna", domain.dirasakan)
        assertEquals("https://data.bmkg.go.id/DataMKG/TEWS/20261122145439.mmi.jpg", domain.shakemapUrl)
        assertFalse(domain.isTsunamiWarning)
        assertEquals(GempaCategory.TERBARU, domain.category)
    }

    @Test
    fun gempaterkini_withNegativeCoordinatesAndNoTsunami_isCorrect() {
        val sampleJson = """
            {"Infogempa":{"gempa":[{"Tanggal":"21 Nov 2026","Jam":"16:42:11 WIB","DateTime":"2026-11-21T09:42:11+00:00","Coordinates":"-7.88,127.51","Lintang":"7.88 LS","Bujur":"127.51 BT","Magnitude":"7.2","Kedalaman":"125 km","Wilayah":"43 km BaratLaut MALUKUBRTDAYA","Potensi":"Tidak berpotensi tsunami"}]}}
        """.trimIndent()

        val response = json.decodeFromString<GempaListResponseDto>(sampleJson)
        val dtoList = response.infoGempa?.gempa
        assertNotNull(dtoList)
        assertEquals(1, dtoList!!.size)

        val domain = GempaMapper.toDomain(dtoList[0], GempaCategory.TERKINI)

        assertEquals("2026-11-21T09:42:11+00:00_-7.88,127.51", domain.id)
        assertEquals(7.2, domain.magnitude, 0.001)
        assertEquals(125, domain.kedalamanKm)
        assertEquals(-7.88, domain.latitude, 0.001)
        assertEquals(127.51, domain.longitude, 0.001)
        assertFalse(domain.isTsunamiWarning)
        assertNull(domain.dirasakan)
        assertNull(domain.shakemapUrl)
    }

    @Test
    fun tsunamiWarningDetection_evaluatesCorrectly() {
        assertFalse(GempaMapper.isTsunamiWarning("Tidak berpotensi tsunami"))
        assertFalse(GempaMapper.isTsunamiWarning("tidak berpotensi tsunami"))
        assertFalse(GempaMapper.isTsunamiWarning("Gempa ini dirasakan untuk diteruskan pada masyarakat"))
        assertFalse(GempaMapper.isTsunamiWarning(null))
        assertFalse(GempaMapper.isTsunamiWarning(""))

        assertTrue(GempaMapper.isTsunamiWarning("Peringatan Dini Tsunami untuk wilayah Jatim"))
        assertTrue(GempaMapper.isTsunamiWarning("Berpotensi tsunami"))
        assertTrue(GempaMapper.isTsunamiWarning("Potensi tsunami"))
    }
}
