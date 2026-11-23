package com.example.siagabumi.data.repository

import com.example.siagabumi.data.local.dao.CacheMetaDao
import com.example.siagabumi.data.local.dao.CuacaDao
import com.example.siagabumi.data.local.dao.FavoritDao
import com.example.siagabumi.data.local.entity.CacheMetaEntity
import com.example.siagabumi.data.local.entity.FavoritEntity
import com.example.siagabumi.data.mapper.CuacaMapper
import com.example.siagabumi.data.remote.CuacaApi
import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.siagabumi.data.local.dao.WilayahDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CuacaRepositoryImpl @Inject constructor(
    private val cuacaApi: CuacaApi,
    private val cuacaDao: CuacaDao,
    private val favoritDao: FavoritDao,
    private val cacheMetaDao: CacheMetaDao,
    private val wilayahDao: WilayahDao
) : CuacaRepository {

    companion object {
        const val CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L // 6 hours
        private fun cacheKey(adm4: String) = "cuaca_$adm4"
    }

    override fun getPrakiraanCuaca(adm4: String): Flow<List<PrakiraanCuaca>> {
        return cuacaDao.getPrakiraanByAdm4(adm4).map { list ->
            list.map { CuacaMapper.toDomain(it) }
        }
    }

    override fun getCacheLastUpdated(adm4: String): Flow<Long?> {
        return cacheMetaDao.getLastUpdated(cacheKey(adm4))
    }

    override suspend fun refreshCuaca(adm4: String, force: Boolean): Result<Unit> {
        return try {
            val cleanAdm4 = if (adm4.isBlank() || adm4 == "{adm4}") {
                WilayahRepositoryImpl.DEFAULT_WILAYAH.kode
            } else {
                adm4
            }

            // If adm4 has fewer than 3 dots (e.g. 35.78 or 32), resolve to a level 4 child
            val resolvedAdm4 = if (cleanAdm4.count { it == '.' } < 3) {
                wilayahDao.findLevel4Wilayah(cleanAdm4)?.kode ?: cleanAdm4
            } else {
                cleanAdm4
            }

            val key = cacheKey(resolvedAdm4)
            val lastUpdated = cacheMetaDao.getLastUpdatedSync(key)
            val cachedCount = cuacaDao.countByAdm4(resolvedAdm4)
            if (!force && isCacheValid(lastUpdated) && cachedCount > 0) {
                return Result.success(Unit)
            }

            val response = cuacaApi.getPrakiraanCuaca(resolvedAdm4)
            val entities = CuacaMapper.toPrakiraanList(resolvedAdm4, response)
            if (entities.isNotEmpty()) {
                cuacaDao.replacePrakiraan(resolvedAdm4, entities)
                if (resolvedAdm4 != cleanAdm4) {
                    val cleanEntities = entities.map {
                        it.copy(
                            id = "${cleanAdm4}_${it.datetimeUtc.ifEmpty { it.localDatetime }}",
                            adm4 = cleanAdm4
                        )
                    }
                    cuacaDao.replacePrakiraan(cleanAdm4, cleanEntities)
                }
                cacheMetaDao.setLastUpdated(CacheMetaEntity(key, System.currentTimeMillis()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isFavorit(adm4: String): Flow<Boolean> {
        return favoritDao.isFavorit(adm4)
    }

    override suspend fun toggleFavorit(wilayah: Wilayah) {
        val isFav = favoritDao.isFavoritSync(wilayah.kode)
        if (isFav) {
            favoritDao.deleteFavorit(wilayah.kode)
        } else {
            val parts = wilayah.namaLengkap.split(",").map { it.trim() }
            val desa = parts.getOrNull(0) ?: wilayah.nama
            val kec = parts.getOrNull(1) ?: ""
            val kot = parts.getOrNull(2) ?: ""
            val prov = parts.getOrNull(3) ?: ""

            favoritDao.insertFavorit(
                FavoritEntity(
                    adm4 = wilayah.kode,
                    desa = desa,
                    kecamatan = kec,
                    kotkab = kot,
                    provinsi = prov
                )
            )
        }
    }

    override fun getAllFavorit(): Flow<List<Wilayah>> {
        return favoritDao.getAllFavorit().map { list ->
            list.map { fav ->
                val namaLengkap = listOf(fav.desa, fav.kecamatan, fav.kotkab, fav.provinsi)
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                Wilayah(
                    kode = fav.adm4,
                    nama = fav.desa,
                    level = 4,
                    parentKode = null,
                    namaLengkap = namaLengkap
                )
            }
        }
    }

    private fun isCacheValid(lastUpdated: Long?): Boolean {
        if (lastUpdated == null) return false
        return (System.currentTimeMillis() - lastUpdated) < CACHE_TTL_MILLIS
    }
}
