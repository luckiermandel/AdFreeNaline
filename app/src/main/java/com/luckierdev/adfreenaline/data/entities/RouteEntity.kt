package com.luckierdev.adfreenaline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luckierdev.adfreenaline.RouteMode

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val category: String,
    val colorHex: Int,
    val mode: RouteMode
)
