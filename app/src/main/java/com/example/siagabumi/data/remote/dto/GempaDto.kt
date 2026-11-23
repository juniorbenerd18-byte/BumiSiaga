package com.example.siagabumi.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AutoGempaResponseDto(
    @SerialName("Infogempa") val infoGempa: AutoInfoGempaDto? = null
)

@Serializable
data class AutoInfoGempaDto(
    @SerialName("gempa") val gempa: GempaItemDto? = null
)

@Serializable
data class GempaListResponseDto(
    @SerialName("Infogempa") val infoGempa: GempaListInfoDto? = null
)

@Serializable
data class GempaListInfoDto(
    @SerialName("gempa") val gempa: List<GempaItemDto> = emptyList()
)

@Serializable
data class GempaItemDto(
    @SerialName("Tanggal") val tanggal: String? = null,
    @SerialName("Jam") val jam: String? = null,
    @SerialName("DateTime") val dateTime: String? = null,
    @SerialName("Coordinates") val coordinates: String? = null,
    @SerialName("Lintang") val lintang: String? = null,
    @SerialName("Bujur") val bujur: String? = null,
    @SerialName("Magnitude") val magnitude: String? = null,
    @SerialName("Kedalaman") val kedalaman: String? = null,
    @SerialName("Wilayah") val wilayah: String? = null,
    @SerialName("Potensi") val potensi: String? = null,
    @SerialName("Dirasakan") val dirasakan: String? = null,
    @SerialName("Shakemap") val shakemap: String? = null
)
