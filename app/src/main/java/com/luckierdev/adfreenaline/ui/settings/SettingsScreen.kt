package com.luckierdev.adfreenaline.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.GoalAlertNotifier
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RunRecord
import com.luckierdev.adfreenaline.RunSettings
import com.luckierdev.adfreenaline.ThemeMode
import com.luckierdev.adfreenaline.ui.components.DeferredCommitTextField
import com.luckierdev.adfreenaline.ui.components.GoalPresetChip
import com.luckierdev.adfreenaline.ui.components.SettingToggle
import com.luckierdev.adfreenaline.ui.components.SettingsDoubleField
import com.luckierdev.adfreenaline.ui.components.SettingsIntField
import com.luckierdev.adfreenaline.ui.format.MI_PER_KM
import com.luckierdev.adfreenaline.ui.theme.Dimens
import com.luckierdev.adfreenaline.ui.theme.resolveDarkTheme
import java.util.Locale

private const val MIN_GOAL_KM = 0.0009 // 0.9 meters
private const val MAX_GOAL_KM = 10_000.0

private fun displayGoalValue(km: Double, unit: DistanceUnit): String {
    val value = if (unit == DistanceUnit.KM) km else km * MI_PER_KM
    val decimals = if (value < 0.01) 4 else 2
    return String.format(Locale.US, "%.${decimals}f", value)
}

private fun parseGoalInput(input: String, unit: DistanceUnit): Double? {
    val value = input.toDoubleOrNull() ?: return null
    return if (unit == DistanceUnit.KM) value else value / MI_PER_KM
}

private fun defaultGoalKm(unit: DistanceUnit): Double = parseGoalInput("1", unit)!!

private fun snapGoalKm(input: String, unit: DistanceUnit): Double {
    val parsed = parseGoalInput(input, unit) ?: return MIN_GOAL_KM
    return parsed.coerceIn(MIN_GOAL_KM, MAX_GOAL_KM)
}

