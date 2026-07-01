package com.luckierdev.adfreenaline

import org.osmdroid.util.GeoPoint

enum class RouteMode {
    STRAIGHT,
    FOLLOW_ROADS
}

data class SavedRoute(
    val id: Long,
    val name: String,
    val category: String,
    val colorHex: Int,
    val mode: RouteMode,
    val waypoints: List<GeoPoint>
)

data class CustomChallenge(
    val id: Long,
    val title: String,
    val pictureEmoji: String,
    val timeframeDays: Int,
    val targetRuns: Int,
    val targetDistanceKm: Double
)
