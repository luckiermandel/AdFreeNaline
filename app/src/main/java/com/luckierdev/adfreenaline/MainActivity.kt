package com.luckierdev.adfreenaline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String) {
    TRACK("Track"),
    CREATOR("Creator"),
    STATS("Stats"),
    ROUTES("Routes"),
    SETTINGS("Settings")
}

private data class RouteDraft(
    val id: Long,
    val name: String,
    val category: String
)

private enum class StatsMetric { DISTANCE, DURATION, SPEED }
private enum class StatsWindow { DAY, WEEK, MONTH, YEAR, ALL }

private const val MIN_GOAL_KM = 0.0009 // 0.9 meters
private const val MAX_GOAL_KM = 10_000.0
private const val PREFS_PERMISSIONS = "permissions"
private const val KEY_LOCATION_REQUESTED = "location_requested"

class MainActivity : ComponentActivity() {
    private val viewModel: RunViewModel by viewModels()
    private var hasLocationPermission by mutableStateOf(false)
    private var locationPermissionBlocked by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    private val permissionPrefs by lazy { getSharedPreferences(PREFS_PERMISSIONS, MODE_PRIVATE) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        hasLocationPermission = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            locationPermissionBlocked = false
            viewModel.startLocationWatch()
        } else {
            locationPermissionBlocked = isLocationPermissionBlocked()
            val message = if (locationPermissionBlocked) {
                "Location denied. Enable it in app settings."
            } else {
                "Location permission required"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (!hasNotificationPermission) {
            Toast.makeText(this, "Notification permission enables live run updates.", Toast.LENGTH_SHORT).show()
        }
    }
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateSettings { it.copy(goalAlertSoundUri = uri?.toString()) }
        }
    }
    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val records = viewModel.history.value
        if (records.isEmpty()) {
            Toast.makeText(this, "No runs to export", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        runCatching {
            val csv = RunHistoryExporter.toCsv(records)
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open file")
            Toast.makeText(this, "Exported ${records.size} runs", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        requestNotificationPermissionIfNeeded()
        promptLocationPermissionIfNeeded()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dark = androidx.compose.material3.darkColorScheme(
                primary = Color(0xFFFF7043),
                secondary = Color.Black,
                background = Color(0xFF0B0E13),
                surface = Color(0xFF151A22),
                surfaceVariant = Color.Black,
                onSurface = Color(0xFFE8EEF8)
            )
            val light = androidx.compose.material3.lightColorScheme(
                primary = Color(0xFFFF6D00),
                secondary = Color(0xFFFFB74D),
                tertiary = Color(0xFFFF8A65)
            )
            MaterialTheme(colorScheme = if (settings.darkMode) dark else light) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        vm = viewModel,
                        hasLocationPermission = hasLocationPermission,
                        locationPermissionBlocked = locationPermissionBlocked,
                        requestPermission = ::requestLocation,
                        onPickGoalSound = ::launchGoalSoundPicker,
                        onPreviewGoalSound = viewModel::previewGoalSound,
                        onExportRuns = ::launchExportCsv
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncLocationPermissionState()
    }

    private fun syncLocationPermissionState() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = granted
        locationPermissionBlocked = !granted && isLocationPermissionBlocked()
        if (granted) viewModel.startLocationWatch()
    }

    private fun isLocationPermissionBlocked(): Boolean {
        return permissionPrefs.getBoolean(KEY_LOCATION_REQUESTED, false) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun promptLocationPermissionIfNeeded() {
        syncLocationPermissionState()
        if (hasLocationPermission || locationPermissionBlocked) return
        requestLocationPermissionDialog()
    }

    private fun requestLocation() {
        syncLocationPermissionState()
        if (hasLocationPermission) return
        if (locationPermissionBlocked) {
            openAppSettings()
            return
        }
        requestLocationPermissionDialog()
    }

    private fun requestLocationPermissionDialog() {
        permissionPrefs.edit().putBoolean(KEY_LOCATION_REQUESTED, true).apply()
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        hasNotificationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun launchGoalSoundPicker() {
        val current = viewModel.settings.value.goalAlertSoundUri
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            if (!current.isNullOrBlank()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current))
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun launchExportCsv() {
        if (viewModel.history.value.isEmpty()) {
            Toast.makeText(this, "No runs to export", Toast.LENGTH_SHORT).show()
            return
        }
        val filename = "adfreenaline_runs_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.csv"
        exportCsvLauncher.launch(filename)
    }
}

@Composable
private fun AppRoot(
    vm: RunViewModel,
    hasLocationPermission: Boolean,
    locationPermissionBlocked: Boolean,
    requestPermission: () -> Unit,
    onPickGoalSound: () -> Unit,
    onPreviewGoalSound: () -> Unit,
    onExportRuns: () -> Unit
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

    if (!settings.onboardingComplete) {
        OnboardingDialog(
            onSave = { sex, age, height -> vm.completeOnboarding(sex, age, height) },
            onSkip = vm::skipOnboarding
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeaderCard(stats, settings, history)
        TabRow(selectedTabIndex = tab.ordinal) {
            AppTab.entries.forEach { entry ->
                Tab(
                    selected = tab == entry,
                    onClick = { tab = entry },
                    text = { Text(entry.label) }
                )
            }
        }

        when (tab) {
            AppTab.TRACK -> TrackTab(stats, runPath, creatorPath, currentLocation, currentBearing, hasLocationPermission, locationPermissionBlocked, requestPermission, history, vm)
            AppTab.CREATOR -> CreatorTab(creatorPath, runPath, currentLocation, currentBearing, hasLocationPermission, locationPermissionBlocked, requestPermission, settings.creatorRouteColor, settings.darkMapStyleEnabled, settings.satelliteImageryEnabled, editingRoute, vm)
            AppTab.STATS -> StatsTab(history)
            AppTab.ROUTES -> SavedRoutesTab(
                routes = routes,
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
            AppTab.SETTINGS -> SettingsTab(
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

@Composable
private fun OnboardingDialog(
    onSave: (BiologicalSex, Int, Int) -> Unit,
    onSkip: () -> Unit
) {
    var sex by rememberSaveable { mutableStateOf(BiologicalSex.MALE) }
    var age by rememberSaveable { mutableStateOf("30") }
    var height by rememberSaveable { mutableStateOf("175") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {},
        title = { Text("Quick setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose your profile, or skip.")
                Text(
                    stringResource(R.string.onboarding_profile_explanation),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.calorie_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { sex = BiologicalSex.MALE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sex == BiologicalSex.MALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Male") }
                    Button(
                        onClick = { sex = BiologicalSex.FEMALE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sex == BiologicalSex.FEMALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Female") }
                }
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height cm") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = { onSave(sex, age.toIntOrNull() ?: 30, height.toIntOrNull() ?: 175) }) { Text("Save") } },
        dismissButton = { Button(onClick = onSkip) { Text("Skip") } }
    )
}

@Composable
private fun HeaderCard(stats: RunStats, settings: RunSettings, history: List<RunRecord>) {
    val pace = if (stats.avgPaceMinKm == 0.0) "--" else String.format(Locale.US, "%.2f", if (settings.distanceUnit == DistanceUnit.KM) stats.avgPaceMinKm else stats.avgPaceMinKm / 0.621371)
    val speedText = if (settings.distanceUnit == DistanceUnit.KM) "${String.format(Locale.US, "%.2f", stats.avgSpeedKmh)} km/h" else "${String.format(Locale.US, "%.2f", stats.avgSpeedKmh * 0.621371)} mph"
    val distanceText = if (settings.distanceUnit == DistanceUnit.KM) {
        "${String.format(Locale.US, "%.2f", stats.distanceMeters / 1000.0)} km"
    } else {
        "${String.format(Locale.US, "%.2f", (stats.distanceMeters / 1000.0) * 0.621371)} mi"
    }
    val headerColor = if (stats.isTracking && !stats.isPaused) Color(0xFF303030) else MaterialTheme.colorScheme.surfaceVariant
    val showGoals = settings.perRunDistanceGoalKm > 0 || settings.weeklyDistanceGoalKm > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = headerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text("Distance: $distanceText | Duration: ${formatDuration(stats.durationMs)} | ${stringResource(R.string.calories_estimated_label)}: ${stats.calories}")
            Text("Avg Pace: $pace ${if (settings.distanceUnit == DistanceUnit.KM) "min/km" else "min/mi"} | Avg Speed: $speedText")
            if (showGoals) {
                Text("Distance goals", style = MaterialTheme.typography.labelLarge)
                if (settings.perRunDistanceGoalKm > 0) {
                    GoalProgressRow(
                        label = "Run",
                        currentMeters = stats.distanceMeters,
                        goalKm = settings.perRunDistanceGoalKm,
                        unit = settings.distanceUnit,
                        compact = true
                    )
                }
                if (settings.weeklyDistanceGoalKm > 0) {
                    GoalProgressRow(
                        label = "Week",
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

@Composable
private fun ColumnScope.TrackTab(
    stats: RunStats,
    runPath: List<GeoPoint>,
    creatorPath: List<GeoPoint>,
    currentLocation: GeoPoint?,
    currentBearing: Float,
    hasPermission: Boolean,
    permissionBlocked: Boolean,
    requestPermission: () -> Unit,
    history: List<RunRecord>,
    vm: RunViewModel
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!stats.isTracking) Button(onClick = vm::startRun, modifier = Modifier.weight(1f)) { Text("Start") }
        else if (stats.isPaused) {
            Button(onClick = vm::resumeRun, modifier = Modifier.weight(1f)) { Text("Resume") }
            Button(onClick = vm::stopRun, modifier = Modifier.weight(1f)) { Text("Stop") }
        } else {
            Button(onClick = vm::pauseRun, modifier = Modifier.weight(1f)) { Text("Pause") }
            Button(onClick = vm::stopRun, modifier = Modifier.weight(1f)) { Text("Finish") }
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
        routeColor = 0xFF1E88E5.toInt(),
        darkMapStyleEnabled = settings.darkMapStyleEnabled,
        satelliteImageryEnabled = settings.satelliteImageryEnabled,
        modifier = Modifier.weight(1f),
        onTap = {}
    )
}

@Composable
private fun ColumnScope.CreatorTab(
    creatorPath: List<GeoPoint>,
    runPath: List<GeoPoint>,
    currentLocation: GeoPoint?,
    currentBearing: Float,
    hasPermission: Boolean,
    permissionBlocked: Boolean,
    requestPermission: () -> Unit,
    routeColor: Int,
    darkMapStyleEnabled: Boolean,
    satelliteImageryEnabled: Boolean,
    editingRoute: RouteDraft?,
    vm: RunViewModel
) {
    var selectedWaypoint by rememberSaveable { mutableStateOf(-1) }
    var routeName by rememberSaveable(editingRoute?.id) { mutableStateOf(editingRoute?.name ?: "") }
    var routeCategory by rememberSaveable(editingRoute?.id) { mutableStateOf(editingRoute?.category ?: "General") }
    val creatorScroll = rememberScrollState()
    MapPanel(
        runPath = runPath,
        creatorPath = creatorPath,
        currentLocation = currentLocation,
        currentBearing = currentBearing,
        hasPermission = hasPermission,
        permissionBlocked = permissionBlocked,
        requestPermission = requestPermission,
        creatorMode = true,
        routeColor = routeColor,
        darkMapStyleEnabled = darkMapStyleEnabled,
        satelliteImageryEnabled = satelliteImageryEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        onTap = { p ->
            if (selectedWaypoint >= 0) {
                vm.setCreatorPoint(selectedWaypoint, p)
                selectedWaypoint = -1
            } else vm.addCreatorPoint(p)
        }
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(creatorScroll),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = routeName, onValueChange = { routeName = it }, label = { Text("Route name") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = routeCategory, onValueChange = { routeCategory = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { vm.saveRoute(routeName.ifBlank { "My Route" }, routeCategory.ifBlank { "General" }, routeColor, RouteMode.STRAIGHT, editingRoute?.id) }, modifier = Modifier.weight(1f), enabled = creatorPath.size > 1) { Text(if (editingRoute == null) "Save" else "Update") }
            Button(onClick = vm::clearCreator, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Waypoints")
                creatorPath.forEachIndexed { i, _ ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${i + 1}. waypoint")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Move", modifier = Modifier.clickable { selectedWaypoint = i })
                            Text("Delete", modifier = Modifier.clickable { vm.deleteCreatorPoint(i) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPanel(
    runPath: List<GeoPoint>,
    creatorPath: List<GeoPoint>,
    currentLocation: GeoPoint?,
    currentBearing: Float,
    hasPermission: Boolean,
    permissionBlocked: Boolean,
    requestPermission: () -> Unit,
    creatorMode: Boolean,
    routeColor: Int,
    darkMapStyleEnabled: Boolean,
    satelliteImageryEnabled: Boolean,
    modifier: Modifier = Modifier,
    onTap: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var mapInitialized by remember { mutableStateOf(false) }
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }
    val targetStyleUrl = MapTileSources.styleUrl(darkMapStyleEnabled)
    var savedCameraLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedCameraLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedCameraZoom by rememberSaveable { mutableStateOf<Double?>(null) }
    var centered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasPermission) {
        if (hasPermission) centered = false
    }

    fun captureCamera(map: MapLibreMap) {
        val position = map.cameraPosition ?: return
        val target = position.target ?: return
        savedCameraLat = target.latitude
        savedCameraLon = target.longitude
        savedCameraZoom = position.zoom
    }

    fun restoreCamera(map: MapLibreMap): Boolean {
        val lat = savedCameraLat ?: return false
        val lon = savedCameraLon ?: return false
        val zoom = savedCameraZoom ?: return false
        map.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(lat, lon))
                    .zoom(zoom)
                    .build()
            )
        )
        return true
    }

    fun centerOnLocationIfNeeded(map: MapLibreMap) {
        if (centered) return
        if (!hasPermission || currentLocation == null) return
        map.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(currentLocation.latitude, currentLocation.longitude))
                    .zoom(MapTileSources.DEFAULT_ZOOM)
                    .build()
            )
        )
        centered = true
    }

    fun finishStyleLoad(map: MapLibreMap, styleUrl: String) {
        val style = map.style ?: return
        MapLibreOverlays.install(style, context, satelliteImageryEnabled)
        MapLibreOverlays.update(
            style,
            runPath,
            creatorPath,
            routeColor,
            currentLocation,
            currentBearing
        )
        appliedStyleUrl = styleUrl
        styleReady = true
        if (!restoreCamera(map)) {
            centerOnLocationIfNeeded(map)
        } else {
            centered = true
        }
    }
    var attributionCycle by remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        attributionCycle++
        onDispose {}
    }
    val attributionText = if (satelliteImageryEnabled) {
        stringResource(R.string.map_attribution_esri)
    } else {
        stringResource(R.string.map_attribution_openfreemap)
    }
    val attributionUrl = if (satelliteImageryEnabled) {
        stringResource(R.string.url_esri_attribution)
    } else {
        stringResource(R.string.url_openfreemap)
    }
    var attributionVisible by remember { mutableStateOf(true) }
    LaunchedEffect(attributionCycle, satelliteImageryEnabled) {
        attributionVisible = true
        delay(5_000)
        attributionVisible = false
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val map = mapLibreMap
            if (map != null && styleReady) {
                captureCamera(map)
            }
        }
    }

    fun applyStyle(map: MapLibreMap, styleUrl: String) {
        if (styleUrl == appliedStyleUrl) return
        if (map.style != null && styleReady) {
            captureCamera(map)
        }
        styleReady = false
        map.setStyle(styleUrl) { finishStyleLoad(map, styleUrl) }
    }

    val mapClickListener = remember(onTap) {
        MapLibreMap.OnMapClickListener { point ->
            onTap(GeoPoint(point.latitude, point.longitude))
            true
        }
    }

    LaunchedEffect(mapView) {
        if (mapInitialized) return@LaunchedEffect
        mapInitialized = true
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isLogoEnabled = false
            map.setMaxZoomPreference(MapTileSources.MAX_ZOOM)
        }
    }

    LaunchedEffect(satelliteImageryEnabled, styleReady, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        MapLibreOverlays.setSatelliteImageryEnabled(style, satelliteImageryEnabled)
    }

    LaunchedEffect(targetStyleUrl, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        applyStyle(map, targetStyleUrl)
    }

    LaunchedEffect(currentLocation, hasPermission, styleReady, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        if (savedCameraLat != null) return@LaunchedEffect
        centerOnLocationIfNeeded(map)
    }

    LaunchedEffect(creatorMode, mapLibreMap, mapClickListener, styleReady) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        map.removeOnMapClickListener(mapClickListener)
        if (creatorMode) {
            map.addOnMapClickListener(mapClickListener)
        }
    }

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = {
                    val map = mapLibreMap ?: return@AndroidView
                    if (targetStyleUrl != appliedStyleUrl) {
                        applyStyle(map, targetStyleUrl)
                        return@AndroidView
                    }
                    val style = map.style ?: return@AndroidView
                    if (!styleReady) return@AndroidView
                    MapLibreOverlays.setSatelliteImageryEnabled(style, satelliteImageryEnabled)
                    MapLibreOverlays.update(
                        style,
                        runPath,
                        creatorPath,
                        routeColor,
                        currentLocation,
                        currentBearing
                    )
                }
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = attributionVisible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = attributionText,
                    modifier = Modifier
                        .background(Color(0xA6FFFFFF), RoundedCornerShape(4.dp))
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(attributionUrl))
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF323232)
                )
            }
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xB0000000), RoundedCornerShape(10.dp))
                        .clickable { requestPermission() }
                        .padding(12.dp)
                ) {
                    Text(
                        if (permissionBlocked) {
                            "Location denied\nTap to open Settings"
                        } else {
                            "Location not enabled\nTap to allow"
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SavedRoutesTab(
    routes: List<SavedRoute>,
    onApply: (SavedRoute) -> Unit,
    onEdit: (SavedRoute) -> Unit,
    onDelete: (SavedRoute) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = routes.map { it.category }.distinct().sorted()
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedCategory != null) {
            Button(onClick = { selectedCategory = null }) { Text("Back to categories") }
            routes.filter { it.category == selectedCategory }.forEach { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApply(route) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(route.name)
                            val routeKm = route.waypoints.zipWithNext { a, b -> a.distanceToAsDouble(b) }.sum() / 1000.0
                            Text("${String.format(Locale.US, "%.2f", routeKm)} km")
                        }
                        Row {
                            IconButton(onClick = { onEdit(route) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit route", tint = Color(0xFFFF7043))
                            }
                            IconButton(onClick = { onDelete(route) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete route", tint = Color(0xFFFF7043))
                            }
                        }
                    }
                }
            }
        } else {
            Text("Route Categories", style = MaterialTheme.typography.titleMedium)
            categories.forEach { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategory = cat }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat, modifier = Modifier.padding(horizontal = 4.dp))
                        IconButton(onClick = {
                            onDeleteCategory(cat)
                            if (selectedCategory == cat) selectedCategory = null
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete category", tint = Color(0xFFFF7043))
                        }
                    }
                }
            }
            if (categories.isEmpty()) Text("No saved routes yet.")
        }
    }
}

data class UiAchievement(
    val id: String,
    val name: String,
    val description: String,
    val unlocked: Boolean,
    val progress: String
)

private fun buildAchievements(history: List<RunRecord>): List<UiAchievement> {
    val eligibleRuns = history.filter { it.distanceMeters >= 500.0 }
    val totalKm = eligibleRuns.sumOf { it.distanceMeters } / 1000.0
    val bestKm = (eligibleRuns.maxOfOrNull { it.distanceMeters } ?: 0.0) / 1000.0
    val bestPace = eligibleRuns.filter { it.avgPaceMinKm > 0 }.minOfOrNull { it.avgPaceMinKm } ?: Double.MAX_VALUE
    val totalCalories = eligibleRuns.sumOf { it.calories }
    val sameHourRuns = eligibleRuns.groupBy { hourOfDay(it.timestampMs) }.values.maxOfOrNull { it.size } ?: 0
    val longRuns = eligibleRuns.count { it.durationMs >= 90L * 60_000L }
    val sprintRuns = eligibleRuns.count { it.avgSpeedKmh >= 12.0 }
    val rainyGuess = eligibleRuns.count { hourOfDay(it.timestampMs) in 0..4 }
    val countriesRunIn = eligibleRuns.map { it.countryCode }.filter { it != "UNK" }.toSet().size
    val streakNow = currentStreak(eligibleRuns)
    val streakBest = bestStreak(eligibleRuns)
    val consistency30 = eligibleRuns.filterByWindow(StatsWindow.MONTH).size
    val lazyGapDays = maxGapDays(eligibleRuns)
    val monthKm = eligibleRuns.filterByWindow(StatsWindow.MONTH).sumOf { it.distanceMeters } / 1000.0
    val weekKm = eligibleRuns.filterByWindow(StatsWindow.WEEK).sumOf { it.distanceMeters } / 1000.0
    return listOf(
        UiAchievement("first", "First Run", "Finish your first tracked run.", eligibleRuns.isNotEmpty(), "${eligibleRuns.size}/1 runs"),
        UiAchievement("5k", "5K Finisher", "Run 5km in a single session.", bestKm >= 5.0, "${fmt(bestKm)}/5.00 km"),
        UiAchievement("10k", "10K Hero", "Run 10km in a single session.", bestKm >= 10.0, "${fmt(bestKm)}/10.00 km"),
        UiAchievement("speed", "Speed Demon", "Hit sub 5:00 min/km pace.", bestPace <= 5.0, "${fmtPace(bestPace)}/5.00 min/km"),
        UiAchievement("distance", "Century Club", "Accumulate 100km total.", totalKm >= 100.0, "${fmt(totalKm)}/100.00 km"),
        UiAchievement("streak", "Streak Keeper", "Run 3 days in a row.", streakNow >= 3, "$streakNow/3 days"),
        UiAchievement("night", "Night Owl", "Log a run after 10 PM.", eligibleRuns.any { hourOfDay(it.timestampMs) >= 22 }, "${eligibleRuns.count { hourOfDay(it.timestampMs) >= 22 }}/1 late runs"),
        UiAchievement("early", "Sunrise Sprinter", "Log a run before 6 AM.", eligibleRuns.any { hourOfDay(it.timestampMs) < 6 }, "${eligibleRuns.count { hourOfDay(it.timestampMs) < 6 }}/1 early runs"),
        UiAchievement("marathon", "Marathon Mindset", "Accumulate 42.2km.", totalKm >= 42.2, "${fmt(totalKm)}/42.20 km"),
        UiAchievement("fifty", "Half-Century", "Accumulate 50km total.", totalKm >= 50.0, "${fmt(totalKm)}/50.00 km"),
        UiAchievement("calburn", "Bonfire", "Burn 500+ calories in one run.", eligibleRuns.any { it.calories >= 500 }, "${eligibleRuns.maxOfOrNull { it.calories } ?: 0}/500 kcal"),
        UiAchievement("volcano", "Volcano Legs", "Burn 2,000 total calories.", totalCalories >= 2000, "$totalCalories/2000 kcal"),
        UiAchievement("clockwork", "Clockwork Runner", "Run 3+ times in the same hour slot.", sameHourRuns >= 3, "$sameHourRuns/3 runs"),
        UiAchievement("ultra", "Weekend Ultra Brain", "Complete two 90+ minute runs.", longRuns >= 2, "$longRuns/2 long runs"),
        UiAchievement("warp", "Warp Drive Ankles", "Log 5 runs averaging 12+ km/h.", sprintRuns >= 5, "$sprintRuns/5 fast runs"),
        UiAchievement("ghost", "Ghost Runner", "Run at 3 AM like a cryptid.", eligibleRuns.any { hourOfDay(it.timestampMs) == 3 }, "${eligibleRuns.count { hourOfDay(it.timestampMs) == 3 }}/1 ghost runs"),
        UiAchievement("gremlin", "Treadmill Gremlin", "Do 7 runs in 7 days.", eligibleRuns.filterByWindow(StatsWindow.WEEK).size >= 7, "${eligibleRuns.filterByWindow(StatsWindow.WEEK).size}/7 runs"),
        UiAchievement("orbit", "Orbital Jogger", "Accumulate 250km total.", totalKm >= 250.0, "${fmt(totalKm)}/250.00 km"),
        UiAchievement("dragon", "Dragon Breath", "Burn 1,000 calories in one run.", eligibleRuns.any { it.calories >= 1000 }, "${eligibleRuns.maxOfOrNull { it.calories } ?: 0}/1000 kcal"),
        UiAchievement("owlstack", "Double Owl", "Log 4 late-night runs.", eligibleRuns.count { hourOfDay(it.timestampMs) >= 22 } >= 4, "${eligibleRuns.count { hourOfDay(it.timestampMs) >= 22 }}/4 late runs"),
        UiAchievement("rain", "Rain Goblin", "Do 6 runs in deep-night hours.", rainyGuess >= 6, "$rainyGuess/6 night runs"),
        UiAchievement("boomerang", "Boomerang Runner", "Run twice in one day, 3 times.", eligibleRuns.groupBy { dayKey(it.timestampMs) }.values.count { it.size >= 2 } >= 3, "${eligibleRuns.groupBy { dayKey(it.timestampMs) }.values.count { it.size >= 2 }}/3 double-days"),
        UiAchievement("chaos", "Chaotic Neutral Cardio", "Run once in every daypart.", hasAllDayParts(eligibleRuns), "${dayPartCount(eligibleRuns)}/4 dayparts"),
        UiAchievement("collector", "Mileage Collector", "Accumulate 500km total.", totalKm >= 500.0, "${fmt(totalKm)}/500.00 km"),
        UiAchievement("atlas", "Atlas Ankles", "Accumulate 1,000km total.", totalKm >= 1000.0, "${fmt(totalKm)}/1000.00 km"),
        UiAchievement("worldly", "Worldly Runner", "Run in 2 different countries.", countriesRunIn >= 2, "$countriesRunIn/2 countries"),
        UiAchievement("passport", "Sneaker Passport", "Run in 4 different countries.", countriesRunIn >= 4, "$countriesRunIn/4 countries"),
        UiAchievement("daily", "No-Skip Engine", "Keep a 10-day streak.", streakNow >= 10, "$streakNow/10 days"),
        UiAchievement("iron", "Iron Rhythm", "Hit a 30-day best streak.", streakBest >= 30, "$streakBest/30 days"),
        UiAchievement("steady", "Steady Feet", "Log 20 runs in the last 30 days.", consistency30 >= 20, "$consistency30/20 runs"),
        UiAchievement("metronome", "Metronome Mode", "Log 25 runs in the last 30 days.", consistency30 >= 25, "$consistency30/25 runs"),
        UiAchievement("month100", "Monthly Beast", "Run 100km in a month.", monthKm >= 100.0, "${fmt(monthKm)}/100.00 km"),
        UiAchievement("month150", "Monthly Monster", "Run 150km in a month.", monthKm >= 150.0, "${fmt(monthKm)}/150.00 km"),
        UiAchievement("week30", "Weekly Grinder", "Run 30km in a week.", weekKm >= 30.0, "${fmt(weekKm)}/30.00 km"),
        UiAchievement("skipday", "Oops, Day Off", "Skip at least one day.", lazyGapDays >= 2, "$lazyGapDays/2 day gap"),
        UiAchievement("gaveup", "Gave Up Mid-Run", "Finish a run shorter than 5 minutes.", eligibleRuns.any { it.durationMs in 1 until 300_000 }, "${eligibleRuns.count { it.durationMs in 1 until 300_000 }}/1 short runs")
    )
}

private fun hasAllDayParts(history: List<RunRecord>): Boolean {
    val parts = history.map {
        when (hourOfDay(it.timestampMs)) {
            in 5..10 -> "morning"
            in 11..16 -> "day"
            in 17..21 -> "evening"
            else -> "night"
        }
    }.toSet()
    return parts.size == 4
}

private fun dayPartCount(history: List<RunRecord>): Int = history.map {
    when (hourOfDay(it.timestampMs)) {
        in 5..10 -> "morning"
        in 11..16 -> "day"
        in 17..21 -> "evening"
        else -> "night"
    }
}.toSet().size

private fun maxGapDays(history: List<RunRecord>): Int {
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

private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
private fun fmtPace(v: Double): String = if (v == Double.MAX_VALUE) "--" else String.format(Locale.US, "%.2f", v)

private fun bestStreak(records: List<RunRecord>): Int {
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

@Composable
private fun StreakCounter(history: List<RunRecord>) {
    val streak = currentStreak(history)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_streak_fire),
                contentDescription = "Streak fire",
                tint = Color.Unspecified
            )
            Text("Current streak: $streak day${if (streak == 1) "" else "s"}")
        }
    }
}

