package com.luckierdev.adfreenaline.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.R
import java.util.Locale

const val MI_PER_KM = 0.621371

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

fun kmToDisplay(km: Double, unit: DistanceUnit): Double =
    if (unit == DistanceUnit.KM) km else km * MI_PER_KM

fun speedToDisplay(kmh: Double, unit: DistanceUnit): Double =
    if (unit == DistanceUnit.KM) kmh else kmh * MI_PER_KM

fun paceToDisplay(minPerKm: Double, unit: DistanceUnit): Double =
    if (unit == DistanceUnit.KM) minPerKm else minPerKm / MI_PER_KM

@Composable
fun distanceUnitLabel(unit: DistanceUnit): String =
    stringResource(if (unit == DistanceUnit.KM) R.string.unit_km else R.string.unit_mi)

@Composable
fun speedUnitLabel(unit: DistanceUnit): String =
    stringResource(if (unit == DistanceUnit.KM) R.string.unit_kmh else R.string.unit_mph)

@Composable
fun paceUnitLabel(unit: DistanceUnit): String =
    stringResource(if (unit == DistanceUnit.KM) R.string.unit_min_km else R.string.unit_min_mi)

@Composable
fun formatDistance(meters: Double, unit: DistanceUnit, decimals: Int = 2): String {
    val value = kmToDisplay(meters / 1000.0, unit)
    return "${String.format(Locale.US, "%.${decimals}f", value)} ${distanceUnitLabel(unit)}"
}

@Composable
fun formatSpeed(kmh: Double, unit: DistanceUnit, decimals: Int = 2): String {
    val value = speedToDisplay(kmh, unit)
    return "${String.format(Locale.US, "%.${decimals}f", value)} ${speedUnitLabel(unit)}"
}
