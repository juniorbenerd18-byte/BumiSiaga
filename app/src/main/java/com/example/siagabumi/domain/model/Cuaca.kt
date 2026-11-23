package com.example.siagabumi.domain.model

data class PrakiraanCuaca(
    val id: String,
    val adm4: String,
    val datetimeUtc: String,
    val localDatetime: String,
    val formattedHour: String,
    val suhu: Int,
    val tutupanAwan: Int,
    val curahHujan: Double,
    val weatherCode: Int,
    val weatherDesc: String,
    val weatherDescEn: String,
    val arahAngin: String,
    val kecepatanAngin: Double,
    val kelembapan: Int,
    val imageUrl: String,
    val tanggal: String
)

data class RingkasanHarianCuaca(
    val tanggal: String,
    val formattedDate: String,
    val suhuMin: Int,
    val suhuMax: Int,
    val cuacaDominan: String,
    val iconDominanUrl: String,
    val totalCurahHujan: Double,
    val daftarPrakiraan: List<PrakiraanCuaca>
)

data class CuacaDetail(
    val lokasi: Wilayah,
    val kondisiSaatIni: PrakiraanCuaca?,
    val prakiraanHariIni3Jam: List<PrakiraanCuaca>,
    val ringkasanHarian: List<RingkasanHarianCuaca>,
    val isFavorit: Boolean = false,
    val lastUpdated: String? = null
)
