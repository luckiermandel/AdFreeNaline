package com.luckierdev.adfreenaline

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.luckierdev.adfreenaline.data.mappers.ActiveRunSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

private const val ACTIVE_SESSION_PERSIST_INTERVAL_MS = 15_000L

data class RunStats(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val durationMs: Long = 0L,
    val distanceMeters: Double = 0.0,
    val avgPaceMinKm: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val calories: Int = 0
)

class RunTrackingManager(
    context: Context,
    private val activeSessionRepository: ActiveSessionRepository
) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationExecutor = ContextCompat.getMainExecutor(appContext)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var trackingStartMs = 0L
    private var pausedAccumulatedMs = 0L
    private var pauseStartMs = 0L
    private var lastLocation: Location? = null
    private var lastPersistMs = 0L
    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            val current = _stats.value
            if (!current.isTracking || current.isPaused) return
            refreshLiveStats()
            persistActiveSessionIfNeeded()
            tickHandler.postDelayed(this, 1000L)
        }
    }

    private val _runPath = MutableStateFlow<List<GeoPoint>>(emptyList())
    val runPath: StateFlow<List<GeoPoint>> = _runPath.asStateFlow()

    private val _routeCreatorPath = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routeCreatorPath: StateFlow<List<GeoPoint>> = _routeCreatorPath.asStateFlow()

    private val _stats = MutableStateFlow(RunStats())
    val stats: StateFlow<RunStats> = _stats.asStateFlow()
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()
    private val _currentBearing = MutableStateFlow(0f)
    val currentBearing: StateFlow<Float> = _currentBearing.asStateFlow()
    private var settings: RunSettings = RunSettings()
    private var isPassiveWatching = false

    private val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun updateSettings(value: RunSettings) {
        val oldSettings = settings
        settings = value
        if (oldSettings.batterySaver != value.batterySaver) {
            refreshLocationUpdatesForPowerMode()
        }
    }

    fun addRoutePoint(point: GeoPoint) {
        _routeCreatorPath.value = _routeCreatorPath.value + point
    }

    fun setRoutePoint(index: Int, point: GeoPoint) {
        val current = _routeCreatorPath.value.toMutableList()
        if (index !in current.indices) return
        current[index] = point
        _routeCreatorPath.value = current
    }

    fun deleteRoutePoint(index: Int) {
        val current = _routeCreatorPath.value.toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        _routeCreatorPath.value = current
    }

    fun replaceRoute(points: List<GeoPoint>) {
        _routeCreatorPath.value = points
    }

    fun clearRouteCreator() {
        _routeCreatorPath.value = emptyList()
    }

    fun resetSession() {
        if (_stats.value.isTracking) {
            stopRun()
        }
        _runPath.value = emptyList()
        _routeCreatorPath.value = emptyList()
        _stats.value = RunStats()
        activeSessionRepository.clear()
    }

    @SuppressLint("MissingPermission")
    fun restoreRun(snapshot: ActiveRunSnapshot) {
        trackingStartMs = snapshot.trackingStartMs
        pausedAccumulatedMs = snapshot.pausedAccumulatedMs
        pauseStartMs = snapshot.pauseStartMs
        lastLocation = snapshot.lastLocation
        _runPath.value = snapshot.path
        _stats.value = snapshot.stats
        isPassiveWatching = false
        ensureRunChannel()
        showRunNotification(_stats.value)
        if (!snapshot.isPaused && hasLocationPermission()) {
            stopLocationUpdates()
            requestLocationUpdates()
            startLiveStatsTick()
        }
    }

    @SuppressLint("MissingPermission")
    fun startRun() {
        if (_stats.value.isTracking) return
        if (!hasLocationPermission()) return
        activeSessionRepository.clear()
        stopLocationUpdates()
        trackingStartMs = System.currentTimeMillis()
        pausedAccumulatedMs = 0L
        pauseStartMs = 0L
        lastLocation = null
        _runPath.value = emptyList()
        _stats.value = RunStats(isTracking = true, isPaused = false)
        isPassiveWatching = false
        ensureRunChannel()
        showRunNotification(_stats.value)
        seedCurrentLocation()
        requestLocationUpdates()
        startLiveStatsTick()
        persistActiveSession(force = true)
    }

    fun pauseRun() {
        val current = _stats.value
        if (!current.isTracking || current.isPaused) return
        pauseStartMs = System.currentTimeMillis()
        _stats.value = current.copy(isPaused = true)
        showRunNotification(_stats.value)
        stopLocationUpdates()
        stopLiveStatsTick()
        persistActiveSession(force = true)
    }

    @SuppressLint("MissingPermission")
    fun resumeRun() {
        val current = _stats.value
        if (!current.isTracking || !current.isPaused) return
        pausedAccumulatedMs += System.currentTimeMillis() - pauseStartMs
        pauseStartMs = 0L
        _stats.value = current.copy(isPaused = false)
        showRunNotification(_stats.value)
        requestLocationUpdates()
        startLiveStatsTick()
        persistActiveSession(force = true)
    }

    fun stopRun() {
        stopLiveStatsTick()
        stopLocationUpdates()
        _stats.value = _stats.value.copy(isTracking = false, isPaused = false)
        lastLocation = null
        isPassiveWatching = false
        notificationManager.cancel(7021)
        appContext.startService(Intent(appContext, RunForegroundService::class.java).apply {
            action = RunForegroundService.ACTION_STOP
        })
        activeSessionRepository.clear()
    }

    @SuppressLint("MissingPermission")
    fun startLocationWatch() {
        if (!hasLocationPermission()) return
        fetchCurrentLocationImmediately()
        if (isPassiveWatching) return
        isPassiveWatching = true
        requestLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationImmediately() {
        if (!hasLocationPermission()) return
        locationManager.getLastKnownLocation(locationProvider())?.let(::applyPassiveLocation)
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            locationProvider(),
            null,
            locationExecutor
        ) { location ->
            applyPassiveLocation(location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun seedCurrentLocation() {
        if (!hasLocationPermission()) return
        locationManager.getLastKnownLocation(locationProvider())?.let(::applyPassiveLocation)
    }

    private fun applyPassiveLocation(location: Location) {
        _currentLocation.value = GeoPoint(location.latitude, location.longitude)
        if (location.hasBearing()) _currentBearing.value = location.bearing
    }

    fun stopLocationWatch() {
        if (!isPassiveWatching) return
        stopLocationUpdates()
        isPassiveWatching = false
    }

    private fun locationProvider(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        } else {
            LocationManager.GPS_PROVIDER
        }
    }

    private fun currentLocationRequest(): LocationRequestCompat {
        val intervalMs = if (settings.batterySaver) 6000L else 2000L
        val minIntervalMs = if (settings.batterySaver) 3000L else 1000L
        val minDistanceM = if (settings.batterySaver) 5f else 2f
        val quality = if (settings.batterySaver) {
            LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
        } else {
            LocationRequestCompat.QUALITY_HIGH_ACCURACY
        }
        return LocationRequestCompat.Builder(intervalMs)
            .setMinUpdateIntervalMillis(minIntervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .setQuality(quality)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) return
        LocationManagerCompat.requestLocationUpdates(
            locationManager,
            locationProvider(),
            currentLocationRequest(),
            locationExecutor,
            locationListener
        )
    }

    private fun stopLocationUpdates() {
        LocationManagerCompat.removeUpdates(locationManager, locationListener)
    }

    @SuppressLint("MissingPermission")
    private fun refreshLocationUpdatesForPowerMode() {
        stopLocationUpdates()
        val current = _stats.value
        if (current.isTracking && !current.isPaused) {
            requestLocationUpdates()
            return
        }
        if (isPassiveWatching) {
            requestLocationUpdates()
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val currentStats = _stats.value
        _currentLocation.value = GeoPoint(location.latitude, location.longitude)
        _currentBearing.value = when {
            location.hasBearing() -> location.bearing
            lastLocation != null -> lastLocation!!.bearingTo(location)
            else -> _currentBearing.value
        }
        if (!currentStats.isTracking || currentStats.isPaused) return

        val elapsed = currentElapsedMs()
        val prev = lastLocation
        val incrementalDistance = prev?.distanceTo(location)?.toDouble() ?: 0.0
        if (prev != null) {
            val deltaMs = (location.time - prev.time).coerceAtLeast(1L)
            if (!shouldAcceptGpsSegment(incrementalDistance, deltaMs)) {
                lastLocation = location
                return
            }
        }

        val newDistance = currentStats.distanceMeters + incrementalDistance
        val newPath = _runPath.value + GeoPoint(location.latitude, location.longitude)

        _runPath.value = newPath
        refreshLiveStats(distanceMeters = newDistance, elapsedMs = elapsed)
        lastLocation = location
        persistActiveSession(force = true)
    }

    private fun currentElapsedMs(): Long {
        if (!_stats.value.isTracking || _stats.value.isPaused) return _stats.value.durationMs
        return System.currentTimeMillis() - trackingStartMs - pausedAccumulatedMs
    }

    private fun refreshLiveStats(distanceMeters: Double? = null, elapsedMs: Long? = null) {
        val current = _stats.value
        if (!current.isTracking || current.isPaused) return

        val elapsed = elapsedMs ?: currentElapsedMs()
        val distance = distanceMeters ?: current.distanceMeters
        val pace = computeAvgPaceMinKm(distance, elapsed)
        val speedKmh = computeAvgSpeedKmh(distance, elapsed)
        val calories = estimateCaloriesBurned(elapsed, speedKmh, settings)

        _stats.value = current.copy(
            durationMs = elapsed,
            distanceMeters = distance,
            avgPaceMinKm = pace,
            avgSpeedKmh = speedKmh,
            calories = calories
        )
        showRunNotification(_stats.value)
    }

    private fun startLiveStatsTick() {
        tickHandler.removeCallbacks(tickRunnable)
        tickHandler.post(tickRunnable)
    }

    private fun stopLiveStatsTick() {
        tickHandler.removeCallbacks(tickRunnable)
    }

    private fun ensureRunChannel() {
        val channel = NotificationChannel(
            "run_live_channel",
            "Live Run",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun showRunNotification(stats: RunStats) {
        val distanceKm = stats.distanceMeters / 1000.0
        val duration = formatDuration(stats.durationMs)
        val text = "${String.format("%.2f", distanceKm)} km | ${String.format("%.2f", stats.avgSpeedKmh)} km/h | ${stats.calories} kcal"
        val title = if (stats.isPaused) "Run paused ($duration)" else "Run active ($duration)"
        val notification = buildRunNotification(title, text)
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, RunForegroundService::class.java).apply {
                action = if (stats.durationMs == 0L) RunForegroundService.ACTION_START else RunForegroundService.ACTION_UPDATE
                putExtra(RunForegroundService.EXTRA_TITLE, title)
                putExtra(RunForegroundService.EXTRA_CONTENT, text)
            }
        )
        notificationManager.notify(RunForegroundService.NOTIFICATION_ID, notification)
    }

    private fun buildRunNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(appContext, RunForegroundService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun persistActiveSessionIfNeeded() {
        persistActiveSession(force = false)
    }

    private fun persistActiveSession(force: Boolean) {
        val current = _stats.value
        if (!current.isTracking) return
        val now = System.currentTimeMillis()
        if (!force && now - lastPersistMs < ACTIVE_SESSION_PERSIST_INTERVAL_MS) return
        lastPersistMs = now
        activeSessionRepository.save(
            ActiveRunSnapshot(
                isTracking = current.isTracking,
                isPaused = current.isPaused,
                trackingStartMs = trackingStartMs,
                pausedAccumulatedMs = pausedAccumulatedMs,
                pauseStartMs = pauseStartMs,
                stats = current,
                lastLocation = lastLocation,
                path = _runPath.value
            )
        )
    }
}
