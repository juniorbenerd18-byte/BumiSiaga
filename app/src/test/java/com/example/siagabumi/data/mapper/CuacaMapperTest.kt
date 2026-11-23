package com.example.siagabumi.data.mapper

import com.example.siagabumi.data.remote.dto.CuacaResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuacaMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun cuacaJson_flatteningAndMapping_isCorrect() {
        val sampleJson = """
        {
          "data": [
            {
              "lokasi": {
                "adm1": "31",
                "adm2": "31.71",
                "adm3": "31.71.03",
                "adm4": "31.71.03.1001",
                "desa": "Kemayoran",
                "kecamatan": "Kemayoran",
                "kotkab": "Kota Adm. Jakarta Pusat",
                "lat": -6.1647214778,
                "lon": 106.8453837867,
                "provinsi": "DKI Jakarta",
                "timezone": "+0700",
                "type": "adm4"
              },
              "cuaca": [
                [
                  {
                    "datetime": "2026-11-23T02:00:00Z",
                    "t": 31,
                    "tcc": 96,
                    "tp": 0.0,
                    "weather": 3,
                    "weather_desc": "Berawan",
                    "weather_desc_en": "Mostly Cloudy",
                    "wd_deg": 40,
                    "wd": "N",
                    "wd_to": "S",
                    "ws": 1.9,
                    "hu": 70,
                    "vs": null,
                    "vs_text": null,
                    "time_index": "2-3",
                    "analysis_date": "2026-11-23T00:00:00",
                    "image": "https://api-apps.bmkg.go.id/storage/icon/cuaca/hujan ringan-am.svg",
                    "utc_datetime": "2026-11-23 02:00:00",
                    "local_datetime": "2026-11-23 09:00:00"
                  }
                ],
                [
                  {
                    "datetime": "2026-11-24T02:00:00Z",
                    "t": 28,
                    "tcc": 80,
                    "tp": 2.5,
                    "weather": 60,
                    "weather_desc": "Hujan Ringan",
                    "weather_desc_en": "Light Rain",
                    "wd_deg": 90,
                    "wd": "E",
                    "wd_to": "W",
                    "ws": 3.5,
                    "hu": 85,
                    "vs": null,
                    "vs_text": null,
                    "time_index": "2-3",
                    "analysis_date": "2026-11-24T00:00:00",
                    "image": "https://api-apps.bmkg.go.id/storage/icon/cuaca/hujan sedang-pm.svg",
                    "utc_datetime": "2026-11-24 02:00:00",
                    "local_datetime": "2026-11-24 09:00:00"
                  }
                ]
              ]
            }
          ],
          "lokasi": {
            "adm1": "31",
            "timezone": "Asia/Jakarta"
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<CuacaResponseDto>(sampleJson)
        val entities = CuacaMapper.toPrakiraanList("31.71.03.1001", response)

        // Flattening check: list of 2 daily lists -> 2 items total
        assertEquals(2, entities.size)

        val first = entities[0]
        assertEquals("31.71.03.1001_2026-11-23T02:00:00Z", first.id)
        assertEquals(31, first.suhu)
        assertEquals(70, first.kelembapan)
        assertEquals(1.9, first.kecepatanAngin, 0.001)
        assertEquals("N", first.arahAngin)
        assertEquals(96, first.tutupanAwan)
        assertEquals(0.0, first.curahHujan, 0.001)
        assertEquals("Berawan", first.weatherDesc)
        // Space encoding check: "hujan ringan-am.svg" -> "hujan%20ringan-am.svg"
        assertEquals("https://api-apps.bmkg.go.id/storage/icon/cuaca/hujan%20ringan-am.svg", first.imageUrl)
        assertEquals("2026-11-23", first.tanggal)

        val domainList = entities.map { CuacaMapper.toDomain(it) }
        val dailyGroups = CuacaMapper.groupDailyForecast(domainList)
        assertEquals(2, dailyGroups.size)
        assertEquals("2026-11-23", dailyGroups[0].tanggal)
        assertEquals("2026-11-24", dailyGroups[1].tanggal)
    }

    @Test
    fun encodeImageUrl_replacesSpacesWithPercent20() {
        val input = "https://api-apps.bmkg.go.id/storage/icon/cuaca/hujan lebat-am.svg"
        val expected = "https://api-apps.bmkg.go.id/storage/icon/cuaca/hujan%20lebat-am.svg"
        assertEquals(expected, CuacaMapper.encodeImageUrl(input))

        val alreadyClean = "https://api-apps.bmkg.go.id/storage/icon/cuaca/cerah.svg"
        assertEquals(alreadyClean, CuacaMapper.encodeImageUrl(alreadyClean))
    }
}
