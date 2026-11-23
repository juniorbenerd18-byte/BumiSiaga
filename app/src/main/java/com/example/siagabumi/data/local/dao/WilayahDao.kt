package com.example.siagabumi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.siagabumi.data.local.entity.WilayahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WilayahDao {

    @Query("SELECT * FROM wilayah WHERE level = 4 AND (nama LIKE '%' || :query || '%' OR namaLengkap LIKE '%' || :query || '%') ORDER BY nama ASC LIMIT 50")
    fun searchWilayah(query: String): Flow<List<WilayahEntity>>

    @Query("SELECT * FROM wilayah WHERE kode = :kode LIMIT 1")
    suspend fun getWilayahByKode(kode: String): WilayahEntity?

    @Query("SELECT * FROM wilayah WHERE (kode = :kode OR kode LIKE :kode || '.%') AND level = 4 LIMIT 1")
    suspend fun findLevel4Wilayah(kode: String): WilayahEntity?

    @Query("SELECT COUNT(*) FROM wilayah")
    suspend fun countWilayah(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<WilayahEntity>)
}
