package com.example.siagabumi.di

import android.content.Context
import androidx.room.Room
import com.example.siagabumi.data.local.AppDatabase
import com.example.siagabumi.data.local.dao.CacheMetaDao
import com.example.siagabumi.data.local.dao.CuacaDao
import com.example.siagabumi.data.local.dao.FavoritDao
import com.example.siagabumi.data.local.dao.GempaDao
import com.example.siagabumi.data.local.dao.WilayahDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "siagabumi.db"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    fun provideGempaDao(db: AppDatabase): GempaDao = db.gempaDao()

    @Provides
    fun provideCacheMetaDao(db: AppDatabase): CacheMetaDao = db.cacheMetaDao()

    @Provides
    fun provideCuacaDao(db: AppDatabase): CuacaDao = db.cuacaDao()

    @Provides
    fun provideWilayahDao(db: AppDatabase): WilayahDao = db.wilayahDao()

    @Provides
    fun provideFavoritDao(db: AppDatabase): FavoritDao = db.favoritDao()
}
