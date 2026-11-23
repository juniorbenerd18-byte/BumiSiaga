package com.example.siagabumi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.siagabumi.data.local.dao.CacheMetaDao
import com.example.siagabumi.data.local.dao.CuacaDao
import com.example.siagabumi.data.local.dao.FavoritDao
import com.example.siagabumi.data.local.dao.GempaDao
import com.example.siagabumi.data.local.dao.WilayahDao
import com.example.siagabumi.data.local.entity.CacheMetaEntity
import com.example.siagabumi.data.local.entity.FavoritEntity
import com.example.siagabumi.data.local.entity.GempaEntity
import com.example.siagabumi.data.local.entity.PrakiraanEntity
import com.example.siagabumi.data.local.entity.WilayahEntity

@Database(
    entities = [
        GempaEntity::class,
        CacheMetaEntity::class,
        PrakiraanEntity::class,
        WilayahEntity::class,
        FavoritEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gempaDao(): GempaDao
    abstract fun cacheMetaDao(): CacheMetaDao
    abstract fun cuacaDao(): CuacaDao
    abstract fun wilayahDao(): WilayahDao
    abstract fun favoritDao(): FavoritDao
}
