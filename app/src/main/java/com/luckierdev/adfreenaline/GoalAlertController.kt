package com.luckierdev.adfreenaline

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class GoalAlertController(
    context: Context,
    private val settingsRepository: RunSettingsRepository
) {
    private val appContext = context.applicationContext
    private val notifier = GoalAlertNotifier(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var perRunAlerted = false

    fun resetPerRunAlert() {
        perRunAlerted = false
    }

    fun clearAll() {
        perRunAlerted = false
        scope.launch {
            settingsRepository.setLastWeeklyGoalAlertKey(null)
        }
    }

    fun evaluate(stats: RunStats, settings: RunSettings, history: List<RunRecord>) {
        if (!stats.isTracking || stats.isPaused) return

        val perRunGoalMeters = settings.perRunDistanceGoalKm * 1000.0
        if (perRunGoalMeters > 0 && !perRunAlerted && stats.distanceMeters >= perRunGoalMeters) {
            perRunAlerted = true
            val label = formatGoalDistance(settings.perRunDistanceGoalKm, settings.distanceUnit)
            notifier.notifyGoalReached(
                title = appContext.getString(R.string.notif_goal_run_title),
                message = appContext.getString(R.string.notif_goal_run_body, label),
                muted = settings.goalAlertMuted,
                soundUri = settings.goalAlertSoundUri
            )
        }

        val weeklyGoalMeters = settings.weeklyDistanceGoalKm * 1000.0
        if (weeklyGoalMeters > 0) {
            val weekKey = currentWeekKey()
            scope.launch {
                val alreadyAlerted = settingsRepository.getLastWeeklyGoalAlertKey() == weekKey
                val weekDistanceMeters = calendarWeekDistanceMeters(history, stats.distanceMeters)
                if (!alreadyAlerted && weekDistanceMeters >= weeklyGoalMeters) {
                    settingsRepository.setLastWeeklyGoalAlertKey(weekKey)
                    val label = formatGoalDistance(settings.weeklyDistanceGoalKm, settings.distanceUnit)
                    notifier.notifyGoalReached(
                        title = appContext.getString(R.string.notif_goal_week_title),
                        message = appContext.getString(R.string.notif_goal_week_body, label),
                        muted = settings.goalAlertMuted,
                        soundUri = settings.goalAlertSoundUri
                    )
                }
            }
        }
    }

    fun previewSound(settings: RunSettings) {
        notifier.previewSound(settings.goalAlertMuted, settings.goalAlertSoundUri)
    }

    fun soundLabel(settings: RunSettings): String {
        return notifier.soundLabel(settings.goalAlertMuted, settings.goalAlertSoundUri)
    }

    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    companion object {
        fun formatGoalDistance(km: Double, unit: DistanceUnit): String {
            return if (unit == DistanceUnit.KM) {
                if (km < 0.01) String.format(Locale.US, "%.1f m", km * 1000.0)
                else String.format(Locale.US, "%.2f km", km)
            } else {
                val mi = km * 0.621371
                if (mi < 0.01) String.format(Locale.US, "%.1f m", km * 1000.0)
                else String.format(Locale.US, "%.2f mi", mi)
            }
        }

        fun formatProgressDistance(meters: Double, unit: DistanceUnit): String {
            val km = meters / 1000.0
            return if (unit == DistanceUnit.KM) {
                if (km < 0.01) String.format(Locale.US, "%.1f m", meters)
                else String.format(Locale.US, "%.2f km", km)
            } else {
                val mi = km * 0.621371
                if (mi < 0.01) String.format(Locale.US, "%.1f m", meters)
                else String.format(Locale.US, "%.2f mi", mi)
            }
        }

        fun calendarWeekDistanceMeters(history: List<RunRecord>, currentRunMeters: Double = 0.0): Double {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            val historyMeters = history.filter { run ->
                val runCal = Calendar.getInstance().apply { timeInMillis = run.timestampMs }
                runCal.get(Calendar.YEAR) == year &&
                    runCal.get(Calendar.WEEK_OF_YEAR) == week
            }.sumOf { it.distanceMeters }
            return historyMeters + currentRunMeters
        }
    }
}
