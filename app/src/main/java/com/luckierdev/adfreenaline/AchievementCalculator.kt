package com.luckierdev.adfreenaline

import java.util.Calendar
import java.util.Locale

enum class StatsMetric { DISTANCE, DURATION, SPEED }
enum class StatsWindow { DAY, WEEK, MONTH, YEAR, ALL }

/**
 * A computed achievement. [nameRes], [descriptionRes], and [progressUnitRes] are string
 * resource IDs so the calculator itself stays free of Android dependencies and unit-testable.
 */
data class Achievement(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val unlocked: Boolean,
    val progressCurrent: String,
    val progressTarget: String,
    val progressUnitRes: Int
)

object AchievementCalculator {

    fun build(history: List<RunRecord>): List<Achievement> {
        val eligibleRuns = history.filter { it.distanceMeters >= 500.0 }
        val totalKm = eligibleRuns.sumOf { it.distanceMeters } / 1000.0
        val bestKm = (eligibleRuns.maxOfOrNull { it.distanceMeters } ?: 0.0) / 1000.0
        val bestPace = eligibleRuns.filter { it.avgPaceMinKm > 0 }.minOfOrNull { it.avgPaceMinKm } ?: Double.MAX_VALUE
        val totalCalories = eligibleRuns.sumOf { it.calories }
        val sameHourRuns = eligibleRuns.groupBy { hourOfDay(it.timestampMs) }.values.maxOfOrNull { it.size } ?: 0
        val longRuns = eligibleRuns.count { it.durationMs >= 90L * 60_000L }
        val sprintRuns = eligibleRuns.count { it.avgSpeedKmh >= 12.0 }
        val deepNightRuns = eligibleRuns.count { hourOfDay(it.timestampMs) in 0..4 }
        val countriesRunIn = eligibleRuns.map { it.countryCode }.filter { it != "UNK" }.toSet().size
        val streakNow = currentStreak(eligibleRuns)
        val streakBest = bestStreak(eligibleRuns)
        val consistency30 = eligibleRuns.filterByWindow(StatsWindow.MONTH).size
        val lazyGapDays = maxGapDays(eligibleRuns)
        val monthKm = eligibleRuns.filterByWindow(StatsWindow.MONTH).sumOf { it.distanceMeters } / 1000.0
        val weekKm = eligibleRuns.filterByWindow(StatsWindow.WEEK).sumOf { it.distanceMeters } / 1000.0
        val lateRuns = eligibleRuns.count { hourOfDay(it.timestampMs) >= 22 }
        val earlyRuns = eligibleRuns.count { hourOfDay(it.timestampMs) < 6 }
        val ghostRuns = eligibleRuns.count { hourOfDay(it.timestampMs) == 3 }
        val weekRuns = eligibleRuns.filterByWindow(StatsWindow.WEEK).size
        val doubleDays = eligibleRuns.groupBy { dayKey(it.timestampMs) }.values.count { it.size >= 2 }
        val bestRunCalories = eligibleRuns.maxOfOrNull { it.calories } ?: 0
        val shortRuns = eligibleRuns.count { it.durationMs in 1 until 300_000 }

        return listOf(
            Achievement("first", R.string.ach_first_name, R.string.ach_first_desc, eligibleRuns.isNotEmpty(), "${eligibleRuns.size}", "1", R.string.ach_unit_runs),
            Achievement("5k", R.string.ach_5k_name, R.string.ach_5k_desc, bestKm >= 5.0, fmt(bestKm), "5.00", R.string.ach_unit_km),
            Achievement("10k", R.string.ach_10k_name, R.string.ach_10k_desc, bestKm >= 10.0, fmt(bestKm), "10.00", R.string.ach_unit_km),
            Achievement("speed", R.string.ach_speed_name, R.string.ach_speed_desc, bestPace <= 5.0, fmtPace(bestPace), "5.00", R.string.ach_unit_min_km),
            Achievement("distance", R.string.ach_distance_name, R.string.ach_distance_desc, totalKm >= 100.0, fmt(totalKm), "100.00", R.string.ach_unit_km),
            Achievement("streak", R.string.ach_streak_name, R.string.ach_streak_desc, streakNow >= 3, "$streakNow", "3", R.string.ach_unit_days),
            Achievement("night", R.string.ach_night_name, R.string.ach_night_desc, lateRuns >= 1, "$lateRuns", "1", R.string.ach_unit_late_runs),
            Achievement("early", R.string.ach_early_name, R.string.ach_early_desc, earlyRuns >= 1, "$earlyRuns", "1", R.string.ach_unit_early_runs),
            Achievement("marathon", R.string.ach_marathon_name, R.string.ach_marathon_desc, totalKm >= 42.2, fmt(totalKm), "42.20", R.string.ach_unit_km),
            Achievement("fifty", R.string.ach_fifty_name, R.string.ach_fifty_desc, totalKm >= 50.0, fmt(totalKm), "50.00", R.string.ach_unit_km),
            Achievement("calburn", R.string.ach_calburn_name, R.string.ach_calburn_desc, bestRunCalories >= 500, "$bestRunCalories", "500", R.string.ach_unit_kcal),
            Achievement("volcano", R.string.ach_volcano_name, R.string.ach_volcano_desc, totalCalories >= 2000, "$totalCalories", "2000", R.string.ach_unit_kcal),
            Achievement("clockwork", R.string.ach_clockwork_name, R.string.ach_clockwork_desc, sameHourRuns >= 3, "$sameHourRuns", "3", R.string.ach_unit_runs),
            Achievement("ultra", R.string.ach_ultra_name, R.string.ach_ultra_desc, longRuns >= 2, "$longRuns", "2", R.string.ach_unit_long_runs),
            Achievement("warp", R.string.ach_warp_name, R.string.ach_warp_desc, sprintRuns >= 5, "$sprintRuns", "5", R.string.ach_unit_fast_runs),
            Achievement("ghost", R.string.ach_ghost_name, R.string.ach_ghost_desc, ghostRuns >= 1, "$ghostRuns", "1", R.string.ach_unit_ghost_runs),
            Achievement("gremlin", R.string.ach_gremlin_name, R.string.ach_gremlin_desc, weekRuns >= 7, "$weekRuns", "7", R.string.ach_unit_runs),
            Achievement("orbit", R.string.ach_orbit_name, R.string.ach_orbit_desc, totalKm >= 250.0, fmt(totalKm), "250.00", R.string.ach_unit_km),
            Achievement("dragon", R.string.ach_dragon_name, R.string.ach_dragon_desc, bestRunCalories >= 1000, "$bestRunCalories", "1000", R.string.ach_unit_kcal),
            Achievement("owlstack", R.string.ach_owlstack_name, R.string.ach_owlstack_desc, lateRuns >= 4, "$lateRuns", "4", R.string.ach_unit_late_runs),
            Achievement("rain", R.string.ach_rain_name, R.string.ach_rain_desc, deepNightRuns >= 6, "$deepNightRuns", "6", R.string.ach_unit_night_runs),
            Achievement("boomerang", R.string.ach_boomerang_name, R.string.ach_boomerang_desc, doubleDays >= 3, "$doubleDays", "3", R.string.ach_unit_double_days),
            Achievement("chaos", R.string.ach_chaos_name, R.string.ach_chaos_desc, dayPartCount(eligibleRuns) == 4, "${dayPartCount(eligibleRuns)}", "4", R.string.ach_unit_dayparts),
            Achievement("collector", R.string.ach_collector_name, R.string.ach_collector_desc, totalKm >= 500.0, fmt(totalKm), "500.00", R.string.ach_unit_km),
            Achievement("atlas", R.string.ach_atlas_name, R.string.ach_atlas_desc, totalKm >= 1000.0, fmt(totalKm), "1000.00", R.string.ach_unit_km),
            Achievement("worldly", R.string.ach_worldly_name, R.string.ach_worldly_desc, countriesRunIn >= 2, "$countriesRunIn", "2", R.string.ach_unit_countries),
            Achievement("passport", R.string.ach_passport_name, R.string.ach_passport_desc, countriesRunIn >= 4, "$countriesRunIn", "4", R.string.ach_unit_countries),
            Achievement("daily", R.string.ach_daily_name, R.string.ach_daily_desc, streakNow >= 10, "$streakNow", "10", R.string.ach_unit_days),
            Achievement("iron", R.string.ach_iron_name, R.string.ach_iron_desc, streakBest >= 30, "$streakBest", "30", R.string.ach_unit_days),
            Achievement("steady", R.string.ach_steady_name, R.string.ach_steady_desc, consistency30 >= 20, "$consistency30", "20", R.string.ach_unit_runs),
            Achievement("metronome", R.string.ach_metronome_name, R.string.ach_metronome_desc, consistency30 >= 25, "$consistency30", "25", R.string.ach_unit_runs),
            Achievement("month100", R.string.ach_month100_name, R.string.ach_month100_desc, monthKm >= 100.0, fmt(monthKm), "100.00", R.string.ach_unit_km),
            Achievement("month150", R.string.ach_month150_name, R.string.ach_month150_desc, monthKm >= 150.0, fmt(monthKm), "150.00", R.string.ach_unit_km),
            Achievement("week30", R.string.ach_week30_name, R.string.ach_week30_desc, weekKm >= 30.0, fmt(weekKm), "30.00", R.string.ach_unit_km),
            Achievement("skipday", R.string.ach_skipday_name, R.string.ach_skipday_desc, lazyGapDays >= 2, "$lazyGapDays", "2", R.string.ach_unit_day_gap),
            Achievement("gaveup", R.string.ach_gaveup_name, R.string.ach_gaveup_desc, shortRuns >= 1, "$shortRuns", "1", R.string.ach_unit_short_runs)
        )
    }

