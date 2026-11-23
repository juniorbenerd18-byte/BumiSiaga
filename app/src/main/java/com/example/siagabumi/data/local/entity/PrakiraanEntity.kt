package com.example.siagabumi.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prakiraan_cuaca",
    indices = [
        Index(value = ["adm4"]),
        Index(value = ["tanggal"])
    ]
)
data class PrakiraanEntity(
    @PrimaryKey val id: String,
    val adm4: String,
    val datetimeUtc: String,
    val localDatetime: String,
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
    val tanggal: String,
    val cachedAt: Long = System.currentTimeMillis()
)
