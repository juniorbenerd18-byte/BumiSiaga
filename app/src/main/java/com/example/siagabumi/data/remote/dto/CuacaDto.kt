package com.example.siagabumi.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CuacaResponseDto(
    @SerialName("data") val data: List<CuacaDataDto> = emptyList(),
    @SerialName("lokasi") val lokasi: LokasiSummaryDto? = null
)

@Serializable
data class CuacaDataDto(
    @SerialName("lokasi") val lokasi: CuacaLokasiDto? = null,
    // data[0].cuaca is a list of lists (grouped by day)
    @SerialName("cuaca") val cuaca: List<List<CuacaItemDto>> = emptyList()
)

@Serializable
data class LokasiSummaryDto(
    @SerialName("adm1") val adm1: String? = null,
    @SerialName("adm2") val adm2: String? = null,
    @SerialName("adm3") val adm3: String? = null,
    @SerialName("adm4") val adm4: String? = null,
    @SerialName("provinsi") val provinsi: String? = null,
    @SerialName("kotkab") val kotkab: String? = null,
    @SerialName("kecamatan") val kecamatan: String? = null,
    @SerialName("desa") val desa: String? = null,
    @SerialName("timezone") val timezone: String? = null
)

@Serializable
data class CuacaLokasiDto(
    @SerialName("adm1") val adm1: String? = null,
    @SerialName("adm2") val adm2: String? = null,
    @SerialName("adm3") val adm3: String? = null,
    @SerialName("adm4") val adm4: String? = null,
    @SerialName("desa") val desa: String? = null,
    @SerialName("kecamatan") val kecamatan: String? = null,
    @SerialName("kotkab") val kotkab: String? = null,
    @SerialName("provinsi") val provinsi: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("type") val type: String? = null
)

@Serializable
data class CuacaItemDto(
    @SerialName("datetime") val datetime: String? = null,
    @SerialName("t") val t: Double? = null, // suhu °C (tolerant to float or int)
    @SerialName("tcc") val tcc: Double? = null, // tutupan awan %
    @SerialName("tp") val tp: Double? = null, // curah hujan mm
    @SerialName("weather") val weather: Int? = null,
    @SerialName("weather_desc") val weatherDesc: String? = null,
    @SerialName("weather_desc_en") val weatherDescEn: String? = null,
    @SerialName("wd_deg") val wdDeg: Double? = null,
    @SerialName("wd") val wd: String? = null,
    @SerialName("wd_to") val wdTo: String? = null,
    @SerialName("ws") val ws: Double? = null, // kecepatan angin km/jam
    @SerialName("hu") val hu: Double? = null, // kelembapan %
    @SerialName("time_index") val timeIndex: String? = null,
    @SerialName("analysis_date") val analysisDate: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("utc_datetime") val utcDatetime: String? = null,
    @SerialName("local_datetime") val localDatetime: String? = null
)
