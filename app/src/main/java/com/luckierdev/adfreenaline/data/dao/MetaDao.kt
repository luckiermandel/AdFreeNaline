package com.luckierdev.adfreenaline.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luckierdev.adfreenaline.data.entities.AppMetaEntity

@Dao
interface MetaDao {
    @Query("SELECT value FROM app_meta WHERE metaKey = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AppMetaEntity)
}
