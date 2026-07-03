package com.luckierdev.adfreenaline.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luckierdev.adfreenaline.BiologicalSex
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.ThemeMode

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    // Legacy column kept for schema compatibility; superseded by themeMode.
    val darkMode: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val showSpeed: Boolean = true,
    val batterySaver: Boolean = false,
    @ColumnInfo(name = "satelliteMapEnabled")
    val darkMapStyleEnabled: Boolean = false,
    val satelliteImageryEnabled: Boolean = true,
    val creatorRouteColor: Int = 0xFF3F51B5.toInt(),
    val weightKg: Double = 70.0,
    val age: Int = 30,
    val heightCm: Int = 175,
    val sex: BiologicalSex = BiologicalSex.MALE,
    val heartRateAvg: Int? = null,
    val effortLevel: Int? = null,
    val calorieGoalPerRun: Int = 500,
    val remindersEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val perRunDistanceGoalKm: Double = 0.0,
    val weeklyDistanceGoalKm: Double = 0.0,
    val goalAlertMuted: Boolean = false,
    val goalAlertSoundUri: String? = null,
    val lastWeeklyGoalAlertKey: String? = null
)
