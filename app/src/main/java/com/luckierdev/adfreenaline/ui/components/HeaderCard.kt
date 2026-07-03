package com.luckierdev.adfreenaline.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.GoalAlertController
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.RunSettings
import com.luckierdev.adfreenaline.RunStats
import com.luckierdev.adfreenaline.ui.format.formatDistance
import com.luckierdev.adfreenaline.ui.format.formatDuration
import com.luckierdev.adfreenaline.ui.format.formatSpeed
import com.luckierdev.adfreenaline.ui.format.paceToDisplay
import com.luckierdev.adfreenaline.ui.format.paceUnitLabel
import com.luckierdev.adfreenaline.ui.theme.AdFreenalineTheme
import com.luckierdev.adfreenaline.ui.theme.Dimens
import java.util.Locale

@Composable
fun HeaderCard(stats: RunStats, settings: RunSettings, history: List<RunRecord>) {
    val pace = if (stats.avgPaceMinKm == 0.0) {
        "--"
    } else {
        String.format(Locale.US, "%.2f", paceToDisplay(stats.avgPaceMinKm, settings.distanceUnit))
    }
    val active = stats.isTracking && !stats.isPaused
    val headerColor = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val showGoals = settings.perRunDistanceGoalKm > 0 || settings.weeklyDistanceGoalKm > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = headerColor),
        shape = RoundedCornerShape(Dimens.CardCorner)
    ) {
        Column(Modifier.padding(Dimens.CardPadding), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.header_stats_line,
                    formatDistance(stats.distanceMeters, settings.distanceUnit),
                    formatDuration(stats.durationMs),
                    stringResource(R.string.calories_estimated_label),
                    stats.calories
                )
            )
            Text(
                stringResource(
                    R.string.header_pace_line,
                    pace,
                    paceUnitLabel(settings.distanceUnit),
                    formatSpeed(stats.avgSpeedKmh, settings.distanceUnit)
                )
            )
            if (showGoals) {
                Text(stringResource(R.string.label_distance_goals), style = MaterialTheme.typography.labelLarge)
                if (settings.perRunDistanceGoalKm > 0) {
                    GoalProgressRow(
                        label = stringResource(R.string.goal_label_run),
                        currentMeters = stats.distanceMeters,
                        goalKm = settings.perRunDistanceGoalKm,
                        unit = settings.distanceUnit,
                        compact = true
                    )
                }
                if (settings.weeklyDistanceGoalKm > 0) {
                    GoalProgressRow(
                        label = stringResource(R.string.goal_label_week),
                        currentMeters = GoalAlertController.calendarWeekDistanceMeters(history, stats.distanceMeters),
                        goalKm = settings.weeklyDistanceGoalKm,
                        unit = settings.distanceUnit,
                        compact = true
                    )
                }
            }
        }
    }
}

/** Condensed one-line header used outside the Track tab while a run is active. */
@Composable
fun CompactRunHeader(stats: RunStats, settings: RunSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(Dimens.CardCorner)
    ) {
        Column(Modifier.padding(horizontal = Dimens.CardPadding, vertical = Dimens.SpacingSm)) {
            Text(stringResource(R.string.header_active_run), style = MaterialTheme.typography.labelLarge)
            Text(
                "${formatDistance(stats.distanceMeters, settings.distanceUnit)} • ${formatDuration(stats.durationMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(name = "Header idle", showBackground = true)
@Composable
private fun HeaderCardPreview() {
    AdFreenalineTheme {
        HeaderCard(stats = RunStats(), settings = RunSettings(), history = emptyList())
    }
}

@Preview(name = "Header tracking with goals", showBackground = true)
@Composable
private fun HeaderCardTrackingPreview() {
    AdFreenalineTheme {
        HeaderCard(
            stats = RunStats(
                isTracking = true,
                durationMs = 1_500_000,
                distanceMeters = 4_200.0,
                avgPaceMinKm = 5.95,
                avgSpeedKmh = 10.1,
                calories = 320
            ),
            settings = RunSettings(perRunDistanceGoalKm = 5.0, weeklyDistanceGoalKm = 20.0),
            history = emptyList()
        )
    }
}