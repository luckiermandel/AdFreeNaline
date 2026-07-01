package com.luckierdev.adfreenaline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val metaKey: String,
    val value: String
)
