package com.luckierdev.adfreenaline

object MapTileSources {
    /** Short on-map label; full acknowledgment lives in Settings. */
    const val OPENFREEMAP_ATTRIBUTION = "© OpenFreeMap © OpenMapTiles © OSM"
    const val ESRI_ATTRIBUTION = "© Esri, Maxar, Earthstar Geographics"

    const val OPENFREEMAP_URL = "https://openfreemap.org"
    const val ESRI_ATTRIBUTION_URL =
        "https://www.esri.com/en-us/legal/terms/full-master-agreement"

    const val STYLE_LIBERTY = "https://tiles.openfreemap.org/styles/liberty"
    const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

    const val ESRI_WORLD_IMAGERY_TILE_URL =
        "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

    const val DEFAULT_ZOOM = 17.5
    const val MAX_ZOOM = 19.5

    fun styleUrl(darkMapStyleEnabled: Boolean): String =
        if (darkMapStyleEnabled) STYLE_DARK else STYLE_LIBERTY
}
