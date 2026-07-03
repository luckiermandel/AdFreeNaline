package com.luckierdev.adfreenaline.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luckierdev.adfreenaline.Achievement
import com.luckierdev.adfreenaline.AchievementCalculator
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.StatsMetric
import com.luckierdev.adfreenaline.StatsWindow
import com.luckierdev.adfreenaline.bucketize
import com.luckierdev.adfreenaline.filterByWindow
import com.luckierdev.adfreenaline.isSameDay
import com.luckierdev.adfreenaline.ui.components.EmptyState
import com.luckierdev.adfreenaline.ui.format.formatDistance
import com.luckierdev.adfreenaline.ui.format.formatDuration
import com.luckierdev.adfreenaline.ui.format.formatSpeed
import com.luckierdev.adfreenaline.ui.format.kmToDisplay
import com.luckierdev.adfreenaline.ui.format.speedToDisplay
import com.luckierdev.adfreenaline.ui.format.speedUnitLabel
import com.luckierdev.adfreenaline.ui.theme.AdFreenalineTheme
import com.luckierdev.adfreenaline.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun StatsWindow.label(): String = stringResource(
    when (this) {
        StatsWindow.DAY -> R.string.window_day
        StatsWindow.WEEK -> R.string.window_week
        StatsWindow.MONTH -> R.string.window_month
        StatsWindow.YEAR -> R.string.window_year
        StatsWindow.ALL -> R.string.window_all
    }
)

@Composable
private fun StatsMetric.label(): String = stringResource(
    when (this) {
        StatsMetric.DISTANCE -> R.string.metric_distance
        StatsMetric.DURATION -> R.string.metric_duration
        StatsMetric.SPEED -> R.string.metric_speed
    }
)

@Composable
fun ColumnScope.StatsScreen(history: List<RunRecord>, unit: DistanceUnit) {
    var metric by rememberSaveable { mutableStateOf(StatsMetric.DISTANCE) }
    var window by rememberSaveable { mutableStateOf(StatsWindow.WEEK) }
    var windowMenuOpen by remember { mutableStateOf(false) }
    val filtered = history.filterByWindow(window)
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = stringResource(R.string.no_runs_title),
                hint = stringResource(R.string.no_runs_hint)
            )
        }
        StatsSummaryCards(history, unit)
        Box {
            Button(onClick = { windowMenuOpen = true }) {
                Text(window.label())
            }
            DropdownMenu(expanded = windowMenuOpen, onDismissRequest = { windowMenuOpen = false }) {
                StatsWindow.entries.forEach { w ->
                    DropdownMenuItem(
                        text = { Text(w.label()) },
                        onClick = {
                            window = w
                            windowMenuOpen = false
                        }
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatsMetric.entries.forEach { m ->
                Button(
                    onClick = { metric = m },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (metric == m) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (metric == m) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) { Text(m.label()) }
            }
        }
        MetricGraph(filtered, metric, window, unit)
        StreakCounter(history)
        AchievementSection(history)
        GroupedRunsCard(
            stringResource(R.string.group_today),
            history.filterByWindow(StatsWindow.DAY),
            unit
        )
        GroupedRunsCard(
            stringResource(R.string.group_last_week),
            history.filterByWindow(StatsWindow.WEEK)
                .filterNot { isSameDay(it.timestampMs, System.currentTimeMillis()) },
            unit
        )
        GroupedRunsCard(
            stringResource(R.string.group_older),
            history.filterNot { it in history.filterByWindow(StatsWindow.WEEK) },
            unit
        )
    }
}

@Composable
private fun StatsSummaryCards(history: List<RunRecord>, unit: DistanceUnit) {
    val cards = listOf(
        stringResource(R.string.summary_today) to history.filterByWindow(StatsWindow.DAY),
        stringResource(R.string.summary_month) to history.filterByWindow(StatsWindow.MONTH),
        stringResource(R.string.summary_year) to history.filterByWindow(StatsWindow.YEAR),
        stringResource(R.string.summary_decade) to history.filterByWindow(StatsWindow.ALL)
            .filter { System.currentTimeMillis() - it.timestampMs <= 10L * 365 * 24 * 60 * 60 * 1000 }
    )
    cards.forEach { (title, runs) ->
        val meters = runs.sumOf { it.distanceMeters }
        val duration = runs.sumOf { it.durationMs }
        val avgSpeedKmh = if (duration > 0) (meters / duration) * 3600.0 else 0.0
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title)
                Text(stringResource(R.string.runs_count, runs.size))
                Text(formatDistance(meters, unit, decimals = 1))
                Text(formatSpeed(avgSpeedKmh, unit, decimals = 1))
            }
        }
    }
}

