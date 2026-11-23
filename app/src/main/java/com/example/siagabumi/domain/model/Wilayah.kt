package com.example.siagabumi.domain.model

data class Wilayah(
    val kode: String,
    val nama: String,
    val level: Int,
    val parentKode: String? = null,
    val namaLengkap: String = nama
)
