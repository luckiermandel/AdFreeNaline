package com.luckierdev.adfreenaline

import android.content.Context
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.entities.AppSettingsEntity
import com.luckierdev.adfreenaline.data.mappers.toEntity
import com.luckierdev.adfreenaline.data.mappers.toRunSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RunSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _settings = MutableStateFlow(RunSettings())
    val settings: StateFlow<RunSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            val dao = AppDatabase.getInstance(appContext).settingsDao()
            dao.observe()
                .map { entity -> entity?.toRunSettings() ?: RunSettings() }
                .collect { _settings.value = it }
        }
    }

    fun update(transform: (RunSettings) -> RunSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        scope.launch {
            val db = AppDatabase.getInstance(appContext)
            val currentEntity = db.settingsDao().get() ?: AppSettingsEntity()
            db.settingsDao().upsert(
                updated.toEntity(lastWeeklyGoalAlertKey = currentEntity.lastWeeklyGoalAlertKey)
            )
        }
    }

    fun resetToDefaults() {
        scope.launch {
            val db = AppDatabase.getInstance(appContext)
            db.settingsDao().deleteAll()
            db.settingsDao().upsert(AppSettingsEntity())
        }
    }

    suspend fun getLastWeeklyGoalAlertKey(): String? {
        return AppDatabase.getInstance(appContext).settingsDao().get()?.lastWeeklyGoalAlertKey
    }

    suspend fun setLastWeeklyGoalAlertKey(key: String?) {
        val db = AppDatabase.getInstance(appContext)
        val current = db.settingsDao().get() ?: AppSettingsEntity()
        db.settingsDao().upsert(current.copy(lastWeeklyGoalAlertKey = key))
    }
}
