package com.luckierdev.adfreenaline

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object RunHistoryExporter {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun toCsv(records: List<RunRecord>): String {
        val header = "timestamp_iso,duration_sec,distance_m,pace_min_per_km,speed_kmh,calories,country_code"
        val rows = records.sortedBy { it.timestampMs }.map { record ->
            listOf(
                csvField(isoFormat.format(Date(record.timestampMs))),
                csvField((record.durationMs / 1000.0).toString()),
                csvField(record.distanceMeters.toString()),
                csvField(record.avgPaceMinKm.toString()),
                csvField(record.avgSpeedKmh.toString()),
                csvField(record.calories.toString()),
                csvField(record.countryCode)
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csvField(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