    fun currentStreak(records: List<RunRecord>): Int {
        if (records.isEmpty()) return 0
        val days = records.map { dayKey(it.timestampMs) }.toSet()
        var streak = 0
        val c = Calendar.getInstance()
        while (true) {
            val key = dayKey(c.timeInMillis)
            if (!days.contains(key)) break
            streak++
            c.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun bestStreak(records: List<RunRecord>): Int {
        if (records.isEmpty()) return 0
        val days = records.map { dayKey(it.timestampMs) }.toSet().map {
            val p = it.split("-")
            p[0].toInt() to p[1].toInt()
        }.sortedWith(compareBy({ it.first }, { it.second }))
        var best = 1
        var running = 1
        for (i in 1 until days.size) {
            val prev = days[i - 1]
            val cur = days[i]
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, prev.first)
                set(Calendar.DAY_OF_YEAR, prev.second)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val expected = c.get(Calendar.YEAR) to c.get(Calendar.DAY_OF_YEAR)
            if (cur == expected) {
                running++
                best = maxOf(best, running)
            } else running = 1
        }
        return best
    }

    fun maxGapDays(history: List<RunRecord>): Int {
        val days = history.map { dayKey(it.timestampMs) }.toSet().map {
            val p = it.split("-")
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, p[0].toInt())
            c.set(Calendar.DAY_OF_YEAR, p[1].toInt())
            c.timeInMillis / 86_400_000L
        }.sorted()
        if (days.size < 2) return 0
        var maxGap = 0L
        for (i in 1 until days.size) maxGap = maxOf(maxGap, days[i] - days[i - 1])
        return maxGap.toInt()
    }

