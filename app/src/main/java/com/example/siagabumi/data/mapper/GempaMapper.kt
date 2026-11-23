package com.example.siagabumi.data.mapper

import com.example.siagabumi.data.local.entity.GempaEntity
import com.example.siagabumi.data.remote.dto.GempaItemDto
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object GempaMapper {

    private val localDateFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }

    fun toDomain(
        dto: GempaItemDto,
        category: GempaCategory = GempaCategory.TERKINI
    ): Gempa {
        val coords = dto.coordinates.orEmpty()
        val coordParts = coords.split(",")
        val lat = coordParts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
        val lon = coordParts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0

        val magnitude = dto.magnitude?.trim()?.toDoubleOrNull() ?: 0.0

        val kedalamanKm = dto.kedalaman?.let { extractKm(it) } ?: 0

        val dateTimeUtc = dto.dateTime.orEmpty().trim()
        val formattedTime = formatLocalTime(dateTimeUtc, dto.jam, dto.tanggal)

        val id = if (dateTimeUtc.isNotEmpty() || coords.isNotEmpty()) {
            "${dateTimeUtc}_$coords"
        } else {
            UUID.randomUUID().toString()
        }

        val isTsunami = isTsunamiWarning(dto.potensi)

        val shakemapUrl = dto.shakemap?.trim()?.takeIf { it.isNotEmpty() }?.let {
            if (it.startsWith("http://") || it.startsWith("https://")) {
                it
            } else {
                "https://static.bmkg.go.id/$it"
            }
        }

        return Gempa(
            id = id,
            tanggal = dto.tanggal.orEmpty().trim(),
            jam = dto.jam.orEmpty().trim(),
            dateTimeUtc = dateTimeUtc,
            formattedLocalTime = formattedTime,
            coordinates = coords,
            latitude = lat,
            longitude = lon,
            lintang = dto.lintang.orEmpty().trim(),
            bujur = dto.bujur.orEmpty().trim(),
            magnitude = magnitude,
            kedalamanKm = kedalamanKm,
            kedalamanText = dto.kedalaman.orEmpty().trim(),
            wilayah = dto.wilayah.orEmpty().trim(),
            potensi = dto.potensi.orEmpty().trim(),
            dirasakan = dto.dirasakan?.trim()?.takeIf { it.isNotEmpty() },
            shakemapUrl = shakemapUrl,
            isTsunamiWarning = isTsunami,
            category = category
        )
    }

    fun toEntity(domain: Gempa): GempaEntity {
        return GempaEntity(
            id = domain.id,
            tanggal = domain.tanggal,
            jam = domain.jam,
            dateTimeUtc = domain.dateTimeUtc,
            formattedLocalTime = domain.formattedLocalTime,
            coordinates = domain.coordinates,
            latitude = domain.latitude,
            longitude = domain.longitude,
            lintang = domain.lintang,
            bujur = domain.bujur,
            magnitude = domain.magnitude,
            kedalamanKm = domain.kedalamanKm,
            kedalamanText = domain.kedalamanText,
            wilayah = domain.wilayah,
            potensi = domain.potensi,
            dirasakan = domain.dirasakan,
            shakemapUrl = domain.shakemapUrl,
            isTsunamiWarning = domain.isTsunamiWarning,
            category = domain.category.name,
            cachedAt = System.currentTimeMillis()
        )
    }

    fun toDomain(entity: GempaEntity): Gempa {
        val category = try {
            GempaCategory.valueOf(entity.category)
        } catch (_: Exception) {
            GempaCategory.TERKINI
        }
        return Gempa(
            id = entity.id,
            tanggal = entity.tanggal,
            jam = entity.jam,
            dateTimeUtc = entity.dateTimeUtc,
            formattedLocalTime = entity.formattedLocalTime,
            coordinates = entity.coordinates,
            latitude = entity.latitude,
            longitude = entity.longitude,
            lintang = entity.lintang,
            bujur = entity.bujur,
            magnitude = entity.magnitude,
            kedalamanKm = entity.kedalamanKm,
            kedalamanText = entity.kedalamanText,
            wilayah = entity.wilayah,
            potensi = entity.potensi,
            dirasakan = entity.dirasakan,
            shakemapUrl = entity.shakemapUrl,
            isTsunamiWarning = entity.isTsunamiWarning,
            category = category
        )
    }

    fun extractKm(kedalamanText: String): Int {
        val match = Regex("""(\d+)""").find(kedalamanText)
        return match?.value?.toIntOrNull() ?: 0
    }

    fun isTsunamiWarning(potensi: String?): Boolean {
        if (potensi.isNullOrBlank()) return false
        val trimmed = potensi.trim()
        val containsTsunami = trimmed.contains("tsunami", ignoreCase = true)
        val startsWithTidak = trimmed.startsWith("tidak", ignoreCase = true)
        return containsTsunami && !startsWithTidak
    }

    fun formatLocalTime(dateTimeIso: String, jam: String?, tanggal: String?): String {
        if (dateTimeIso.isBlank()) {
            return listOfNotNull(tanggal?.trim(), jam?.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        }
        return try {
            val instant = Instant.parse(dateTimeIso)
            localDateFormatter.format(instant)
        } catch (_: Exception) {
            listOfNotNull(tanggal?.trim(), jam?.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        }
    }
}
