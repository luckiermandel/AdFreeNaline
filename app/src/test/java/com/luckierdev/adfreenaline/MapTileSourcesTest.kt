package com.luckierdev.adfreenaline



import org.junit.Assert.assertEquals

import org.junit.Test



class MapTileSourcesTest {



    @Test

    fun styleUrl_returnsLibertyByDefault() {

        assertEquals(

            "https://tiles.openfreemap.org/styles/liberty",

            MapTileSources.styleUrl(darkMapStyleEnabled = false)

        )

    }



    @Test

    fun styleUrl_returnsDarkWhenEnabled() {

        assertEquals(

            "https://tiles.openfreemap.org/styles/dark",

            MapTileSources.styleUrl(darkMapStyleEnabled = true)

        )

    }



    @Test

    fun esriTileUrl_usesZyxTemplate() {

        assertEquals(

            "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",

            MapTileSources.ESRI_WORLD_IMAGERY_TILE_URL

        )

    }

}