@Composable
private fun ColumnScope.StatsTab(history: List<RunRecord>) {
    var metric by rememberSaveable { mutableStateOf(StatsMetric.DISTANCE) }
    var window by rememberSaveable { mutableStateOf(StatsWindow.WEEK) }
    var windowMenuOpen by remember { mutableStateOf(false) }
    val filtered = history.filterByWindow(window)
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsSummaryCards(history)
        Box {
            Button(onClick = { windowMenuOpen = true }) {
                Text(window.name.lowercase().replaceFirstChar { it.uppercase() })
            }
            DropdownMenu(expanded = windowMenuOpen, onDismissRequest = { windowMenuOpen = false }) {
                StatsWindow.entries.forEach { w ->
                    DropdownMenuItem(
                        text = { Text(w.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            window = w
                            windowMenuOpen = false
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            StatsMetric.entries.forEach { m ->
                Button(
                    onClick = { metric = m },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (metric == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
            }
        }
        MetricGraph(filtered, metric, window)
        StreakCounter(history)
        AchievementSection(history)
        groupedRunsCard("Today", history.filterByWindow(StatsWindow.DAY))
        groupedRunsCard("Last week", history.filterByWindow(StatsWindow.WEEK).filterNot { isSameDay(it.timestampMs, System.currentTimeMillis()) })
        groupedRunsCard("Older", history.filterNot { it in history.filterByWindow(StatsWindow.WEEK) })
    }
}

@Composable
private fun StatsSummaryCards(history: List<RunRecord>) {
    val cards = listOf(
        "Today" to history.filterByWindow(StatsWindow.DAY),
        "Month" to history.filterByWindow(StatsWindow.MONTH),
        "Year" to history.filterByWindow(StatsWindow.YEAR),
        "Decade" to history.filterByWindow(StatsWindow.ALL).filter { System.currentTimeMillis() - it.timestampMs <= 10L * 365 * 24 * 60 * 60 * 1000 }
    )
    val summaryTextColor = MaterialTheme.colorScheme.onSurface
    cards.forEach { (title, runs) ->
        val km = runs.sumOf { it.distanceMeters } / 1000.0
        val duration = runs.sumOf { it.durationMs }
        val avgSpeed = if (duration > 0) (runs.sumOf { it.distanceMeters } / duration) * 3600.0 else 0.0
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = summaryTextColor)
                Text("${runs.size} runs", color = summaryTextColor)
                Text("${String.format(Locale.US, "%.1f", km)} km", color = summaryTextColor)
                Text("${String.format(Locale.US, "%.1f", avgSpeed)} km/h", color = summaryTextColor)
            }
        }
    }
}

@Composable
private fun MetricGraph(records: List<RunRecord>, metric: StatsMetric, window: StatsWindow) {
    val buckets = bucketize(records, metric, window)
    val max = (buckets.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Graph")
            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val width = size.width
                val height = size.height
                val unit = when (metric) {
                    StatsMetric.DISTANCE -> "km"
                    StatsMetric.DURATION -> "min"
                    StatsMetric.SPEED -> "km/h"
                }
                val points = buckets.mapIndexed { index, value ->
                    val x = if (buckets.size <= 1) 0f else (index.toFloat() / (buckets.size - 1).toFloat()) * width
                    val ratio = (value / max).toFloat().coerceIn(0f, 1f)
                    val y = height - (height * ratio)
                    Offset(x, y)
                }
                val steps = 5
                for (i in 0..steps) {
                    val y = height - (height * (i / steps.toFloat()))
                    val tickValue = (max / steps.toDouble()) * i
                    drawLine(
                        color = Color(0x55FFFFFF),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            "${String.format(Locale.US, "%.1f", tickValue)} $unit",
                            8f,
                            y - 4f,
                            Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 22f
                                isAntiAlias = true
                            }
                        )
                    }
                }
                points.zipWithNext { a, b ->
                    drawLine(
                        color = Color.White,
                        start = a,
                        end = b,
                        strokeWidth = 3f
                    )
                }
            }
        }
    }
}