@Composable
private fun MetricGraph(
    records: List<RunRecord>,
    metric: StatsMetric,
    window: StatsWindow,
    unit: DistanceUnit
) {
    val rawBuckets = bucketize(records, metric, window)
    val buckets = when (metric) {
        StatsMetric.DISTANCE -> rawBuckets.map { kmToDisplay(it, unit) }
        StatsMetric.SPEED -> rawBuckets.map { speedToDisplay(it, unit) }
        StatsMetric.DURATION -> rawBuckets
    }
    val max = (buckets.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val unitLabel = when (metric) {
        StatsMetric.DISTANCE -> stringResource(
            if (unit == DistanceUnit.KM) R.string.unit_km else R.string.unit_mi
        )
        StatsMetric.DURATION -> stringResource(R.string.unit_min)
        StatsMetric.SPEED -> speedUnitLabel(unit)
    }
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Text(stringResource(R.string.label_graph))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.GraphHeight)
            ) {
                val width = size.width
                val height = size.height
                val points = buckets.mapIndexed { index, value ->
                    val x = if (buckets.size <= 1) {
                        0f
                    } else {
                        (index.toFloat() / (buckets.size - 1).toFloat()) * width
                    }
                    val ratio = (value / max).toFloat().coerceIn(0f, 1f)
                    val y = height - (height * ratio)
                    Offset(x, y)
                }
                val steps = 5
                for (i in 0..steps) {
                    val y = height - (height * (i / steps.toFloat()))
                    val tickValue = (max / steps.toDouble()) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    val label = "${String.format(Locale.US, "%.1f", tickValue)} $unitLabel"
                    val measured = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(8f, (y - measured.size.height - 2f).coerceAtLeast(0f))
                    )
                }
                points.zipWithNext { a, b ->
                    drawLine(
                        color = lineColor,
                        start = a,
                        end = b,
                        strokeWidth = 3f
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCounter(history: List<RunRecord>) {
    val streak = AchievementCalculator.currentStreak(history)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_streak_fire),
                contentDescription = stringResource(R.string.cd_streak_fire),
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )
            Text(
                stringResource(
                    R.string.current_streak,
                    streak,
                    stringResource(if (streak == 1) R.string.day_singular else R.string.day_plural)
                )
            )
        }
    }
}

@Composable
private fun AchievementSection(history: List<RunRecord>) {
    val achievements = remember(history) { AchievementCalculator.build(history) }
    Text(stringResource(R.string.label_achievements), style = MaterialTheme.typography.titleMedium)
    achievements.forEach { ach ->
        AchievementCard(ach)
    }
}

@Composable
private fun AchievementCard(ach: Achievement) {
    var open by rememberSaveable(ach.id) { mutableStateOf(false) }
    val container = if (ach.unlocked) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (ach.unlocked) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = !open },
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Column(
            Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Icon(
                    imageVector = if (ach.unlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = stringResource(
                        if (ach.unlocked) R.string.cd_achievement_unlocked else R.string.cd_achievement_locked
                    ),
                    modifier = Modifier.height(18.dp)
                )
                Text(stringResource(ach.nameRes))
            }
            if (open) {
                Text(stringResource(ach.descriptionRes))
                Text(
                    stringResource(
                        R.string.achievement_progress,
                        ach.progressCurrent,
                        ach.progressTarget,
                        stringResource(ach.progressUnitRes)
                    )
                )
            }
        }
    }
}

@Composable
private fun GroupedRunsCard(title: String, runs: List<RunRecord>, unit: DistanceUnit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (runs.isEmpty()) {
                Text(
                    stringResource(R.string.no_runs_in_group),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            runs.take(20).forEach { run ->
                val d = SimpleDateFormat("MMM d HH:mm", Locale.US).format(Date(run.timestampMs))
                val runSpeedKmh = if (run.durationMs > 0) {
                    (run.distanceMeters / run.durationMs) * 3600.0
                } else {
                    0.0
                }
                Text(
                    "$d  •  ${formatDistance(run.distanceMeters, unit)}  •  " +
                        "${formatDuration(run.durationMs)}  •  ${formatSpeed(runSpeedKmh, unit, decimals = 1)}"
                )
            }
        }
    }
}

@Preview(name = "Stats empty", showBackground = true)
@Composable
private fun StatsScreenEmptyPreview() {
    AdFreenalineTheme {
        Column {
            StatsScreen(history = emptyList(), unit = DistanceUnit.KM)
        }
    }
}

@Preview(name = "Stats with data", showBackground = true)
@Composable
private fun StatsScreenDataPreview() {
    val now = System.currentTimeMillis()
    val runs = (0..6).map { i ->
        RunRecord(
            timestampMs = now - i * 86_400_000L,
            durationMs = 1_800_000L + i * 120_000L,
            distanceMeters = 4000.0 + i * 500,
            avgPaceMinKm = 6.0,
            avgSpeedKmh = 10.0,
            calories = 300 + i * 20,
            countryCode = "DE"
        )
    }
    AdFreenalineTheme {
        Column {
            StatsScreen(history = runs, unit = DistanceUnit.KM)
        }
    }
}
