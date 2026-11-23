package com.example.siagabumi.domain.repository

import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.Wilayah
import kotlinx.coroutines.flow.Flow

interface CuacaRepository {
    fun getPrakiraanCuaca(adm4: String): Flow<List<PrakiraanCuaca>>
    fun getCacheLastUpdated(adm4: String): Flow<Long?>
    suspend fun refreshCuaca(adm4: String, force: Boolean = false): Result<Unit>
    fun isFavorit(adm4: String): Flow<Boolean>
    suspend fun toggleFavorit(wilayah: Wilayah)
    fun getAllFavorit(): Flow<List<Wilayah>>
}
