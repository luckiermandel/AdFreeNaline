package com.luckierdev.adfreenaline.ui.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.RouteMode
import com.luckierdev.adfreenaline.ui.RouteDraft
import com.luckierdev.adfreenaline.ui.map.MapPanel
import com.luckierdev.adfreenaline.ui.theme.Dimens
import org.osmdroid.util.GeoPoint

@Composable
fun ColumnScope.CreatorScreen(
    creatorPath: List<GeoPoint>,
    runPath: List<GeoPoint>,
    currentLocation: GeoPoint?,
    currentBearing: Float,
    hasPermission: Boolean,
    permissionBlocked: Boolean,
    requestPermission: () -> Unit,
    routeColor: Int,
    darkMapStyleEnabled: Boolean,
    satelliteImageryEnabled: Boolean,
    editingRoute: RouteDraft?,
    onAddPoint: (GeoPoint) -> Unit,
    onSetPoint: (Int, GeoPoint) -> Unit,
    onDeletePoint: (Int) -> Unit,
    onSaveRoute: (name: String, category: String, color: Int, mode: RouteMode, existingId: Long?) -> Unit,
    onClear: () -> Unit
) {
    var selectedWaypoint by rememberSaveable { mutableStateOf(-1) }
    val defaultName = stringResource(R.string.default_route_name)
    val defaultCategory = stringResource(R.string.default_route_category)
    var routeName by rememberSaveable(editingRoute?.id) { mutableStateOf(editingRoute?.name ?: "") }
    var routeCategory by rememberSaveable(editingRoute?.id) {
        mutableStateOf(editingRoute?.category ?: defaultCategory)
    }
    val creatorScroll = rememberScrollState()
    MapPanel(
        runPath = runPath,
        creatorPath = creatorPath,
        currentLocation = currentLocation,
        currentBearing = currentBearing,
        hasPermission = hasPermission,
        permissionBlocked = permissionBlocked,
        requestPermission = requestPermission,
        creatorMode = true,
        routeColor = routeColor,
        darkMapStyleEnabled = darkMapStyleEnabled,
        satelliteImageryEnabled = satelliteImageryEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.CreatorMapHeight),
        onTap = { p ->
            if (selectedWaypoint >= 0) {
                onSetPoint(selectedWaypoint, p)
                selectedWaypoint = -1
            } else {
                onAddPoint(p)
            }
        }
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(creatorScroll),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
            OutlinedTextField(
                value = routeName,
                onValueChange = { routeName = it },
                label = { Text(stringResource(R.string.label_route_name)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = routeCategory,
                onValueChange = { routeCategory = it },
                label = { Text(stringResource(R.string.label_category)) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
            Button(
                onClick = {
                    onSaveRoute(
                        routeName.ifBlank { defaultName },
                        routeCategory.ifBlank { defaultCategory },
                        routeColor,
                        RouteMode.STRAIGHT,
                        editingRoute?.id
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = creatorPath.size > 1
            ) {
                Text(
                    stringResource(
                        if (editingRoute == null) R.string.action_save else R.string.action_update
                    )
                )
            }
            Button(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_clear))
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
            ) {
                Text(stringResource(R.string.label_waypoints), style = MaterialTheme.typography.titleSmall)
                creatorPath.forEachIndexed { i, _ ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.waypoint_item, i + 1))
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                            TextButton(onClick = { selectedWaypoint = i }) {
                                Text(stringResource(R.string.action_move))
                            }
                            TextButton(onClick = { onDeletePoint(i) }) {
                                Text(
                                    stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
