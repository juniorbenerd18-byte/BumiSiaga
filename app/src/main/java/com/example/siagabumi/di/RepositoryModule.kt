package com.example.siagabumi.di

import com.example.siagabumi.data.repository.GempaRepositoryImpl
import com.example.siagabumi.domain.repository.GempaRepository
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
}
