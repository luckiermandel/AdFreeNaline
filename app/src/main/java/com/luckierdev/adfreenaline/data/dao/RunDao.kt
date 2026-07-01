package com.luckierdev.adfreenaline.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luckierdev.adfreenaline.data.entities.RunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<RunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RunEntity>)

    @Query("DELETE FROM runs")
    suspend fun deleteAll()
}