    fun dayPartCount(history: List<RunRecord>): Int = history.map {
        when (hourOfDay(it.timestampMs)) {
            in 5..10 -> "morning"
            in 11..16 -> "day"
            in 17..21 -> "evening"
            else -> "night"
        }
    }.toSet().size

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun fmtPace(v: Double): String = if (v == Double.MAX_VALUE) "--" else String.format(Locale.US, "%.2f", v)
}

fun List<RunRecord>.filterByWindow(window: StatsWindow, now: Long = System.currentTimeMillis()): List<RunRecord> {
    return when (window) {
        StatsWindow.DAY -> filter { isSameDay(it.timestampMs, now) }
        StatsWindow.WEEK -> filter { now - it.timestampMs <= 7L * 86_400_000L }
        StatsWindow.MONTH -> filter { now - it.timestampMs <= 30L * 86_400_000L }
        StatsWindow.YEAR -> filter { now - it.timestampMs <= 365L * 86_400_000L }
        StatsWindow.ALL -> this
    }
}

fun bucketize(records: List<RunRecord>, metric: StatsMetric, window: StatsWindow): List<Double> {
    return when (window) {
        StatsWindow.DAY -> (0..23).map { hour ->
            metricValue(records.filter { hourOfDay(it.timestampMs) == hour }, metric)
        }
        StatsWindow.WEEK -> (0..13).map { dayBack ->
            val start = System.currentTimeMillis() - dayBack * 86_400_000L
            metricValue(records.filter { isSameDay(it.timestampMs, start) }, metric)
        }.reversed()
        StatsWindow.MONTH -> (0..59).map { dayBack ->
            val start = System.currentTimeMillis() - dayBack * 86_400_000L
            metricValue(records.filter { isSameDay(it.timestampMs, start) }, metric)
        }.reversed()
        StatsWindow.YEAR -> (0..23).map { monthBack ->
            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthBack) }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val r = records.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timestampMs }
                c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
            }
            metricValue(r, metric)
        }.reversed()
        StatsWindow.ALL -> {
            val years = records.map {
                Calendar.getInstance().apply { timeInMillis = it.timestampMs }.get(Calendar.YEAR)
            }.distinct().sorted()
            if (years.isEmpty()) listOf(0.0) else years.map { y ->
                metricValue(
                    records.filter {
                        Calendar.getInstance().apply { timeInMillis = it.timestampMs }.get(Calendar.YEAR) == y
                    },
                    metric
                )
            }
        }
    }
}

fun metricValue(records: List<RunRecord>, metric: StatsMetric): Double {
    if (records.isEmpty()) return 0.0
    return when (metric) {
        StatsMetric.DISTANCE -> records.sumOf { it.distanceMeters } / 1000.0
        StatsMetric.DURATION -> records.sumOf { it.durationMs } / 60000.0
        StatsMetric.SPEED -> {
            val duration = records.sumOf { it.durationMs }
            if (duration == 0L) 0.0 else (records.sumOf { it.distanceMeters } / duration) * 3600.0
        }
    }
}

fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

fun hourOfDay(ts: Long): Int = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)

fun dayKey(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
}
