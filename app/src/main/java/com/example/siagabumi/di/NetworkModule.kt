package com.example.siagabumi.di

import com.example.siagabumi.BuildConfig
import com.example.siagabumi.data.remote.CuacaApi
import com.example.siagabumi.data.remote.GempaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GempaRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CuacaRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GEMPA_BASE_URL = "https://data.bmkg.go.id/"
    private const val CUACA_BASE_URL = "https://api.bmkg.go.id/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @GempaRetrofit
    fun provideGempaRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(GEMPA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    @CuacaRetrofit
    fun provideCuacaRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(CUACA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideGempaApi(
        @GempaRetrofit retrofit: Retrofit
    ): GempaApi = retrofit.create(GempaApi::class.java)

    @Provides
    @Singleton
    fun provideCuacaApi(
        @CuacaRetrofit retrofit: Retrofit
    ): CuacaApi = retrofit.create(CuacaApi::class.java)
}