@Composable
fun ColumnScope.SettingsScreen(
    settings: RunSettings,
    settingsResetVersion: Int,
    history: List<RunRecord>,
    onUpdate: ((RunSettings) -> RunSettings) -> Unit,
    onDeleteAllAppData: () -> Unit,
    onExportRuns: () -> Unit,
    onPickGoalSound: () -> Unit,
    onPreviewGoalSound: () -> Unit
) {
    val context = LocalContext.current
    var perRunGoalText by rememberSaveable(settingsResetVersion, settings.distanceUnit) {
        val km = if (settings.perRunDistanceGoalKm > 0) {
            settings.perRunDistanceGoalKm
        } else {
            defaultGoalKm(settings.distanceUnit)
        }
        mutableStateOf(displayGoalValue(km, settings.distanceUnit))
    }
    var weeklyGoalText by rememberSaveable(settingsResetVersion, settings.distanceUnit) {
        val km = if (settings.weeklyDistanceGoalKm > 0) {
            settings.weeklyDistanceGoalKm
        } else {
            defaultGoalKm(settings.distanceUnit)
        }
        mutableStateOf(displayGoalValue(km, settings.distanceUnit))
    }
    var perRunGoalFocused by remember { mutableStateOf(false) }
    var weeklyGoalFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(settingsResetVersion) {
        perRunGoalFocused = false
        weeklyGoalFocused = false
        focusManager.clearFocus()
    }
    LaunchedEffect(settings.perRunDistanceGoalKm, settings.distanceUnit) {
        if (!perRunGoalFocused && settings.perRunDistanceGoalKm > 0) {
            perRunGoalText = displayGoalValue(settings.perRunDistanceGoalKm, settings.distanceUnit)
        }
    }
    LaunchedEffect(settings.weeklyDistanceGoalKm, settings.distanceUnit) {
        if (!weeklyGoalFocused && settings.weeklyDistanceGoalKm > 0) {
            weeklyGoalText = displayGoalValue(settings.weeklyDistanceGoalKm, settings.distanceUnit)
        }
    }
    var showDeleteAllConfirm by rememberSaveable { mutableStateOf(false) }
    val perRunEnabled = settings.perRunDistanceGoalKm > 0
    val weeklyEnabled = settings.weeklyDistanceGoalKm > 0
    val soundLabel = remember(settings.goalAlertMuted, settings.goalAlertSoundUri) {
        GoalAlertNotifier(context).soundLabel(settings.goalAlertMuted, settings.goalAlertSoundUri)
    }
    val unitLabel = stringResource(
        if (settings.distanceUnit == DistanceUnit.KM) R.string.unit_km else R.string.unit_mi
    )
    val applyPerRunGoal = {
        val km = snapGoalKm(perRunGoalText, settings.distanceUnit)
        perRunGoalText = displayGoalValue(km, settings.distanceUnit)
        onUpdate { it.copy(perRunDistanceGoalKm = km) }
    }
    val applyWeeklyGoal = {
        val km = snapGoalKm(weeklyGoalText, settings.distanceUnit)
        weeklyGoalText = displayGoalValue(km, settings.distanceUnit)
        onUpdate { it.copy(weeklyDistanceGoalKm = km) }
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(stringResource(R.string.label_distance_goals), style = MaterialTheme.typography.titleMedium)
                SettingToggle(stringResource(R.string.settings_per_run_goal_toggle), perRunEnabled) {
                    if (perRunEnabled) {
                        onUpdate { it.copy(perRunDistanceGoalKm = 0.0) }
                    } else {
                        val km = snapGoalKm(perRunGoalText, settings.distanceUnit)
                        perRunGoalText = displayGoalValue(km, settings.distanceUnit)
                        onUpdate { it.copy(perRunDistanceGoalKm = km) }
                    }
                }
                if (perRunEnabled) {
                    DeferredCommitTextField(
                        value = perRunGoalText,
                        onValueChange = { perRunGoalText = it },
                        label = { Text(stringResource(R.string.settings_per_run_goal_field, unitLabel)) },
                        keyboardType = KeyboardType.Decimal,
                        onApply = applyPerRunGoal,
                        onFocusedChange = { perRunGoalFocused = it },
                        settingsResetVersion = settingsResetVersion
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                        GoalPresetChip("5K") {
                            perRunGoalText = displayGoalValue(5.0, settings.distanceUnit)
                            onUpdate { it.copy(perRunDistanceGoalKm = 5.0) }
                        }
                        GoalPresetChip("10K") {
                            perRunGoalText = displayGoalValue(10.0, settings.distanceUnit)
                            onUpdate { it.copy(perRunDistanceGoalKm = 10.0) }
                        }
                    }
                }
                SettingToggle(stringResource(R.string.settings_weekly_goal_toggle), weeklyEnabled) {
                    if (weeklyEnabled) {
                        onUpdate { it.copy(weeklyDistanceGoalKm = 0.0) }
                    } else {
                        val km = snapGoalKm(weeklyGoalText, settings.distanceUnit)
                        weeklyGoalText = displayGoalValue(km, settings.distanceUnit)
                        onUpdate { it.copy(weeklyDistanceGoalKm = km) }
                    }
                }
                if (weeklyEnabled) {
                    DeferredCommitTextField(
                        value = weeklyGoalText,
                        onValueChange = { weeklyGoalText = it },
                        label = { Text(stringResource(R.string.settings_weekly_goal_field, unitLabel)) },
                        keyboardType = KeyboardType.Decimal,
                        onApply = applyWeeklyGoal,
                        onFocusedChange = { weeklyGoalFocused = it },
                        settingsResetVersion = settingsResetVersion
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                        GoalPresetChip("20K") {
                            weeklyGoalText = displayGoalValue(20.0, settings.distanceUnit)
                            onUpdate { it.copy(weeklyDistanceGoalKm = 20.0) }
                        }
                        GoalPresetChip("50K") {
                            weeklyGoalText = displayGoalValue(50.0, settings.distanceUnit)
                            onUpdate { it.copy(weeklyDistanceGoalKm = 50.0) }
                        }
                        GoalPresetChip("100K") {
                            weeklyGoalText = displayGoalValue(100.0, settings.distanceUnit)
                            onUpdate { it.copy(weeklyDistanceGoalKm = 100.0) }
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(stringResource(R.string.settings_goal_alerts_title), style = MaterialTheme.typography.titleMedium)
                SettingToggle(stringResource(R.string.settings_mute_goal_sounds), settings.goalAlertMuted) {
                    onUpdate { it.copy(goalAlertMuted = !it.goalAlertMuted) }
                }
                Text(stringResource(R.string.settings_sound_label, soundLabel))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    Button(onClick = onPickGoalSound, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_choose_sound))
                    }
                    Button(
                        onClick = onPreviewGoalSound,
                        modifier = Modifier.weight(1f),
                        enabled = !settings.goalAlertMuted
                    ) { Text(stringResource(R.string.settings_preview)) }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleMedium)
                ThemeModeSelector(settings.themeMode) { mode ->
                    onUpdate { it.copy(themeMode = mode) }
                }
                SettingToggle(
                    stringResource(R.string.settings_use_miles),
                    settings.distanceUnit == DistanceUnit.MI
                ) {
                    onUpdate {
                        it.copy(
                            distanceUnit = if (it.distanceUnit == DistanceUnit.KM) DistanceUnit.MI else DistanceUnit.KM
                        )
                    }
                }
                SettingToggle(stringResource(R.string.settings_battery_saver), settings.batterySaver) {
                    onUpdate { it.copy(batterySaver = !it.batterySaver) }
                }
                SettingToggle(stringResource(R.string.setting_dark_map_style), settings.darkMapStyleEnabled) {
                    onUpdate { it.copy(darkMapStyleEnabled = !it.darkMapStyleEnabled) }
                }
                SettingToggle(stringResource(R.string.setting_satellite_imagery), settings.satelliteImageryEnabled) {
                    onUpdate { it.copy(satelliteImageryEnabled = !it.satelliteImageryEnabled) }
                }
                SettingToggle(stringResource(R.string.settings_reminders), settings.remindersEnabled) {
                    onUpdate { it.copy(remindersEnabled = !it.remindersEnabled) }
                }
                Text(stringResource(R.string.settings_creator_color))
                val darkTheme = resolveDarkTheme(settings.themeMode)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    ColorChoice(
                        if (darkTheme) stringResource(R.string.color_black) else stringResource(R.string.color_purple),
                        if (darkTheme) 0xFF000000.toInt() else 0xFF7E57C2.toInt(),
                        settings.creatorRouteColor,
                        onUpdate
                    )
                    ColorChoice(stringResource(R.string.color_teal), 0xFF00897B.toInt(), settings.creatorRouteColor, onUpdate)
                    ColorChoice(stringResource(R.string.color_orange), 0xFFEF6C00.toInt(), settings.creatorRouteColor, onUpdate)
                }
                SettingsIntField(
                    label = stringResource(R.string.label_age),
                    value = settings.age,
                    onValueChange = { onUpdate { s -> s.copy(age = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                SettingsIntField(
                    label = stringResource(R.string.settings_height_cm),
                    value = settings.heightCm,
                    onValueChange = { onUpdate { s -> s.copy(heightCm = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                SettingsDoubleField(
                    label = stringResource(R.string.settings_weight_kg),
                    value = settings.weightKg,
                    onValueChange = { onUpdate { s -> s.copy(weightKg = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                SettingsIntField(
                    label = stringResource(R.string.settings_calorie_goal),
                    value = settings.calorieGoalPerRun,
                    onValueChange = { onUpdate { s -> s.copy(calorieGoalPerRun = it) } },
                    settingsResetVersion = settingsResetVersion
                )
                Text(
                    stringResource(R.string.calorie_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DataLicensesCard()
        OutlinedButton(
            onClick = onExportRuns,
            enabled = history.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_export_csv))
        }
        if (history.isEmpty()) {
            Text(stringResource(R.string.msg_no_runs_to_export), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { showDeleteAllConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(stringResource(R.string.settings_delete_all))
        }
    }
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllAppData()
                        showDeleteAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.delete_all_confirm)) }
            },
            dismissButton = {
                Button(onClick = { showDeleteAllConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ThemeModeSelector(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun ColorChoice(
    label: String,
    color: Int,
    selectedColor: Int,
    onUpdate: ((RunSettings) -> RunSettings) -> Unit
) {
    Button(
        onClick = { onUpdate { it.copy(creatorRouteColor = color) } },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedColor == color) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selectedColor == color) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) { Text(label) }
}

@Composable
private fun DataLicensesCard() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            Text(stringResource(R.string.settings_data_licenses_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_map_data_body),
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.settings_osm_attribution_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_osm_attribution_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_osm_copyright)))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_license_page))
            }
            Text(stringResource(R.string.settings_openfreemap_attribution_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_openfreemap_attribution_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_openfreemap)))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_license_page))
            }
            Text(stringResource(R.string.settings_esri_attribution_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_esri_attribution_body),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_esri_attribution)))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_license_page))
            }
        }
    }
}
