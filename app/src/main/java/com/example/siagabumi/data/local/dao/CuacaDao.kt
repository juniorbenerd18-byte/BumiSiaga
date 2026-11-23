package com.example.siagabumi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.siagabumi.data.local.entity.PrakiraanEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CuacaDao {

    @Query("SELECT * FROM prakiraan_cuaca WHERE adm4 = :adm4 ORDER BY datetimeUtc ASC")
    abstract fun getPrakiraanByAdm4(adm4: String): Flow<List<PrakiraanEntity>>

    @Query("SELECT COUNT(*) FROM prakiraan_cuaca WHERE adm4 = :adm4")
    abstract suspend fun countByAdm4(adm4: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(list: List<PrakiraanEntity>)

    @Query("DELETE FROM prakiraan_cuaca WHERE adm4 = :adm4")
    abstract suspend fun deleteByAdm4(adm4: String)

    @Transaction
    open suspend fun replacePrakiraan(adm4: String, list: List<PrakiraanEntity>) {
        deleteByAdm4(adm4)
        insertAll(list)
    }
}
