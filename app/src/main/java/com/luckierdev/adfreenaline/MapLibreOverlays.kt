package com.luckierdev.adfreenaline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.osmdroid.util.GeoPoint

object MapLibreOverlays {
    private const val RUN_PATH_SOURCE = "run-path-source"
    private const val RUN_PATH_LAYER = "run-path-layer"
    private const val CREATOR_PATH_SOURCE = "creator-path-source"
    private const val CREATOR_PATH_LAYER = "creator-path-layer"
    private const val CREATOR_WAYPOINTS_SOURCE = "creator-waypoints-source"
    private const val CREATOR_WAYPOINTS_LAYER = "creator-waypoints-layer"
    private const val USER_LOCATION_SOURCE = "user-location-source"
    private const val USER_LOCATION_LAYER = "user-location-layer"
    private const val USER_MARKER_IMAGE = "user-arrow-marker"
    private const val ESRI_SOURCE_ID = "esri-satellite"
    private const val ESRI_LAYER_ID = "esri-satellite-layer"

    private val emptyFeatureCollection = """{"type":"FeatureCollection","features":[]}"""

    fun install(
        style: org.maplibre.android.maps.Style,
        context: Context,
        satelliteImageryEnabled: Boolean
    ) {
        ensureSatelliteImagery(style, satelliteImageryEnabled)
        if (style.getImage(USER_MARKER_IMAGE) == null) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_red_arrow_marker)
            if (drawable != null) {
                style.addImage(USER_MARKER_IMAGE, drawableToBitmap(drawable))
            }
        }

        ensureSource(style, RUN_PATH_SOURCE)
        ensureSource(style, CREATOR_PATH_SOURCE)
        ensureSource(style, CREATOR_WAYPOINTS_SOURCE)
        ensureSource(style, USER_LOCATION_SOURCE)

        if (style.getLayer(RUN_PATH_LAYER) == null) {
            style.addLayer(
                LineLayer(RUN_PATH_LAYER, RUN_PATH_SOURCE).withProperties(
                    PropertyFactory.lineColor("#FF0000"),
                    PropertyFactory.lineWidth(4f)
                )
            )
        }
        if (style.getLayer(CREATOR_PATH_LAYER) == null) {
            style.addLayer(
                LineLayer(CREATOR_PATH_LAYER, CREATOR_PATH_SOURCE).withProperties(
                    PropertyFactory.lineColor("#3F51B5"),
                    PropertyFactory.lineWidth(3.5f)
                )
            )
        }
        if (style.getLayer(CREATOR_WAYPOINTS_LAYER) == null) {
            style.addLayer(
                CircleLayer(CREATOR_WAYPOINTS_LAYER, CREATOR_WAYPOINTS_SOURCE).withProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleColor("#3F51B5"),
                    PropertyFactory.circleStrokeWidth(1.5f),
                    PropertyFactory.circleStrokeColor("#FFFFFF")
                )
            )
        }
        if (style.getLayer(USER_LOCATION_LAYER) == null) {
            style.addLayer(
                SymbolLayer(USER_LOCATION_LAYER, USER_LOCATION_SOURCE).withProperties(
                    PropertyFactory.iconImage(USER_MARKER_IMAGE),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconRotate(org.maplibre.android.style.expressions.Expression.get("bearing")),
                    PropertyFactory.iconSize(1f)
                )
            )
        }
    }

    fun setSatelliteImageryEnabled(style: org.maplibre.android.maps.Style, enabled: Boolean) {
        ensureSatelliteImagery(style, enabled)
    }

    private fun ensureSatelliteImagery(style: org.maplibre.android.maps.Style, enabled: Boolean) {
        if (style.getSource(ESRI_SOURCE_ID) == null) {
            val tileSet = TileSet("tileset", MapTileSources.ESRI_WORLD_IMAGERY_TILE_URL)
            style.addSource(RasterSource(ESRI_SOURCE_ID, tileSet, 256))
        }
        if (style.getLayer(ESRI_LAYER_ID) == null) {
            style.addLayer(RasterLayer(ESRI_LAYER_ID, ESRI_SOURCE_ID))
        }
        style.getLayer(ESRI_LAYER_ID)?.setProperties(
            PropertyFactory.visibility(if (enabled) Property.VISIBLE else Property.NONE)
        )
    }

    fun update(
        style: org.maplibre.android.maps.Style,
        runPath: List<GeoPoint>,
        creatorPath: List<GeoPoint>,
        creatorRouteColor: Int,
        currentLocation: GeoPoint?,
        currentBearing: Float
    ) {
        (style.getSource(RUN_PATH_SOURCE) as? GeoJsonSource)?.setGeoJson(
            lineFeatureCollection(runPath)
        )
        (style.getSource(CREATOR_PATH_SOURCE) as? GeoJsonSource)?.setGeoJson(
            lineFeatureCollection(creatorPath)
        )
        (style.getLayer(CREATOR_PATH_LAYER) as? LineLayer)?.setProperties(
            PropertyFactory.lineColor(colorToHex(creatorRouteColor))
        )
        (style.getLayer(CREATOR_WAYPOINTS_LAYER) as? CircleLayer)?.setProperties(
            PropertyFactory.circleColor(colorToHex(creatorRouteColor))
        )
        (style.getSource(CREATOR_WAYPOINTS_SOURCE) as? GeoJsonSource)?.setGeoJson(
            pointFeatureCollection(creatorPath)
        )
        (style.getSource(USER_LOCATION_SOURCE) as? GeoJsonSource)?.setGeoJson(
            userLocationFeature(currentLocation, currentBearing)
        )
    }

    private fun ensureSource(style: org.maplibre.android.maps.Style, sourceId: String) {
        if (style.getSource(sourceId) == null) {
            style.addSource(GeoJsonSource(sourceId, emptyFeatureCollection))
        }
    }

    private fun lineFeatureCollection(points: List<GeoPoint>): String {
        if (points.size < 2) return emptyFeatureCollection
        val coordinates = points.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coordinates]},"properties":{}}]}"""
    }

    private fun pointFeatureCollection(points: List<GeoPoint>): String {
        if (points.isEmpty()) return emptyFeatureCollection
        val features = points.mapIndexed { index, point ->
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]},"properties":{"index":"${index + 1}"}}"""
        }
        return """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""
    }

    private fun userLocationFeature(location: GeoPoint?, bearing: Float): String {
        if (location == null) return emptyFeatureCollection
        return """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[${location.longitude},${location.latitude}]},"properties":{"bearing":$bearing}}]}"""
    }

    private fun colorToHex(color: Int): String =
        String.format("#%06X", 0xFFFFFF and color)

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
