package com.luckierdev.adfreenaline.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luckierdev.adfreenaline.MapLibreOverlays
import com.luckierdev.adfreenaline.MapTileSources
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.ui.theme.Dimens
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.osmdroid.util.GeoPoint

@Composable
fun MapPanel(
    runPath: List<GeoPoint>,
    creatorPath: List<GeoPoint>,
    currentLocation: GeoPoint?,
    currentBearing: Float,
    hasPermission: Boolean,
    permissionBlocked: Boolean,
    requestPermission: () -> Unit,
    creatorMode: Boolean,
    routeColor: Int,
    darkMapStyleEnabled: Boolean,
    satelliteImageryEnabled: Boolean,
    modifier: Modifier = Modifier,
    onTap: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var mapInitialized by remember { mutableStateOf(false) }
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }
    val targetStyleUrl = MapTileSources.styleUrl(darkMapStyleEnabled)
    var savedCameraLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedCameraLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedCameraZoom by rememberSaveable { mutableStateOf<Double?>(null) }
    var centered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasPermission) {
        if (hasPermission) centered = false
    }

    fun captureCamera(map: MapLibreMap) {
        val position = map.cameraPosition ?: return
        val target = position.target ?: return
        savedCameraLat = target.latitude
        savedCameraLon = target.longitude
        savedCameraZoom = position.zoom
    }

    fun restoreCamera(map: MapLibreMap): Boolean {
        val lat = savedCameraLat ?: return false
        val lon = savedCameraLon ?: return false
        val zoom = savedCameraZoom ?: return false
        map.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(lat, lon))
                    .zoom(zoom)
                    .build()
            )
        )
        return true
    }

    fun centerOnLocation(map: MapLibreMap, resetZoom: Boolean = false) {
        if (!hasPermission || currentLocation == null) return
        val zoom = if (resetZoom) {
            MapTileSources.DEFAULT_ZOOM
        } else {
            map.cameraPosition?.zoom?.takeIf { it > 0 } ?: MapTileSources.DEFAULT_ZOOM
        }
        map.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(currentLocation.latitude, currentLocation.longitude))
                    .zoom(zoom)
                    .build()
            )
        )
    }

    fun centerOnLocationIfNeeded(map: MapLibreMap) {
        if (centered) return
        if (!hasPermission || currentLocation == null) return
        centerOnLocation(map, resetZoom = true)
        centered = true
    }

    fun finishStyleLoad(map: MapLibreMap, styleUrl: String) {
        val style = map.style ?: return
        MapLibreOverlays.install(style, context, satelliteImageryEnabled)
        MapLibreOverlays.update(
            style,
            runPath,
            creatorPath,
            routeColor,
            currentLocation,
            currentBearing
        )
        appliedStyleUrl = styleUrl
        styleReady = true
        if (!restoreCamera(map)) {
            centerOnLocationIfNeeded(map)
        } else {
            centered = true
        }
    }
    var attributionCycle by remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        attributionCycle++
        onDispose {}
    }
    val attributionText = if (satelliteImageryEnabled) {
        stringResource(R.string.map_attribution_esri)
    } else {
        stringResource(R.string.map_attribution_openfreemap)
    }
    val attributionUrl = if (satelliteImageryEnabled) {
        stringResource(R.string.url_esri_attribution)
    } else {
        stringResource(R.string.url_openfreemap)
    }
    var attributionVisible by remember { mutableStateOf(true) }
    LaunchedEffect(attributionCycle, satelliteImageryEnabled) {
        attributionVisible = true
        delay(5_000)
        attributionVisible = false
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val map = mapLibreMap
            if (map != null && styleReady) {
                captureCamera(map)
            }
        }
    }

    fun applyStyle(map: MapLibreMap, styleUrl: String) {
        if (styleUrl == appliedStyleUrl) return
        if (map.style != null && styleReady) {
            captureCamera(map)
        }
        styleReady = false
        map.setStyle(styleUrl) { finishStyleLoad(map, styleUrl) }
    }

    val mapClickListener = remember(onTap) {
        MapLibreMap.OnMapClickListener { point ->
            onTap(GeoPoint(point.latitude, point.longitude))
            true
        }
    }

    LaunchedEffect(mapView) {
        if (mapInitialized) return@LaunchedEffect
        mapInitialized = true
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isLogoEnabled = false
            map.setMaxZoomPreference(MapTileSources.MAX_ZOOM)
        }
    }

    LaunchedEffect(satelliteImageryEnabled, styleReady, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        MapLibreOverlays.setSatelliteImageryEnabled(style, satelliteImageryEnabled)
    }

    LaunchedEffect(targetStyleUrl, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        applyStyle(map, targetStyleUrl)
    }

    LaunchedEffect(currentLocation, hasPermission, styleReady, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        if (savedCameraLat != null) return@LaunchedEffect
        centerOnLocationIfNeeded(map)
    }

    LaunchedEffect(creatorMode, mapLibreMap, mapClickListener, styleReady) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        map.removeOnMapClickListener(mapClickListener)
        if (creatorMode) {
            map.addOnMapClickListener(mapClickListener)
        }
    }

    val chipBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val chipContent = MaterialTheme.colorScheme.onSurface

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardCorner)) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = {
                    val map = mapLibreMap ?: return@AndroidView
                    if (targetStyleUrl != appliedStyleUrl) {
                        applyStyle(map, targetStyleUrl)
                        return@AndroidView
                    }
                    val style = map.style ?: return@AndroidView
                    if (!styleReady) return@AndroidView
                    MapLibreOverlays.setSatelliteImageryEnabled(style, satelliteImageryEnabled)
                    MapLibreOverlays.update(
                        style,
                        runPath,
                        creatorPath,
                        routeColor,
                        currentLocation,
                        currentBearing
                    )
                }
            )
            if (!styleReady) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            IconButton(
                onClick = {
                    if (!hasPermission) {
                        requestPermission()
                    } else {
                        mapLibreMap?.let { centerOnLocation(it) }
                    }
                },
                enabled = hasPermission && currentLocation != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.SpacingSm)
                    .background(chipBackground, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = stringResource(R.string.center_map),
                    tint = if (hasPermission && currentLocation != null) {
                        chipContent
                    } else {
                        chipContent.copy(alpha = 0.5f)
                    }
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = attributionVisible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = attributionText,
                    modifier = Modifier
                        .background(chipBackground, RoundedCornerShape(4.dp))
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(attributionUrl))
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = chipContent
                )
            }
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { requestPermission() }
                        .padding(Dimens.CardPadding)
                ) {
                    Text(
                        stringResource(
                            if (permissionBlocked) {
                                R.string.permission_denied_overlay
                            } else {
                                R.string.permission_missing_overlay
                            }
                        ),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}