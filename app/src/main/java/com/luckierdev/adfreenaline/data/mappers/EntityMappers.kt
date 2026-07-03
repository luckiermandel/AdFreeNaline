package com.luckierdev.adfreenaline.data.mappers

import com.luckierdev.adfreenaline.CustomChallenge
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.RunSettings
import com.luckierdev.adfreenaline.SavedRoute
import com.luckierdev.adfreenaline.data.entities.ActiveSessionEntity
import com.luckierdev.adfreenaline.data.entities.AppSettingsEntity
import com.luckierdev.adfreenaline.data.entities.ChallengeEntity
import com.luckierdev.adfreenaline.data.entities.RouteEntity
import com.luckierdev.adfreenaline.data.entities.RouteWaypointEntity
import com.luckierdev.adfreenaline.data.entities.RunEntity
import org.osmdroid.util.GeoPoint

fun RunEntity.toRecord() = RunRecord(
    timestampMs = timestampMs,
    durationMs = durationMs,
    distanceMeters = distanceMeters,
    avgPaceMinKm = avgPaceMinKm,
    avgSpeedKmh = avgSpeedKmh,
    calories = calories,
    countryCode = countryCode
)

fun RunRecord.toEntity() = RunEntity(
    timestampMs = timestampMs,
    durationMs = durationMs,
    distanceMeters = distanceMeters,
    avgPaceMinKm = avgPaceMinKm,
    avgSpeedKmh = avgSpeedKmh,
    calories = calories,
    countryCode = countryCode
)

fun AppSettingsEntity.toRunSettings() = RunSettings(
    themeMode = themeMode,
    distanceUnit = distanceUnit,
    showSpeed = showSpeed,
    batterySaver = batterySaver,
    darkMapStyleEnabled = darkMapStyleEnabled,
    satelliteImageryEnabled = satelliteImageryEnabled,
    creatorRouteColor = creatorRouteColor,
    weightKg = weightKg,
    age = age,
    heightCm = heightCm,
    sex = sex,
    heartRateAvg = heartRateAvg,
    effortLevel = effortLevel,
    calorieGoalPerRun = calorieGoalPerRun,
    remindersEnabled = remindersEnabled,
    onboardingComplete = onboardingComplete,
    perRunDistanceGoalKm = perRunDistanceGoalKm,
    weeklyDistanceGoalKm = weeklyDistanceGoalKm,
    goalAlertMuted = goalAlertMuted,
    goalAlertSoundUri = goalAlertSoundUri
)

fun RunSettings.toEntity(lastWeeklyGoalAlertKey: String? = null) = AppSettingsEntity(
    id = 1,
    darkMode = themeMode == com.luckierdev.adfreenaline.ThemeMode.DARK,
    themeMode = themeMode,
    distanceUnit = distanceUnit,
    showSpeed = showSpeed,
    batterySaver = batterySaver,
    darkMapStyleEnabled = darkMapStyleEnabled,
    satelliteImageryEnabled = satelliteImageryEnabled,
    creatorRouteColor = creatorRouteColor,
    weightKg = weightKg,
    age = age,
    heightCm = heightCm,
    sex = sex,
    heartRateAvg = heartRateAvg,
    effortLevel = effortLevel,
    calorieGoalPerRun = calorieGoalPerRun,
    remindersEnabled = remindersEnabled,
    onboardingComplete = onboardingComplete,
    perRunDistanceGoalKm = perRunDistanceGoalKm,
    weeklyDistanceGoalKm = weeklyDistanceGoalKm,
    goalAlertMuted = goalAlertMuted,
    goalAlertSoundUri = goalAlertSoundUri,
    lastWeeklyGoalAlertKey = lastWeeklyGoalAlertKey
)

fun ChallengeEntity.toChallenge() = CustomChallenge(
    id = id,
    title = title,
    pictureEmoji = pictureEmoji,
    timeframeDays = timeframeDays,
    targetRuns = targetRuns,
    targetDistanceKm = targetDistanceKm
)

fun CustomChallenge.toEntity() = ChallengeEntity(
    id = id,
    title = title,
    pictureEmoji = pictureEmoji,
    timeframeDays = timeframeDays,
    targetRuns = targetRuns,
    targetDistanceKm = targetDistanceKm
)

fun RouteEntity.toSavedRoute(waypoints: List<RouteWaypointEntity>) = SavedRoute(
    id = id,
    name = name,
    category = category,
    colorHex = colorHex,
    mode = mode,
    waypoints = waypoints.sortedBy { it.sequence }.map { GeoPoint(it.latitude, it.longitude) }
)

fun SavedRoute.toEntities(): Pair<RouteEntity, List<RouteWaypointEntity>> {
    val route = RouteEntity(
        id = id,
        name = name,
        category = category,
        colorHex = colorHex,
        mode = mode
    )
    val waypoints = waypoints.mapIndexed { index, point ->
        RouteWaypointEntity(
            routeId = id,
            sequence = index,
            latitude = point.latitude,
            longitude = point.longitude
        )
    }
    return route to waypoints
}

fun serializePath(points: List<GeoPoint>): String =
    points.joinToString(";") { "${it.latitude},${it.longitude}" }

fun deserializePath(serialized: String): List<GeoPoint> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";").mapNotNull { part ->
        val xy = part.split(",")
        if (xy.size != 2) return@mapNotNull null
        runCatching { GeoPoint(xy[0].toDouble(), xy[1].toDouble()) }.getOrNull()
    }
}

data class ActiveRunSnapshot(
    val isTracking: Boolean,
    val isPaused: Boolean,
    val trackingStartMs: Long,
    val pausedAccumulatedMs: Long,
    val pauseStartMs: Long,
    val stats: com.luckierdev.adfreenaline.RunStats,
    val lastLocation: android.location.Location?,
    val path: List<GeoPoint>
)

fun ActiveSessionEntity.toSnapshot(): ActiveRunSnapshot? {
    if (!isTracking) return null
    val location = if (lastLat != null && lastLon != null) {
        android.location.Location("restored").apply {
            latitude = lastLat
            longitude = lastLon
            time = lastLocationTimeMs
        }
    } else {
        null
    }
    return ActiveRunSnapshot(
        isTracking = isTracking,
        isPaused = isPaused,
        trackingStartMs = trackingStartMs,
        pausedAccumulatedMs = pausedAccumulatedMs,
        pauseStartMs = pauseStartMs,
        stats = com.luckierdev.adfreenaline.RunStats(
            isTracking = isTracking,
            isPaused = isPaused,
            durationMs = durationMs,
            distanceMeters = distanceMeters,
            avgPaceMinKm = avgPaceMinKm,
            avgSpeedKmh = avgSpeedKmh,
            calories = calories
        ),
        lastLocation = location,
        path = deserializePath(pathSerialized)
    )
}

fun ActiveRunSnapshot.toEntity() = ActiveSessionEntity(
    id = 1,
    isTracking = isTracking,
    isPaused = isPaused,
    trackingStartMs = trackingStartMs,
    pausedAccumulatedMs = pausedAccumulatedMs,
    pauseStartMs = pauseStartMs,
    distanceMeters = stats.distanceMeters,
    durationMs = stats.durationMs,
    avgPaceMinKm = stats.avgPaceMinKm,
    avgSpeedKmh = stats.avgSpeedKmh,
    calories = stats.calories,
    lastLat = lastLocation?.latitude,
    lastLon = lastLocation?.longitude,
    lastLocationTimeMs = lastLocation?.time ?: 0L,
    pathSerialized = serializePath(path)
)
