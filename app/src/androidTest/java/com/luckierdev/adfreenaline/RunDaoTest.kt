package com.luckierdev.adfreenaline

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luckierdev.adfreenaline.data.AppDatabase
import com.luckierdev.adfreenaline.data.entities.RunEntity
import com.luckierdev.adfreenaline.data.mappers.toEntity
import com.luckierdev.adfreenaline.data.mappers.toRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserve_roundTripsRunRecord() = runBlocking {
        val record = RunRecord(
            timestampMs = 1_700_000_000_000L,
            durationMs = 1_800_000L,
            distanceMeters = 5000.0,
            avgPaceMinKm = 6.0,
            avgSpeedKmh = 10.0,
            calories = 420,
            countryCode = "GB"
        )

        db.runDao().insert(record.toEntity())
        val stored = db.runDao().observeAll().first().single()

        assertEquals(record, stored.toRecord())
    }

    @Test
    fun observeAll_ordersByTimestampDescending() = runBlocking {
        val older = RunEntity(
            timestampMs = 1L,
            durationMs = 60_000L,
            distanceMeters = 500.0,
            avgPaceMinKm = 5.0,
            avgSpeedKmh = 10.0,
            calories = 40,
            countryCode = "GB"
        )
        val newer = older.copy(timestampMs = 2L, distanceMeters = 1000.0)

        db.runDao().insertAll(listOf(older, newer))
        val results = db.runDao().observeAll().first()

        assertEquals(2L, results[0].timestampMs)
        assertEquals(1L, results[1].timestampMs)
    }
}
