package com.luckierdev.adfreenaline.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luckierdev.adfreenaline.data.entities.ActiveSessionEntity

@Dao
interface ActiveSessionDao {
    @Query("SELECT * FROM active_session WHERE id = 1")
    suspend fun get(): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun deleteAll()
}
