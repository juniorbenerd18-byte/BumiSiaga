package com.example.siagabumi.data.mapper

import com.example.siagabumi.data.local.entity.PrakiraanEntity
import com.example.siagabumi.data.remote.dto.CuacaItemDto
import com.example.siagabumi.data.remote.dto.CuacaResponseDto
import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.RingkasanHarianCuaca
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object CuacaMapper {

    private val localDateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    private val displayHourFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("HH:mm")
    }

    private val displayDateFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    }

    fun encodeImageUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url.replace(" ", "%20")
    }

    fun toPrakiraanList(
        adm4: String,
        responseDto: CuacaResponseDto
    ): List<PrakiraanEntity> {
        val dataList = responseDto.data
        if (dataList.isEmpty()) return emptyList()

        // data[0].cuaca is a list of lists -> flatten
        val flattened: List<CuacaItemDto> = dataList[0].cuaca.flatten()

        return flattened.mapNotNull { item ->
            val datetimeUtc = item.datetime?.trim().orEmpty()
            val localDt = item.localDatetime?.trim().orEmpty()
            if (datetimeUtc.isEmpty() && localDt.isEmpty()) return@mapNotNull null

            val tanggal = extractDate(localDt, datetimeUtc)
            val id = "${adm4}_${if (datetimeUtc.isNotEmpty()) datetimeUtc else localDt}"

            PrakiraanEntity(
                id = id,
                adm4 = adm4,
                datetimeUtc = datetimeUtc,
                localDatetime = localDt,
                suhu = item.t?.toInt() ?: 0,
                tutupanAwan = item.tcc?.toInt() ?: 0,
                curahHujan = item.tp ?: 0.0,
                weatherCode = item.weather ?: 0,
                weatherDesc = item.weatherDesc?.trim().orEmpty(),
                weatherDescEn = item.weatherDescEn?.trim().orEmpty(),
                arahAngin = item.wd?.trim().orEmpty(),
                kecepatanAngin = item.ws ?: 0.0,
                kelembapan = item.hu?.toInt() ?: 0,
                imageUrl = encodeImageUrl(item.image),
                tanggal = tanggal,
                cachedAt = System.currentTimeMillis()
            )
        }
    }

    fun toDomain(entity: PrakiraanEntity): PrakiraanCuaca {
        val hour = formatHour(entity.localDatetime, entity.datetimeUtc)
        return PrakiraanCuaca(
            id = entity.id,
            adm4 = entity.adm4,
            datetimeUtc = entity.datetimeUtc,
            localDatetime = entity.localDatetime,
            formattedHour = hour,
            suhu = entity.suhu,
            tutupanAwan = entity.tutupanAwan,
            curahHujan = entity.curahHujan,
            weatherCode = entity.weatherCode,
            weatherDesc = entity.weatherDesc,
            weatherDescEn = entity.weatherDescEn,
            arahAngin = entity.arahAngin,
            kecepatanAngin = entity.kecepatanAngin,
            kelembapan = entity.kelembapan,
            imageUrl = entity.imageUrl,
            tanggal = entity.tanggal
        )
    }

    fun groupDailyForecast(list: List<PrakiraanCuaca>): List<RingkasanHarianCuaca> {
        return list.groupBy { it.tanggal }.map { (tanggal, items) ->
            val suhuMin = items.minOfOrNull { it.suhu } ?: 0
            val suhuMax = items.maxOfOrNull { it.suhu } ?: 0
            val dominantDesc = items.groupBy { it.weatherDesc }
                .maxByOrNull { it.value.size }?.key ?: items.firstOrNull()?.weatherDesc.orEmpty()
            val dominantDescEn = items.groupBy { it.weatherDescEn }
                .maxByOrNull { it.value.size }?.key ?: items.firstOrNull()?.weatherDescEn.orEmpty()
            val dominantIcon = items.firstOrNull { it.weatherDesc == dominantDesc }?.imageUrl
                ?: items.firstOrNull()?.imageUrl.orEmpty()
            val totalRain = items.sumOf { it.curahHujan }

            val formattedDate = formatDailyDate(tanggal)

            RingkasanHarianCuaca(
                tanggal = tanggal,
                formattedDate = formattedDate,
                suhuMin = suhuMin,
                suhuMax = suhuMax,
                cuacaDominan = dominantDesc,
                cuacaDominanEn = dominantDescEn,
                iconDominanUrl = dominantIcon,
                totalCurahHujan = totalRain,
                daftarPrakiraan = items
            )
        }
    }

    fun findKondisiSaatIni(list: List<PrakiraanCuaca>): PrakiraanCuaca? {
        if (list.isEmpty()) return null
        val now = Instant.now()
        return list.minByOrNull { item ->
            try {
                val itemInstant = Instant.parse(item.datetimeUtc)
                kotlin.math.abs(itemInstant.toEpochMilli() - now.toEpochMilli())
            } catch (_: Exception) {
                Long.MAX_VALUE
            }
        } ?: list.firstOrNull()
    }

    fun extractDate(localDatetime: String, datetimeUtc: String): String {
        return if (localDatetime.length >= 10) {
            localDatetime.substring(0, 10)
        } else if (datetimeUtc.length >= 10) {
            datetimeUtc.substring(0, 10)
        } else {
            LocalDate.now().toString()
        }
    }

    fun formatHour(localDatetime: String, datetimeUtc: String): String {
        return try {
            if (localDatetime.isNotEmpty()) {
                val parsed = LocalDateTime.parse(localDatetime.trim(), localDateTimeFormatter)
                displayHourFormatter.format(parsed)
            } else {
                val instant = Instant.parse(datetimeUtc)
                DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()).format(instant)
            }
        } catch (_: Exception) {
            if (localDatetime.length >= 16) localDatetime.substring(11, 16) else ""
        }
    }

    fun formatDailyDate(tanggal: String): String {
        return try {
            val parsed = LocalDate.parse(tanggal)
            displayDateFormatter.format(parsed)
        } catch (_: Exception) {
            tanggal
        }
    }
}
