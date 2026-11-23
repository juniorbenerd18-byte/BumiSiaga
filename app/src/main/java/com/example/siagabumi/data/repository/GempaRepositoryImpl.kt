package com.example.siagabumi.data.repository

import com.example.siagabumi.data.local.dao.CacheMetaDao
import com.example.siagabumi.data.local.dao.GempaDao
import com.example.siagabumi.data.local.entity.CacheMetaEntity
import com.example.siagabumi.data.mapper.GempaMapper
import com.example.siagabumi.data.remote.GempaApi
import com.example.siagabumi.data.remote.dto.GempaItemDto
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import com.example.siagabumi.domain.repository.GempaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GempaRepositoryImpl @Inject constructor(
    private val gempaApi: GempaApi,
    private val gempaDao: GempaDao,
    private val cacheMetaDao: CacheMetaDao
) : GempaRepository {

    companion object {
        const val CACHE_TTL_MILLIS = 5 * 60 * 1000L // 5 minutes
        const val KEY_TERBARU = "gempa_terbaru"
        const val KEY_TERKINI = "gempa_terkini"
        const val KEY_DIRASAKAN = "gempa_dirasakan"
    }

    override fun getGempaTerbaru(): Flow<Gempa?> {
        return gempaDao.getGempaTerbaru(GempaCategory.TERBARU.name).map { entity ->
            entity?.let { GempaMapper.toDomain(it) }
        }
    }

    override fun getGempaTerkini(): Flow<List<Gempa>> {
        return gempaDao.getGempaByCategory(GempaCategory.TERKINI.name).map { list ->
            list.map { GempaMapper.toDomain(it) }
        }
    }

    override fun getGempaDirasakan(): Flow<List<Gempa>> {
        return gempaDao.getGempaByCategory(GempaCategory.DIRASAKAN.name).map { list ->
            list.map { GempaMapper.toDomain(it) }
        }
    }

    override fun getGempaById(id: String): Flow<Gempa?> {
        return gempaDao.getGempaById(id).map { entity ->
            entity?.let { GempaMapper.toDomain(it) }
        }
    }

    override fun getCacheLastUpdated(category: GempaCategory): Flow<Long?> {
        val key = when (category) {
            GempaCategory.TERBARU -> KEY_TERBARU
            GempaCategory.TERKINI -> KEY_TERKINI
            GempaCategory.DIRASAKAN -> KEY_DIRASAKAN
        }
        return cacheMetaDao.getLastUpdated(key)
    }

    override suspend fun refreshGempaTerbaru(force: Boolean): Result<Unit> {
        return try {
            val lastUpdated = cacheMetaDao.getLastUpdatedSync(KEY_TERBARU)
            if (!force && isCacheValid(lastUpdated)) {
                return Result.success(Unit)
            }
            val response = gempaApi.getAutoGempa()
            val dto = response.infoGempa?.gempa
            if (dto != null) {
                val domain = GempaMapper.toDomain(dto, GempaCategory.TERBARU)
                val entity = GempaMapper.toEntity(domain)
                gempaDao.replaceSingle(GempaCategory.TERBARU.name, entity)
                cacheMetaDao.setLastUpdated(CacheMetaEntity(KEY_TERBARU, System.currentTimeMillis()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshGempaTerkini(force: Boolean): Result<Unit> {
        return try {
            val lastUpdated = cacheMetaDao.getLastUpdatedSync(KEY_TERKINI)
            if (!force && isCacheValid(lastUpdated)) {
                return Result.success(Unit)
            }
            val response = gempaApi.getGempaTerkini()
            val dtoList: List<GempaItemDto> = response.infoGempa?.gempa.orEmpty()
            val entities = dtoList.map { item: GempaItemDto ->
                val domain = GempaMapper.toDomain(item, GempaCategory.TERKINI)
                GempaMapper.toEntity(domain)
            }
            gempaDao.replaceCategory(GempaCategory.TERKINI.name, entities)
            cacheMetaDao.setLastUpdated(CacheMetaEntity(KEY_TERKINI, System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshGempaDirasakan(force: Boolean): Result<Unit> {
        return try {
            val lastUpdated = cacheMetaDao.getLastUpdatedSync(KEY_DIRASAKAN)
            if (!force && isCacheValid(lastUpdated)) {
                return Result.success(Unit)
            }
            val response = gempaApi.getGempaDirasakan()
            val dtoList: List<GempaItemDto> = response.infoGempa?.gempa.orEmpty()
            val entities = dtoList.map { item: GempaItemDto ->
                val domain = GempaMapper.toDomain(item, GempaCategory.DIRASAKAN)
                GempaMapper.toEntity(domain)
            }
            gempaDao.replaceCategory(GempaCategory.DIRASAKAN.name, entities)
            cacheMetaDao.setLastUpdated(CacheMetaEntity(KEY_DIRASAKAN, System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshAll(force: Boolean): Result<Unit> {
        val r1 = refreshGempaTerbaru(force)
        val r2 = refreshGempaTerkini(force)
        val r3 = refreshGempaDirasakan(force)

        return if (r1.isSuccess || r2.isSuccess || r3.isSuccess) {
            Result.success(Unit)
        } else {
            r1.exceptionOrNull()?.let { Result.failure(it) }
                ?: r2.exceptionOrNull()?.let { Result.failure(it) }
                ?: r3.exceptionOrNull()?.let { Result.failure(it) }
                ?: Result.failure(Exception("Unknown network error"))
        }
    }

    private fun isCacheValid(lastUpdated: Long?): Boolean {
        if (lastUpdated == null) return false
        return (System.currentTimeMillis() - lastUpdated) < CACHE_TTL_MILLIS
    }
}
