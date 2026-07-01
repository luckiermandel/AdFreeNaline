package com.luckierdev.adfreenaline

enum class DistanceUnit {
    KM,
    MI
}

enum class BiologicalSex {
    MALE,
    FEMALE
}

data class RunSettings(
    val darkMode: Boolean = true,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val showSpeed: Boolean = true,
    val batterySaver: Boolean = false,
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
    val goalAlertSoundUri: String? = null
)
