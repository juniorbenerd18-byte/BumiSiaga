package com.example.siagabumi.di

import com.example.siagabumi.data.repository.CuacaRepositoryImpl
import com.example.siagabumi.data.repository.GempaRepositoryImpl
import com.example.siagabumi.data.repository.UserPreferencesRepositoryImpl
import com.example.siagabumi.data.repository.WilayahRepositoryImpl
import com.example.siagabumi.domain.repository.CuacaRepository
import com.example.siagabumi.domain.repository.GempaRepository
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import com.example.siagabumi.domain.repository.WilayahRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGempaRepository(
        impl: GempaRepositoryImpl
    ): GempaRepository

    @Binds
    @Singleton
    abstract fun bindCuacaRepository(
        impl: CuacaRepositoryImpl
    ): CuacaRepository

    @Binds
    @Singleton
    abstract fun bindWilayahRepository(
        impl: WilayahRepositoryImpl
    ): WilayahRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}
