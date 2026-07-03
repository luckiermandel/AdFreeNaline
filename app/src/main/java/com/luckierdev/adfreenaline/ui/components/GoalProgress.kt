package com.luckierdev.adfreenaline.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.GoalAlertController
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.RunSettings
import com.luckierdev.adfreenaline.RunStats
import com.luckierdev.adfreenaline.ui.theme.Dimens

@Composable
fun GoalProgressRow(
    label: String,
    currentMeters: Double,
    goalKm: Double,
    unit: DistanceUnit,
    compact: Boolean
) {
    val goalMeters = goalKm * 1000.0
    val progress = (currentMeters / goalMeters).toFloat().coerceIn(0f, 1f)
    val reached = currentMeters >= goalMeters
    val currentLabel = GoalAlertController.formatProgressDistance(currentMeters, unit)
    val goalLabel = GoalAlertController.formatGoalDistance(goalKm, unit)
    val accent = if (reached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else Dimens.SpacingXs)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$label: $currentLabel / $goalLabel",
                color = if (reached) accent else MaterialTheme.colorScheme.onSurface
            )
            if (reached) {
                Text(
                    stringResource(R.string.goal_reached),
                    color = accent,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 4.dp else 8.dp),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun GoalProgressCard(stats: RunStats, settings: RunSettings, history: List<RunRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(Dimens.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.label_distance_goals), style = MaterialTheme.typography.titleSmall)
            if (settings.perRunDistanceGoalKm > 0) {
                GoalProgressRow(
                    label = stringResource(R.string.goal_label_this_run),
                    currentMeters = stats.distanceMeters,
                    goalKm = settings.perRunDistanceGoalKm,
                    unit = settings.distanceUnit,
                    compact = false
                )
            }
            if (settings.weeklyDistanceGoalKm > 0) {
                GoalProgressRow(
                    label = stringResource(R.string.goal_label_this_week),
                    currentMeters = GoalAlertController.calendarWeekDistanceMeters(history, stats.distanceMeters),
                    goalKm = settings.weeklyDistanceGoalKm,
                    unit = settings.distanceUnit,
                    compact = false
                )
            }
        }
    }
}

@Composable
fun GoalPresetChip(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}
