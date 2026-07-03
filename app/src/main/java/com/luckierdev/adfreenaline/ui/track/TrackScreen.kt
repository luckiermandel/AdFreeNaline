package com.luckierdev.adfreenaline.ui.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.RunSettings
import com.luckierdev.adfreenaline.RunStats
import com.luckierdev.adfreenaline.ui.components.GoalProgressCard
import com.luckierdev.adfreenaline.ui.map.MapPanel
import com.luckierdev.adfreenaline.ui.theme.Dimens
import org.osmdroid.util.GeoPoint

@Composable
fun ColumnScope.TrackScreen(
    stats: RunStats,
    settings: RunSettings,
    runPath: List<GeoPoint>,
    creatorPath: List<GeoPoint>,
    currentLocation: GeoPoint?,
    currentBearing: Float,
    hasPermission: Boolean,
    permissionBlocked: Boolean,
    requestPermission: () -> Unit,
    history: List<RunRecord>,
    onStartRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onStopRun: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
        if (!stats.isTracking) {
            Button(
                onClick = { if (hasPermission) onStartRun() else requestPermission() },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.action_start)) }
        } else if (stats.isPaused) {
            Button(
                onClick = { if (hasPermission) onResumeRun() else requestPermission() },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.action_resume)) }
            Button(onClick = onStopRun, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_stop))
            }
        } else {
            Button(onClick = onPauseRun, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_pause))
            }
            Button(onClick = onStopRun, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_finish))
            }
        }
    }
    val goalsActive = stats.isTracking &&
        (settings.perRunDistanceGoalKm > 0 || settings.weeklyDistanceGoalKm > 0)
    if (goalsActive) {
        GoalProgressCard(stats, settings, history)
    }
    MapPanel(
        runPath = runPath,
        creatorPath = creatorPath,
        currentLocation = currentLocation,
        currentBearing = currentBearing,
        hasPermission = hasPermission,
        permissionBlocked = permissionBlocked,
        requestPermission = requestPermission,
        creatorMode = false,
        routeColor = settings.creatorRouteColor,
        darkMapStyleEnabled = settings.darkMapStyleEnabled,
        satelliteImageryEnabled = settings.satelliteImageryEnabled,
        modifier = Modifier.weight(1f),
        onTap = {}
    )
}
