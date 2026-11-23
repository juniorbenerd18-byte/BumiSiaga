package com.example.siagabumi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.siagabumi.data.local.entity.FavoritEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritDao {

    @Query("SELECT * FROM favorit ORDER BY createdAt DESC")
    fun getAllFavorit(): Flow<List<FavoritEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorit WHERE adm4 = :adm4)")
    fun isFavorit(adm4: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorit WHERE adm4 = :adm4)")
    suspend fun isFavoritSync(adm4: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorit(favorit: FavoritEntity)

    @Query("DELETE FROM favorit WHERE adm4 = :adm4")
    suspend fun deleteFavorit(adm4: String)
}
