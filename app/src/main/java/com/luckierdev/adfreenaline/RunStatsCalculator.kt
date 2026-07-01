package com.luckierdev.adfreenaline

import kotlin.math.roundToInt

const val MIN_MOVING_SPEED_KMH = 0.5

fun shouldAcceptGpsSegment(incrementalDistanceMeters: Double, deltaMs: Long): Boolean {
    if (deltaMs < 1L) return false
    val speedKmh = incrementalDistanceMeters * 3600.0 / deltaMs
    return speedKmh >= MIN_MOVING_SPEED_KMH
}

fun computeAvgPaceMinKm(distanceMeters: Double, elapsedMs: Long): Double {
    if (distanceMeters <= 1.0 || elapsedMs <= 0L) return 0.0
    return (elapsedMs / 60000.0) / (distanceMeters / 1000.0)
}

fun computeAvgSpeedKmh(distanceMeters: Double, elapsedMs: Long): Double {
    if (elapsedMs <= 0L) return 0.0
    return (distanceMeters / elapsedMs) * 3600.0
}

fun estimateCaloriesBurned(elapsedMs: Long, speedKmh: Double, settings: RunSettings): Int {
    val hours = elapsedMs / 3_600_000.0
    if (hours <= 0.0) return 0

    val baseMet = when {
        speedKmh < 8.0 -> 7.0
        speedKmh < 10.0 -> 9.8
        speedKmh < 12.0 -> 11.0
        else -> 12.8
    }
    val hrAdjust = settings.heartRateAvg?.let { (it / 150.0).coerceIn(0.8, 1.3) } ?: 1.0
    val effortFactor = settings.effortLevel?.let { 0.7 + (it * 0.06) } ?: 1.0
    val ageAdjust = (1.02 - ((settings.age - 30) * 0.002)).coerceIn(0.9, 1.1)
    val sexAdjust = if (settings.sex == BiologicalSex.FEMALE) 0.95 else 1.0

    val met = baseMet * effortFactor * hrAdjust * ageAdjust * sexAdjust
    return (met * settings.weightKg * hours).roundToInt()
}
