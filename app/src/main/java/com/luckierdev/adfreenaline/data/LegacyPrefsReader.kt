package com.luckierdev.adfreenaline.data

import android.content.Context
import com.luckierdev.adfreenaline.BiologicalSex
import com.luckierdev.adfreenaline.CustomChallenge
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.RouteMode
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.RunSettings
import com.luckierdev.adfreenaline.SavedRoute
import org.osmdroid.util.GeoPoint

object LegacyPrefsReader {
    fun readRuns(context: Context): List<RunRecord> {
        val prefs = context.getSharedPreferences("run_history", Context.MODE_PRIVATE)
        val raw = prefs.getString("records", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 6) return@mapNotNull null
            runCatching {
                RunRecord(
                    timestampMs = p[0].toLong(),
                    durationMs = p[1].toLong(),
                    distanceMeters = p[2].toDouble(),
                    avgPaceMinKm = p[3].toDouble(),
                    avgSpeedKmh = p[4].toDouble(),
                    calories = p[5].toInt(),
                    countryCode = p.getOrNull(6) ?: "UNK"
                )
            }.getOrNull()
        }.sortedByDescending { it.timestampMs }.toList()
    }

    fun readSettings(context: Context): RunSettings {
        val prefs = context.getSharedPreferences("run_settings", Context.MODE_PRIVATE)
        if (!prefs.contains("darkMode") && !prefs.contains("onboardingComplete")) {
            return RunSettings()
        }
        return RunSettings(
            darkMode = prefs.getBoolean("darkMode", true),
            distanceUnit = DistanceUnit.valueOf(prefs.getString("distanceUnit", DistanceUnit.KM.name)!!),
            showSpeed = prefs.getBoolean("showSpeed", true),
            batterySaver = prefs.getBoolean("batterySaver", false),
            satelliteImageryEnabled = prefs.getBoolean("satelliteMapEnabled", true),
            creatorRouteColor = prefs.getInt("creatorRouteColor", 0xFF3F51B5.toInt()),
            weightKg = prefs.getFloat("weightKg", 70f).toDouble(),
            age = prefs.getInt("age", 30),
            heightCm = prefs.getInt("heightCm", 175),
            sex = BiologicalSex.valueOf(prefs.getString("sex", BiologicalSex.MALE.name)!!),
            heartRateAvg = prefs.getInt("heartRateAvg", -1).takeIf { it > 0 },
            effortLevel = prefs.getInt("effortLevel", -1).takeIf { it > 0 },
            calorieGoalPerRun = prefs.getInt("calorieGoalPerRun", 500),
            remindersEnabled = prefs.getBoolean("remindersEnabled", true),
            onboardingComplete = prefs.getBoolean("onboardingComplete", false),
            perRunDistanceGoalKm = prefs.getFloat("perRunDistanceGoalKm", 0f).toDouble(),
            weeklyDistanceGoalKm = prefs.getFloat("weeklyDistanceGoalKm", 0f).toDouble(),
            goalAlertMuted = prefs.getBoolean("goalAlertMuted", false),
            goalAlertSoundUri = prefs.getString("goalAlertSoundUri", null)?.takeIf { it.isNotBlank() }
        )
    }

    fun readRoutes(context: Context): List<SavedRoute> {
        val prefs = context.getSharedPreferences("saved_routes", Context.MODE_PRIVATE)
        val raw = prefs.getString("routes", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 6) return@mapNotNull null
            val pts = p[5].split(";").mapNotNull {
                val xy = it.split(",")
                if (xy.size != 2) null else runCatching { GeoPoint(xy[0].toDouble(), xy[1].toDouble()) }.getOrNull()
            }
            runCatching {
                SavedRoute(
                    id = p[0].toLong(),
                    name = p[1],
                    category = p[2],
                    colorHex = p[3].toInt(),
                    mode = RouteMode.valueOf(p[4]),
                    waypoints = pts
                )
            }.getOrNull()
        }.toList()
    }

    fun readChallenges(context: Context): List<CustomChallenge> {
        val prefs = context.getSharedPreferences("challenges", Context.MODE_PRIVATE)
        val raw = prefs.getString("items", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val p = line.split("|")
            if (p.size != 6) return@mapNotNull null
            runCatching {
                CustomChallenge(
                    id = p[0].toLong(),
                    title = p[1],
                    pictureEmoji = p[2],
                    timeframeDays = p[3].toInt(),
                    targetRuns = p[4].toInt(),
                    targetDistanceKm = p[5].toDouble()
                )
            }.getOrNull()
        }.toList()
    }

    fun readWeeklyGoalAlertKey(context: Context): String? {
        val prefs = context.getSharedPreferences("goal_alerts", Context.MODE_PRIVATE)
        return prefs.getString("lastWeeklyGoalAlertKey", null)
    }

    fun hasLegacyPrefs(context: Context): Boolean {
        val names = listOf("run_history", "run_settings", "saved_routes", "challenges", "goal_alerts")
        return names.any { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            prefs.all.isNotEmpty()
        }
    }

    fun clearLegacyPrefs(context: Context) {
        listOf("run_history", "run_settings", "saved_routes", "challenges", "goal_alerts").forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
