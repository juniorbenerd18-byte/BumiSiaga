package com.example.siagabumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorit")
data class FavoritEntity(
    @PrimaryKey val adm4: String,
    val desa: String,
    val kecamatan: String,
    val kotkab: String,
    val provinsi: String,
    val createdAt: Long = System.currentTimeMillis()
)
