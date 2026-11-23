package com.example.siagabumi.domain.model

enum class GempaCategory {
    TERBARU,
    TERKINI,
    DIRASAKAN
}

data class Gempa(
    val id: String,
    val tanggal: String,
    val jam: String,
    val dateTimeUtc: String,
    val formattedLocalTime: String,
    val coordinates: String,
    val latitude: Double,
    val longitude: Double,
    val lintang: String,
    val bujur: String,
    val magnitude: Double,
    val kedalamanKm: Int,
    val kedalamanText: String,
    val wilayah: String,
    val potensi: String,
    val dirasakan: String?,
    val shakemapUrl: String?,
    val isTsunamiWarning: Boolean,
    val category: GempaCategory = GempaCategory.TERKINI
)