private fun bucketize(records: List<RunRecord>, metric: StatsMetric, window: StatsWindow): List<Double> {
    return when (window) {
        StatsWindow.DAY -> (0..23).map { hour ->
            val r = records.filter { hourOfDay(it.timestampMs) == hour }
            metricValue(r, metric)
        }
        StatsWindow.WEEK -> (0..13).map { dayBack ->
            val start = System.currentTimeMillis() - dayBack * 86_400_000L
            val r = records.filter { isSameDay(it.timestampMs, start) }
            metricValue(r, metric)
        }.reversed()
        StatsWindow.MONTH -> (0..59).map { dayBack ->
            val start = System.currentTimeMillis() - dayBack * 86_400_000L
            val r = records.filter { isSameDay(it.timestampMs, start) }
            metricValue(r, metric)
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
                metricValue(records.filter {
                    Calendar.getInstance().apply { timeInMillis = it.timestampMs }.get(Calendar.YEAR) == y
                }, metric)
            }
        }
    }
}

private fun metricValue(records: List<RunRecord>, metric: StatsMetric): Double {
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

private fun List<RunRecord>.filterByWindow(window: StatsWindow): List<RunRecord> {
    val now = System.currentTimeMillis()
    return when (window) {
        StatsWindow.DAY -> filter { isSameDay(it.timestampMs, now) }
        StatsWindow.WEEK -> filter { now - it.timestampMs <= 7L * 86_400_000L }
        StatsWindow.MONTH -> filter { now - it.timestampMs <= 30L * 86_400_000L }
        StatsWindow.YEAR -> filter { now - it.timestampMs <= 365L * 86_400_000L }
        StatsWindow.ALL -> this
    }
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun hourOfDay(ts: Long): Int = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)

@Composable
private fun groupedRunsCard(title: String, runs: List<RunRecord>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (runs.isEmpty()) Text("No runs")
            runs.take(20).forEach { run ->
                val d = SimpleDateFormat("MMM d HH:mm", Locale.US).format(Date(run.timestampMs))
                val runSpeed = if (run.durationMs > 0) (run.distanceMeters / run.durationMs) * 3600.0 else 0.0
                Text("$d  •  ${String.format(Locale.US, "%.2f", run.distanceMeters / 1000.0)} km  •  ${formatDuration(run.durationMs)}  •  ${String.format(Locale.US, "%.1f", runSpeed)} km/h")
            }
        }
    }
}

