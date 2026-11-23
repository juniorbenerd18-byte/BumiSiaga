package com.example.siagabumi.data.remote

import com.example.siagabumi.data.remote.dto.AutoGempaResponseDto
import com.example.siagabumi.data.remote.dto.GempaListResponseDto
import retrofit2.http.GET

interface GempaApi {

    @GET("DataMKG/TEWS/autogempa.json")
    suspend fun getAutoGempa(): AutoGempaResponseDto

    @GET("DataMKG/TEWS/gempaterkini.json")
    suspend fun getGempaTerkini(): GempaListResponseDto

    @GET("DataMKG/TEWS/gempadirasakan.json")
    suspend fun getGempaDirasakan(): GempaListResponseDto
}
