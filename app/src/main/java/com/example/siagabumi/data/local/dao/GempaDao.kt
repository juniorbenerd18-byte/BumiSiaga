package com.example.siagabumi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.siagabumi.data.local.entity.GempaEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GempaDao {

    @Query("SELECT * FROM gempa WHERE category = :category ORDER BY cachedAt DESC LIMIT 1")
    abstract fun getGempaTerbaru(category: String = "TERBARU"): Flow<GempaEntity?>

    @Query("SELECT * FROM gempa WHERE category = :category ORDER BY dateTimeUtc DESC")
    abstract fun getGempaByCategory(category: String): Flow<List<GempaEntity>>

    @Query("SELECT * FROM gempa WHERE id = :id LIMIT 1")
    abstract fun getGempaById(id: String): Flow<GempaEntity?>

    @Query("SELECT * FROM gempa WHERE id = :id LIMIT 1")
    abstract suspend fun getGempaByIdSync(id: String): GempaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: GempaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(list: List<GempaEntity>)

    @Query("DELETE FROM gempa WHERE category = :category")
    abstract suspend fun deleteByCategory(category: String)

    @Transaction
    open suspend fun replaceCategory(category: String, list: List<GempaEntity>) {
        deleteByCategory(category)
        insertAll(list)
    }

    @Transaction
    open suspend fun replaceSingle(category: String, entity: GempaEntity) {
        deleteByCategory(category)
        insert(entity)
    }
}
