package com.example.siagabumi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wilayah",
    indices = [
        Index(value = ["level"]),
        Index(value = ["nama"])
    ]
)
data class WilayahEntity(
    @PrimaryKey val kode: String,
    val nama: String,
    val level: Int,
    val parentKode: String? = null,
    val namaLengkap: String = nama
)
