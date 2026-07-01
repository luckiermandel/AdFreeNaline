package com.luckierdev.adfreenaline

data class RunRecord(
    val timestampMs: Long,
    val durationMs: Long,
    val distanceMeters: Double,
    val avgPaceMinKm: Double,
    val avgSpeedKmh: Double,
    val calories: Int,
    val countryCode: String = "UNK"
)
