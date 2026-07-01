package com.luckierdev.adfreenaline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.luckierdev.adfreenaline.data.dao.ActiveSessionDao
import com.luckierdev.adfreenaline.data.dao.ChallengeDao
import com.luckierdev.adfreenaline.data.dao.MetaDao
import com.luckierdev.adfreenaline.data.dao.RouteDao
import com.luckierdev.adfreenaline.data.dao.RunDao
import com.luckierdev.adfreenaline.data.dao.SettingsDao
import com.luckierdev.adfreenaline.data.entities.ActiveSessionEntity
import com.luckierdev.adfreenaline.data.entities.AppMetaEntity
import com.luckierdev.adfreenaline.data.entities.AppSettingsEntity
import com.luckierdev.adfreenaline.data.entities.ChallengeEntity
import com.luckierdev.adfreenaline.data.entities.RouteEntity
import com.luckierdev.adfreenaline.data.entities.RouteWaypointEntity
import com.luckierdev.adfreenaline.data.entities.RunEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        RunEntity::class,
        RouteEntity::class,
        RouteWaypointEntity::class,
        ChallengeEntity::class,
        AppSettingsEntity::class,
        ActiveSessionEntity::class,
        AppMetaEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun routeDao(): RouteDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun settingsDao(): SettingsDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun metaDao(): MetaDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN satelliteImageryEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val initMutex = Mutex()

        @Volatile
        private var instance: AppDatabase? = null

        suspend fun getInstance(context: Context): AppDatabase {
            return instance ?: initMutex.withLock {
                instance ?: buildDatabase(context.applicationContext).also { db ->
                    PrefsMigrator.migrateIfNeeded(context.applicationContext, db)
                    instance = db
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "adfreenaline.db"
            ).addMigrations(MIGRATION_1_2).build()
        }
    }
}
