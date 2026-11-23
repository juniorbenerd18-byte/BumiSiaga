package com.example.siagabumi.domain.repository

import com.example.siagabumi.domain.model.Wilayah
import kotlinx.coroutines.flow.Flow

interface WilayahRepository {
    fun searchWilayah(query: String): Flow<List<Wilayah>>
    suspend fun getWilayahByKode(kode: String): Wilayah?
    suspend fun ensureWilayahDataSeeded(onProgress: ((Float) -> Unit)? = null)
    suspend fun getDefaultWilayah(): Wilayah
    suspend fun resolveToAdm4(kode: String): String
}
