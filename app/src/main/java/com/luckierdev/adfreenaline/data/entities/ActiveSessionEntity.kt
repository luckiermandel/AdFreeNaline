package com.luckierdev.adfreenaline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: Int = 1,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val trackingStartMs: Long = 0L,
    val pausedAccumulatedMs: Long = 0L,
    val pauseStartMs: Long = 0L,
    val distanceMeters: Double = 0.0,
    val durationMs: Long = 0L,
    val avgPaceMinKm: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val calories: Int = 0,
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val lastLocationTimeMs: Long = 0L,
    val pathSerialized: String = ""
)
