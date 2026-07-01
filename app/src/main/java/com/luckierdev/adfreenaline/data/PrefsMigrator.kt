package com.luckierdev.adfreenaline.data

import android.content.Context
import androidx.room.withTransaction
import com.luckierdev.adfreenaline.data.entities.AppMetaEntity
import com.luckierdev.adfreenaline.data.entities.AppSettingsEntity
import com.luckierdev.adfreenaline.data.mappers.toEntity
import com.luckierdev.adfreenaline.data.mappers.toEntities

object PrefsMigrator {
    const val MIGRATION_KEY = "prefs_migrated_v1"

    suspend fun migrateIfNeeded(context: Context, db: AppDatabase) {
        val alreadyMigrated = db.metaDao().getValue(MIGRATION_KEY) == "true"
        if (alreadyMigrated) {
            ensureDefaultSettings(db)
            return
        }

        db.withTransaction {
            val runs = LegacyPrefsReader.readRuns(context)
            if (runs.isNotEmpty()) {
                db.runDao().insertAll(runs.map { it.toEntity() })
            }

            val settings = LegacyPrefsReader.readSettings(context)
            val weeklyKey = LegacyPrefsReader.readWeeklyGoalAlertKey(context)
            db.settingsDao().upsert(settings.toEntity(lastWeeklyGoalAlertKey = weeklyKey))

            LegacyPrefsReader.readRoutes(context).forEach { route ->
                val (entity, waypoints) = route.toEntities()
                db.routeDao().replaceRoute(entity, waypoints)
            }

            val challenges = LegacyPrefsReader.readChallenges(context)
            if (challenges.isNotEmpty()) {
                db.challengeDao().insertAll(challenges.map { it.toEntity() })
            }

            db.metaDao().upsert(AppMetaEntity(MIGRATION_KEY, "true"))

            if (LegacyPrefsReader.hasLegacyPrefs(context)) {
                LegacyPrefsReader.clearLegacyPrefs(context)
            }
        }

        ensureDefaultSettings(db)
    }

    private suspend fun ensureDefaultSettings(db: AppDatabase) {
        if (db.settingsDao().get() == null) {
            db.settingsDao().upsert(AppSettingsEntity())
        }
    }
}