@Composable
private fun DeferredCommitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    keyboardType: KeyboardType,
    onApply: () -> Unit,
    onFocusedChange: (Boolean) -> Unit = {},
    settingsResetVersion: Int = 0,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(settingsResetVersion) {
        focused = false
    }
    val exit = {
        onApply()
        focusManager.clearFocus()
    }
    BackHandler(enabled = focused) { exit() }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var wasKeyboardVisible by remember { mutableStateOf(imeBottom > 0) }
    LaunchedEffect(imeBottom, focused) {
        val keyboardVisible = imeBottom > 0
        if (wasKeyboardVisible && !keyboardVisible && focused) {
            exit()
        }
        wasKeyboardVisible = keyboardVisible
    }
    DisposableEffect(Unit) {
        onDispose {
            if (focused) onApply()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.replace("\n", "")) },
        singleLine = true,
        label = label,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onDone = { exit() }),
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                    exit()
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focus ->
                focused = focus.isFocused
                onFocusedChange(focus.isFocused)
                if (!focus.isFocused) onApply()
            }
    )
}

@Composable
private fun SettingsIntField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    settingsResetVersion: Int,
    keyboardType: KeyboardType = KeyboardType.Number,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var text by rememberSaveable(settingsResetVersion) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!focused) {
            text = value.toString()
        }
    }
    val apply = {
        val snapped = snapIntSetting(text, value)
        text = snapped.toString()
        onValueChange(snapped)
    }
    DeferredCommitTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        keyboardType = keyboardType,
        onApply = apply,
        onFocusedChange = { focused = it },
        settingsResetVersion = settingsResetVersion,
        modifier = modifier
    )
}

