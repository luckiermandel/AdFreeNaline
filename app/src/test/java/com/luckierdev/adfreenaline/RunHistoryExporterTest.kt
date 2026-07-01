package com.luckierdev.adfreenaline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHistoryExporterTest {

    @Test
    fun toCsv_includesHeaderAndSortedRows() {
        val records = listOf(
            RunRecord(
                timestampMs = 2L,
                durationMs = 120_000L,
                distanceMeters = 1000.0,
                avgPaceMinKm = 6.0,
                avgSpeedKmh = 10.0,
                calories = 80,
                countryCode = "US"
            ),
            RunRecord(
                timestampMs = 1L,
                durationMs = 60_000L,
                distanceMeters = 500.0,
                avgPaceMinKm = 5.0,
                avgSpeedKmh = 12.0,
                calories = 40,
                countryCode = "GB"
            )
        )

        val csv = RunHistoryExporter.toCsv(records)
        val lines = csv.lines()

        assertEquals(
            "timestamp_iso,duration_sec,distance_m,pace_min_per_km,speed_kmh,calories,country_code",
            lines.first()
        )
        assertEquals(3, lines.size)
        assertTrue(lines[1].endsWith(",GB"))
        assertTrue(lines[2].endsWith(",US"))
    }

    @Test
    fun toCsv_emptyHistory_returnsHeaderOnly() {
        val csv = RunHistoryExporter.toCsv(emptyList())
        assertEquals(
            "timestamp_iso,duration_sec,distance_m,pace_min_per_km,speed_kmh,calories,country_code",
            csv
        )
    }
}
