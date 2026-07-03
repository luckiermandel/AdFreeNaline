package com.luckierdev.adfreenaline

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.PrefsMigrator
import com.luckierdev.adfreenaline.data.entities.AppMetaEntity
import com.luckierdev.adfreenaline.data.entities.AppSettingsEntity
import com.luckierdev.adfreenaline.data.mappers.toRunSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale

class RunViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = RunSettingsRepository(application)
    private val historyRepository = RunHistoryRepository(application)
    private val activeSessionRepository = ActiveSessionRepository(application)
    private val manager = RunTrackingManager(application, activeSessionRepository)
    private val reminderScheduler = ReminderScheduler(application)
    private val routeRepository = RouteRepository(application)
    private val goalAlertController = GoalAlertController(application, settingsRepository)

    val stats: StateFlow<RunStats> = manager.stats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RunStats()
    )

    val runPath: StateFlow<List<GeoPoint>> = manager.runPath.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val creatorPath: StateFlow<List<GeoPoint>> = manager.routeCreatorPath.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val currentLocation: StateFlow<GeoPoint?> = manager.currentLocation.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )
    val currentBearing: StateFlow<Float> = manager.currentBearing.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0f
    )
    val settings: StateFlow<RunSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RunSettings()
    )
    val history: StateFlow<List<RunRecord>> = historyRepository.history.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val savedRoutes: StateFlow<List<SavedRoute>> = routeRepository.routes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    private val _settingsResetVersion = MutableStateFlow(0)
    val settingsResetVersion: StateFlow<Int> = _settingsResetVersion.asStateFlow()

    fun startRun() {
        goalAlertController.resetPerRunAlert()
        manager.startRun()
    }
    fun pauseRun() = manager.pauseRun()
    fun resumeRun() = manager.resumeRun()
    fun stopRun() {
        val final = stats.value
        manager.stopRun()
        manager.startLocationWatch()
        if (RunSessionPolicy.shouldSaveRun(final.distanceMeters)) {
            historyRepository.addRecord(
                RunRecord(
                    timestampMs = System.currentTimeMillis(),
                    durationMs = final.durationMs,
                    distanceMeters = final.distanceMeters,
                    avgPaceMinKm = final.avgPaceMinKm,
                    avgSpeedKmh = final.avgSpeedKmh,
                    calories = final.calories,
                    countryCode = resolveCountryCode()
                )
            )
        }
    }

    fun addCreatorPoint(point: GeoPoint) = manager.addRoutePoint(point)
    fun setCreatorPoint(index: Int, point: GeoPoint) = manager.setRoutePoint(index, point)
    fun deleteCreatorPoint(index: Int) = manager.deleteRoutePoint(index)
    fun clearCreator() = manager.clearRouteCreator()
    fun startLocationWatch() = manager.startLocationWatch()
    fun stopLocationWatch() = manager.stopLocationWatch()

    fun updateSettings(transform: (RunSettings) -> RunSettings) {
        settingsRepository.update(transform)
        val updated = settingsRepository.settings.value
        manager.updateSettings(updated)
        reminderScheduler.schedule(updated.remindersEnabled)
    }

    fun completeOnboarding(sex: BiologicalSex, age: Int, heightCm: Int) {
        updateSettings {
            it.copy(
                sex = sex,
                age = age,
                heightCm = heightCm,
                onboardingComplete = true
            )
        }
    }

    fun skipOnboarding() {
        updateSettings { it.copy(onboardingComplete = true) }
    }

    fun saveRoute(name: String, category: String, colorHex: Int, mode: RouteMode, existingId: Long? = null) {
        val points = creatorPath.value
        if (points.size < 2) return
        routeRepository.save(
            SavedRoute(
                id = existingId ?: System.currentTimeMillis(),
                name = name,
                category = category,
                colorHex = colorHex,
                mode = mode,
                waypoints = points
            )
        )
    }

    fun loadRoute(route: SavedRoute) {
        manager.replaceRoute(route.waypoints)
    }

    fun deleteRoute(routeId: Long) {
        routeRepository.delete(routeId)
    }

    fun deleteCategory(category: String) {
        routeRepository.deleteCategory(category)
    }

    fun deleteAllAppData() {
        viewModelScope.launch {
            manager.resetSession()
            val db = AppDatabase.getInstance(getApplication())
            db.withTransaction {
                db.clearAllTables()
                db.settingsDao().upsert(AppSettingsEntity())
                db.metaDao().upsert(AppMetaEntity(PrefsMigrator.MIGRATION_KEY, "true"))
            }
            goalAlertController.clearAll()
            val defaults = RunSettings()
            manager.updateSettings(defaults)
            reminderScheduler.schedule(false)
            _settingsResetVersion.value++
        }
    }

    fun previewGoalSound() {
        goalAlertController.previewSound(settings.value)
    }

    fun goalSoundLabel(): String = goalAlertController.soundLabel(settings.value)

    init {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val db = AppDatabase.getInstance(app)
            val initialSettings = db.settingsDao().get()?.toRunSettings() ?: RunSettings()
            manager.updateSettings(initialSettings)
            reminderScheduler.schedule(initialSettings.remindersEnabled)

            val snapshot = activeSessionRepository.load()
            if (snapshot != null && snapshot.isTracking) {
                val ageMs = System.currentTimeMillis() - snapshot.trackingStartMs
                if (RunSessionPolicy.shouldRestoreSession(ageMs)) {
                    manager.restoreRun(snapshot)
                } else {
                    activeSessionRepository.clearSuspend()
                    manager.startLocationWatch()
                }
            } else {
                manager.startLocationWatch()
            }

            combine(manager.stats, settingsRepository.settings, historyRepository.history) { stats, settings, history ->
                Triple(stats, settings, history)
            }.collect { (stats, settings, history) ->
                if (stats.isTracking && !stats.isPaused) {
                    goalAlertController.evaluate(stats, settings, history)
                }
            }
        }
    }

    private fun resolveCountryCode(): String {
        val point = currentLocation.value ?: return "UNK"
        return runCatching {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            geocoder.getFromLocation(point.latitude, point.longitude, 1)
                ?.firstOrNull()
                ?.countryCode
                ?.uppercase(Locale.US)
                ?: "UNK"
        }.getOrDefault("UNK")
    }
}
