package com.luckierdev.adfreenaline.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RunViewModel
import com.luckierdev.adfreenaline.ui.components.CompactRunHeader
import com.luckierdev.adfreenaline.ui.components.HeaderCard
import com.luckierdev.adfreenaline.ui.creator.CreatorScreen
import com.luckierdev.adfreenaline.ui.onboarding.OnboardingDialog
import com.luckierdev.adfreenaline.ui.routes.RoutesScreen
import com.luckierdev.adfreenaline.ui.settings.SettingsScreen
import com.luckierdev.adfreenaline.ui.stats.StatsScreen
import com.luckierdev.adfreenaline.ui.theme.Dimens
import com.luckierdev.adfreenaline.ui.track.TrackScreen

enum class AppTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    TRACK(R.string.tab_track, Icons.AutoMirrored.Filled.DirectionsRun),
    CREATOR(R.string.tab_creator, Icons.Filled.Draw),
    STATS(R.string.tab_stats, Icons.Filled.BarChart),
    ROUTES(R.string.tab_routes, Icons.Filled.Map),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings)
}

data class RouteDraft(
    val id: Long,
    val name: String,
    val category: String
)

@Composable
fun AppRoot(
    vm: RunViewModel,
    hasLocationPermission: Boolean,
    locationPermissionBlocked: Boolean,
    requestPermission: () -> Unit,
    onPickGoalSound: () -> Unit,
    onPreviewGoalSound: () -> Unit,
    onExportRuns: () -> Unit,
    snackbarMessage: String?,
    onSnackbarShown: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val runPath by vm.runPath.collectAsStateWithLifecycle()
    val creatorPath by vm.creatorPath.collectAsStateWithLifecycle()
    val currentLocation by vm.currentLocation.collectAsStateWithLifecycle()
    val currentBearing by vm.currentBearing.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val routes by vm.savedRoutes.collectAsStateWithLifecycle()
    val settingsResetVersion by vm.settingsResetVersion.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(AppTab.TRACK) }
    var editingRoute by remember { mutableStateOf<RouteDraft?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onSnackbarShown()
        }
    }

    if (!settings.onboardingComplete) {
        OnboardingDialog(
            onSave = { sex, age, height -> vm.completeOnboarding(sex, age, height) },
            onSkip = vm::skipOnboarding
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(stringResource(entry.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab-content"
        ) { currentTab ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                if (currentTab == AppTab.TRACK) {
                    HeaderCard(stats, settings, history)
                } else if (stats.isTracking) {
                    CompactRunHeader(stats, settings)
                }
                when (currentTab) {
                    AppTab.TRACK -> TrackScreen(
                        stats = stats,
                        settings = settings,
                        runPath = runPath,
                        creatorPath = creatorPath,
                        currentLocation = currentLocation,
                        currentBearing = currentBearing,
                        hasPermission = hasLocationPermission,
                        permissionBlocked = locationPermissionBlocked,
                        requestPermission = requestPermission,
                        history = history,
                        onStartRun = vm::startRun,
                        onPauseRun = vm::pauseRun,
                        onResumeRun = vm::resumeRun,
                        onStopRun = vm::stopRun
                    )
                    AppTab.CREATOR -> CreatorScreen(
                        creatorPath = creatorPath,
                        runPath = runPath,
                        currentLocation = currentLocation,
                        currentBearing = currentBearing,
                        hasPermission = hasLocationPermission,
                        permissionBlocked = locationPermissionBlocked,
                        requestPermission = requestPermission,
                        routeColor = settings.creatorRouteColor,
                        darkMapStyleEnabled = settings.darkMapStyleEnabled,
                        satelliteImageryEnabled = settings.satelliteImageryEnabled,
                        editingRoute = editingRoute,
                        onAddPoint = vm::addCreatorPoint,
                        onSetPoint = vm::setCreatorPoint,
                        onDeletePoint = vm::deleteCreatorPoint,
                        onSaveRoute = { name, category, color, mode, existingId ->
                            vm.saveRoute(name, category, color, mode, existingId)
                        },
                        onClear = vm::clearCreator
                    )
                    AppTab.STATS -> StatsScreen(history, settings.distanceUnit)
                    AppTab.ROUTES -> RoutesScreen(
                        routes = routes,
                        unit = settings.distanceUnit,
                        onApply = { route ->
                            vm.loadRoute(route)
                            editingRoute = null
                            tab = AppTab.TRACK
                        },
                        onEdit = { route ->
                            vm.loadRoute(route)
                            editingRoute = RouteDraft(route.id, route.name, route.category)
                            tab = AppTab.CREATOR
                        },
                        onDelete = { vm.deleteRoute(it.id) },
                        onDeleteCategory = vm::deleteCategory
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        settingsResetVersion = settingsResetVersion,
                        history = history,
                        onUpdate = vm::updateSettings,
                        onDeleteAllAppData = vm::deleteAllAppData,
                        onExportRuns = onExportRuns,
                        onPickGoalSound = onPickGoalSound,
                        onPreviewGoalSound = onPreviewGoalSound
                    )
                }
            }
        }
    }
}
