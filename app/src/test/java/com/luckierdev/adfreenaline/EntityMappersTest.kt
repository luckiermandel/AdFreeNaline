package com.luckierdev.adfreenaline

import com.luckierdev.adfreenaline.data.mappers.ActiveRunSnapshot
import com.luckierdev.adfreenaline.data.mappers.deserializePath
import com.luckierdev.adfreenaline.data.mappers.serializePath
import com.luckierdev.adfreenaline.data.mappers.toEntity
import com.luckierdev.adfreenaline.data.mappers.toRecord
import com.luckierdev.adfreenaline.data.mappers.toSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.osmdroid.util.GeoPoint

class EntityMappersTest {

    @Test
    fun runRecord_roundTripsThroughEntity() {
        val original = RunRecord(
            timestampMs = 1_700_000_000_000L,
            durationMs = 1_800_000L,
            distanceMeters = 5000.0,
            avgPaceMinKm = 6.0,
            avgSpeedKmh = 10.0,
            calories = 420,
            countryCode = "GB"
        )
        assertEquals(original, original.toEntity().toRecord())
    }

    @Test
    fun serializePath_roundTripsGeoPoints() {
        val points = listOf(
            GeoPoint(51.5074, -0.1278),
            GeoPoint(51.5080, -0.1285)
        )
        assertEquals(points, deserializePath(serializePath(points)))
    }

    @Test
    fun deserializePath_skipsMalformedSegments() {
        val result = deserializePath("51.5,-0.1;bad;52.0,-0.2")
        assertEquals(2, result.size)
        assertEquals(51.5, result[0].latitude, 0.001)
        assertEquals(52.0, result[1].latitude, 0.001)
    }

    @Test
    fun deserializePath_returnsEmptyForBlankInput() {
        assertEquals(emptyList<GeoPoint>(), deserializePath(""))
        assertEquals(emptyList<GeoPoint>(), deserializePath("   "))
    }

    @Test
    fun activeRunSnapshot_roundTripsThroughEntity() {
        val snapshot = ActiveRunSnapshot(
            isTracking = true,
            isPaused = false,
            trackingStartMs = 1_700_000_000_000L,
            pausedAccumulatedMs = 0L,
            pauseStartMs = 0L,
            stats = RunStats(
                isTracking = true,
                durationMs = 120_000L,
                distanceMeters = 400.0,
                avgPaceMinKm = 5.0,
                avgSpeedKmh = 12.0,
                calories = 55
            ),
            lastLocation = null,
            path = listOf(GeoPoint(51.5, -0.1), GeoPoint(51.51, -0.11))
        )

        val restored = snapshot.toEntity().toSnapshot()
        requireNotNull(restored)
        assertEquals(snapshot.isTracking, restored.isTracking)
        assertEquals(snapshot.isPaused, restored.isPaused)
        assertEquals(snapshot.trackingStartMs, restored.trackingStartMs)
        assertEquals(snapshot.stats.distanceMeters, restored.stats.distanceMeters, 0.001)
        assertEquals(snapshot.stats.calories, restored.stats.calories)
        assertEquals(snapshot.path.size, restored.path.size)
        assertEquals(snapshot.path[0].latitude, restored.path[0].latitude, 0.001)
    }

    @Test
    fun activeSessionEntity_notTracking_returnsNullSnapshot() {
        val entity = ActiveRunSnapshot(
            isTracking = false,
            isPaused = false,
            trackingStartMs = 0L,
            pausedAccumulatedMs = 0L,
            pauseStartMs = 0L,
            stats = RunStats(),
            lastLocation = null,
            path = emptyList()
        ).toEntity().copy(isTracking = false)

        assertNull(entity.toSnapshot())
    }
}
