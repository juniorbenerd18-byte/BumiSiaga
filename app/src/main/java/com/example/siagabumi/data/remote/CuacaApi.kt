package com.example.siagabumi.data.remote

import com.example.siagabumi.data.remote.dto.CuacaResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CuacaApi {

    @GET("publik/prakiraan-cuaca")
    suspend fun getPrakiraanCuaca(
        @Query("adm4") adm4: String
    ): CuacaResponseDto
}
