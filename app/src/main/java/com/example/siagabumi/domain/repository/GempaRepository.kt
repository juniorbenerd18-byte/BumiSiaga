package com.example.siagabumi.domain.repository

import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import kotlinx.coroutines.flow.Flow

interface GempaRepository {
    fun getGempaTerbaru(): Flow<Gempa?>
    fun getGempaTerkini(): Flow<List<Gempa>>
    fun getGempaDirasakan(): Flow<List<Gempa>>
    fun getGempaById(id: String): Flow<Gempa?>
    fun getCacheLastUpdated(category: GempaCategory): Flow<Long?>
    suspend fun refreshGempaTerbaru(force: Boolean = false): Result<Unit>
    suspend fun refreshGempaTerkini(force: Boolean = false): Result<Unit>
    suspend fun refreshGempaDirasakan(force: Boolean = false): Result<Unit>
    suspend fun refreshAll(force: Boolean = false): Result<Unit>
}
