package com.example.siagabumi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.siagabumi.data.local.entity.CacheMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheMetaDao {

    @Query("SELECT lastUpdated FROM cache_meta WHERE `key` = :key LIMIT 1")
    fun getLastUpdated(key: String): Flow<Long?>

    @Query("SELECT lastUpdated FROM cache_meta WHERE `key` = :key LIMIT 1")
    suspend fun getLastUpdatedSync(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastUpdated(meta: CacheMetaEntity)
}
