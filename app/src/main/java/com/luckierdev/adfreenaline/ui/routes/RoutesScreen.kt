package com.luckierdev.adfreenaline.ui.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.luckierdev.adfreenaline.DistanceUnit
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.SavedRoute
import com.luckierdev.adfreenaline.ui.components.EmptyState
import com.luckierdev.adfreenaline.ui.format.formatDistance
import com.luckierdev.adfreenaline.ui.theme.AdFreenalineTheme
import com.luckierdev.adfreenaline.ui.theme.Dimens

@Composable
fun ColumnScope.RoutesScreen(
    routes: List<SavedRoute>,
    unit: DistanceUnit,
    onApply: (SavedRoute) -> Unit,
    onEdit: (SavedRoute) -> Unit,
    onDelete: (SavedRoute) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = routes.map { it.category }.distinct().sorted()
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        if (selectedCategory != null) {
            Button(onClick = { selectedCategory = null }) {
                Text(stringResource(R.string.back_to_categories))
            }
            routes.filter { it.category == selectedCategory }.forEach { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApply(route) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.CardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(route.name)
                            val routeMeters = route.waypoints
                                .zipWithNext { a, b -> a.distanceToAsDouble(b) }
                                .sum()
                            Text(
                                formatDistance(routeMeters, unit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { onEdit(route) }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.cd_edit_route),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDelete(route) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.cd_delete_route),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(stringResource(R.string.route_categories), style = MaterialTheme.typography.titleMedium)
            categories.forEach { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategory = cat }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingSm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat, modifier = Modifier.padding(horizontal = Dimens.SpacingXs))
                        IconButton(onClick = {
                            onDeleteCategory(cat)
                            if (selectedCategory == cat) selectedCategory = null
                        }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.cd_delete_category),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            if (categories.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Map,
                    title = stringResource(R.string.no_saved_routes_title),
                    hint = stringResource(R.string.no_saved_routes_hint)
                )
            }
        }
    }
}

@Preview(name = "Routes empty", showBackground = true)
@Composable
private fun RoutesScreenEmptyPreview() {
    AdFreenalineTheme {
        Column {
            RoutesScreen(
                routes = emptyList(),
                unit = DistanceUnit.KM,
                onApply = {},
                onEdit = {},
                onDelete = {},
                onDeleteCategory = {}
            )
        }
    }
}
