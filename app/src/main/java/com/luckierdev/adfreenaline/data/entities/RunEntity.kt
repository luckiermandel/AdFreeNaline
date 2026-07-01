package com.luckierdev.adfreenaline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val timestampMs: Long,
    val durationMs: Long,
    val distanceMeters: Double,
    val avgPaceMinKm: Double,
    val avgSpeedKmh: Double,
    val calories: Int,
    val countryCode: String
)
