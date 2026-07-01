package com.luckierdev.adfreenaline

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luckierdev.adfreenaline.data.LegacyPrefsReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyPrefsReaderTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun clearPrefs() {
        context.getSharedPreferences("run_history", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun readRuns_parsesValidLines() {
        val valid = "1700000000000|1800000|5000.0|6.0|10.0|420|GB"
        context.getSharedPreferences("run_history", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("records", valid)
            .commit()

        val runs = LegacyPrefsReader.readRuns(context)
        assertEquals(1, runs.size)
        assertEquals(5000.0, runs[0].distanceMeters, 0.001)
        assertEquals("GB", runs[0].countryCode)
    }

    @Test
    fun readRuns_skipsCorruptLinesWithoutCrashing() {
        val mixed = listOf(
            "not-a-valid-line",
            "1700000000000|1800000|5000.0|6.0|10.0|not-int|GB",
            "1700000000001|900000|1000.0|5.0|8.0|90|US",
            "too|few|fields"
        ).joinToString("\n")

        context.getSharedPreferences("run_history", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("records", mixed)
            .commit()

        val runs = LegacyPrefsReader.readRuns(context)
        assertEquals(1, runs.size)
        assertEquals("US", runs[0].countryCode)
    }

    @Test
    fun readRuns_blankPrefs_returnsEmptyList() {
        assertTrue(LegacyPrefsReader.readRuns(context).isEmpty())
    }
}