@Composable
private fun SettingsDoubleField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    settingsResetVersion: Int,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var text by rememberSaveable(settingsResetVersion) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!focused) {
            text = value.toString()
        }
    }
    val apply = {
        val snapped = snapDoubleSetting(text, value)
        text = snapped.toString()
        onValueChange(snapped)
    }
    DeferredCommitTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        keyboardType = KeyboardType.Decimal,
        onApply = apply,
        onFocusedChange = { focused = it },
        settingsResetVersion = settingsResetVersion,
        modifier = modifier
    )
}

private fun snapIntSetting(input: String, fallback: Int): Int {
    return input.toIntOrNull() ?: fallback
}

private fun snapDoubleSetting(input: String, fallback: Double): Double {
    return input.toDoubleOrNull() ?: fallback
}

@Composable
private fun DataLicensesCard() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_data_licenses_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_map_data_body),
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.settings_osm_attribution_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_osm_attribution_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_osm_copyright)))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_license_page))
            }
            Text(stringResource(R.string.settings_openfreemap_attribution_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_openfreemap_attribution_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_openfreemap)))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_license_page))
            }
            Text(stringResource(R.string.settings_esri_attribution_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_esri_attribution_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_esri_attribution)))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_license_page))
            }
        }
    }
}

@Composable
private fun ColumnScope.SettingsTab(
    settings: RunSettings,
    settingsResetVersion: Int,
    history: List<RunRecord>,
    onUpdate: ((RunSettings) -> RunSettings) -> Unit,
    onDeleteAllAppData: () -> Unit,
    onExportRuns: () -> Unit,
    onPickGoalSound: () -> Unit,
    onPreviewGoalSound: () -> Unit
) {
    val context = LocalContext.current
    var perRunGoalText by rememberSaveable(settingsResetVersion, settings.distanceUnit) {
        val km = if (settings.perRunDistanceGoalKm > 0) {
            settings.perRunDistanceGoalKm
        } else {
            defaultGoalKm(settings.distanceUnit)
        }
        mutableStateOf(displayGoalValue(km, settings.distanceUnit))
    }
    var weeklyGoalText by rememberSaveable(settingsResetVersion, settings.distanceUnit) {
        val km = if (settings.weeklyDistanceGoalKm > 0) {
            settings.weeklyDistanceGoalKm
        } else {
            defaultGoalKm(settings.distanceUnit)
        }
        mutableStateOf(displayGoalValue(km, settings.distanceUnit))
    }
    var perRunGoalFocused by remember { mutableStateOf(false) }
    var weeklyGoalFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(settingsResetVersion) {
        perRunGoalFocused = false
        weeklyGoalFocused = false
        focusManager.clearFocus()
    }
    LaunchedEffect(settings.perRunDistanceGoalKm, settings.distanceUnit) {
        if (!perRunGoalFocused && settings.perRunDistanceGoalKm > 0) {
            perRunGoalText = displayGoalValue(settings.perRunDistanceGoalKm, settings.distanceUnit)
        }
    }
    LaunchedEffect(settings.weeklyDistanceGoalKm, settings.distanceUnit) {
        if (!weeklyGoalFocused && settings.weeklyDistanceGoalKm > 0) {
            weeklyGoalText = displayGoalValue(settings.weeklyDistanceGoalKm, settings.distanceUnit)
        }
    }
    var showDeleteAllConfirm by rememberSaveable { mutableStateOf(false) }
    val perRunEnabled = settings.perRunDistanceGoalKm > 0
    val weeklyEnabled = settings.weeklyDistanceGoalKm > 0
    val soundLabel = remember(settings.goalAlertMuted, settings.goalAlertSoundUri) {
        GoalAlertNotifier(context).soundLabel(settings.goalAlertMuted, settings.goalAlertSoundUri)
    }
    val unitLabel = goalUnitLabel(settings.distanceUnit)
    val applyPerRunGoal = {
        val km = snapGoalKm(perRunGoalText, settings.distanceUnit)
        perRunGoalText = displayGoalValue(km, settings.distanceUnit)
        onUpdate { it.copy(perRunDistanceGoalKm = km) }
    }
    val applyWeeklyGoal = {
        val km = snapGoalKm(weeklyGoalText, settings.distanceUnit)
        weeklyGoalText = displayGoalValue(km, settings.distanceUnit)
        onUpdate { it.copy(weeklyDistanceGoalKm = km) }
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distance goals", style = MaterialTheme.typography.titleMedium)
                settingToggle("Per-run distance goal", perRunEnabled) {
                    if (perRunEnabled) {
                        onUpdate { it.copy(perRunDistanceGoalKm = 0.0) }
                    } else {
                        val km = snapGoalKm(perRunGoalText, settings.distanceUnit)
                        perRunGoalText = displayGoalValue(km, settings.distanceUnit)
                        onUpdate { it.copy(perRunDistanceGoalKm = km) }
                    }
                }
                if (perRunEnabled) {
                    DeferredCommitTextField(
                        value = perRunGoalText,
                        onValueChange = { perRunGoalText = it },
                        label = { Text("Per-run goal ($unitLabel)") },
                        keyboardType = KeyboardType.Decimal,
                        onApply = applyPerRunGoal,
                        onFocusedChange = { perRunGoalFocused = it },
                        settingsResetVersion = settingsResetVersion
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalPresetChip("5K") {
                            perRunGoalText = displayGoalValue(5.0, settings.distanceUnit)
                            onUpdate { it.copy(perRunDistanceGoalKm = 5.0) }
                        }
                        GoalPresetChip("10K") {
                            perRunGoalText = displayGoalValue(10.0, settings.distanceUnit)
                            onUpdate { it.copy(perRunDistanceGoalKm = 10.0) }
                        }
                    }
                }
                settingToggle("Weekly distance goal", weeklyEnabled) {
                    if (weeklyEnabled) {
                        onUpdate { it.copy(weeklyDistanceGoalKm = 0.0) }
                    } else {
                        val km = snapGoalKm(weeklyGoalText, settings.distanceUnit)
                        weeklyGoalText = displayGoalValue(km, settings.distanceUnit)
                        onUpdate { it.copy(weeklyDistanceGoalKm = km) }
                    }
                }
                if (weeklyEnabled) {
                    DeferredCommitTextField(
                        value = weeklyGoalText,
                        onValueChange = { weeklyGoalText = it },
                        label = { Text("Weekly goal ($unitLabel)") },
                        keyboardType = KeyboardType.Decimal,
                        onApply = applyWeeklyGoal,
                        onFocusedChange = { weeklyGoalFocused = it },
                        settingsResetVersion = settingsResetVersion
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalPresetChip("20K") {
                            weeklyGoalText = displayGoalValue(20.0, settings.distanceUnit)
                            onUpdate { it.copy(weeklyDistanceGoalKm = 20.0) }
                        }
                        GoalPresetChip("50K") {
                            weeklyGoalText = displayGoalValue(50.0, settings.distanceUnit)
                            onUpdate { it.copy(weeklyDistanceGoalKm = 50.0) }
                        }
                        GoalPresetChip("100K") {
                            weeklyGoalText = displayGoalValue(100.0, settings.distanceUnit)
                            onUpdate { it.copy(weeklyDistanceGoalKm = 100.0) }
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Goal alerts", style = MaterialTheme.typography.titleMedium)
                settingToggle("Mute goal sounds", settings.goalAlertMuted) {
                    onUpdate { it.copy(goalAlertMuted = !it.goalAlertMuted) }
                }
                Text("Sound: $soundLabel")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPickGoalSound, modifier = Modifier.weight(1f)) { Text("Choose sound") }
                    Button(
                        onClick = onPreviewGoalSound,
                        modifier = Modifier.weight(1f),
                        enabled = !settings.goalAlertMuted
                    ) { Text("Preview") }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                settingToggle("Dark mode", settings.darkMode) { onUpdate { it.copy(darkMode = !it.darkMode) } }
                settingToggle("Use miles", settings.distanceUnit == DistanceUnit.MI) {
                    onUpdate { it.copy(distanceUnit = if (it.distanceUnit == DistanceUnit.KM) DistanceUnit.MI else DistanceUnit.KM) }
                }
                settingToggle("Battery saver GPS", settings.batterySaver) {
                    onUpdate { it.copy(batterySaver = !it.batterySaver) }
                }
                settingToggle(stringResource(R.string.setting_dark_map_style), settings.darkMapStyleEnabled) {
                    onUpdate { it.copy(darkMapStyleEnabled = !it.darkMapStyleEnabled) }
                }
                settingToggle(stringResource(R.string.setting_satellite_imagery), settings.satelliteImageryEnabled) {
                    onUpdate { it.copy(satelliteImageryEnabled = !it.satelliteImageryEnabled) }
                }
                settingToggle("Comical reminders", settings.remindersEnabled) { onUpdate { it.copy(remindersEnabled = !it.remindersEnabled) } }
                Text("Creator waypoint color")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorChoice(if (settings.darkMode) "Black" else "Better purple", if (settings.darkMode) 0xFF000000.toInt() else 0xFF7E57C2.toInt(), settings.creatorRouteColor, onUpdate)
                    ColorChoice("Teal", 0xFF00897B.toInt(), settings.creatorRouteColor, onUpdate)
                    ColorChoice("Orange", 0xFFEF6C00.toInt(), settings.creatorRouteColor, onUpdate)
                }
                SettingsIntField(
                    label = "Age",
                    value = settings.age,
                    onValueChange = { onUpdate { s -> s.copy(age = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                SettingsIntField(
                    label = "Height (cm)",
                    value = settings.heightCm,
                    onValueChange = { onUpdate { s -> s.copy(heightCm = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                SettingsDoubleField(
                    label = "Weight (kg)",
                    value = settings.weightKg,
                    onValueChange = { onUpdate { s -> s.copy(weightKg = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                SettingsIntField(
                    label = "Calorie goal per run",
                    value = settings.calorieGoalPerRun,
                    onValueChange = { onUpdate { s -> s.copy(calorieGoalPerRun = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                Text(
                    stringResource(R.string.calorie_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DataLicensesCard()
        OutlinedButton(
            onClick = onExportRuns,
            enabled = history.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export run history (CSV)")
        }
        if (history.isEmpty()) {
            Text("No runs to export", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { showDeleteAllConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Delete all app data")
        }
    }
    if (showDeleteAllConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Delete all app data?") },
            text = {
                Text(
                    "This will permanently erase all runs, saved routes, settings, and other stored data, " +
                        "and reset the app to its first-launch state. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllAppData()
                        showDeleteAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Reset app") }
            },
            dismissButton = {
                Button(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AchievementSection(history: List<RunRecord>) {
    val achievements = buildAchievements(history)
    Text("Achievements", style = MaterialTheme.typography.titleMedium)
    achievements.forEach { ach ->
        var open by rememberSaveable(ach.id) { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open },
            colors = CardDefaults.cardColors(containerColor = if (ach.unlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${if (ach.unlocked) "✅" else "🔒"} ${ach.name}")
                if (open) {
                    Text(ach.description)
                    Text("Progress: ${ach.progress}")
                }
            }
        }
    }
}

@Composable
private fun ColorChoice(
    label: String,
    color: Int,
    selectedColor: Int,
    onUpdate: ((RunSettings) -> RunSettings) -> Unit
) {
    Button(
        onClick = { onUpdate { it.copy(creatorRouteColor = color) } },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) { Text(label) }
}

@Composable
private fun GoalProgressRow(
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
    val accent = if (reached) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: $currentLabel / $goalLabel", color = if (reached) accent else MaterialTheme.colorScheme.onSurface)
            if (reached) {
                Text("Goal reached", color = accent, style = MaterialTheme.typography.labelSmall)
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
private fun GoalProgressCard(stats: RunStats, settings: RunSettings, history: List<RunRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Distance goals", style = MaterialTheme.typography.titleSmall)
            if (settings.perRunDistanceGoalKm > 0) {
                GoalProgressRow(
                    label = "This run",
                    currentMeters = stats.distanceMeters,
                    goalKm = settings.perRunDistanceGoalKm,
                    unit = settings.distanceUnit,
                    compact = false
                )
            }
            if (settings.weeklyDistanceGoalKm > 0) {
                GoalProgressRow(
                    label = "This week",
                    currentMeters = GoalAlertController.calendarWeekDistanceMeters(history, stats.distanceMeters),
                    goalKm = settings.weeklyDistanceGoalKm,
                    unit = settings.distanceUnit,
                    compact = false
                )
            }
        }
    }
}

private fun displayGoalValue(km: Double, unit: DistanceUnit): String {
    val value = if (unit == DistanceUnit.KM) km else km * 0.621371
    val decimals = if (value < 0.01) 4 else 2
    return String.format(Locale.US, "%.${decimals}f", value)
}

private fun parseGoalInput(input: String, unit: DistanceUnit): Double? {
    val value = input.toDoubleOrNull() ?: return null
    return if (unit == DistanceUnit.KM) value else value / 0.621371
}

private fun defaultGoalKm(unit: DistanceUnit): Double = parseGoalInput("1", unit)!!

private fun snapGoalKm(input: String, unit: DistanceUnit): Double {
    val parsed = parseGoalInput(input, unit) ?: return MIN_GOAL_KM
    return parsed.coerceIn(MIN_GOAL_KM, MAX_GOAL_KM)
}

private fun goalUnitLabel(unit: DistanceUnit): String = if (unit == DistanceUnit.KM) "km" else "mi"

@Composable
private fun GoalPresetChip(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun settingToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun currentStreak(records: List<RunRecord>): Int {
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

private fun dayKey(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}
