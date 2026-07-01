package com.luckierdev.adfreenaline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val pictureEmoji: String,
    val timeframeDays: Int,
    val targetRuns: Int,
    val targetDistanceKm: Double
)
