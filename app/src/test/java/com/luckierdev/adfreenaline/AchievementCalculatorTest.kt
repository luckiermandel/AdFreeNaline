package com.luckierdev.adfreenaline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AchievementCalculatorTest {

    private fun run(
        timestampMs: Long = System.currentTimeMillis(),
        durationMs: Long = 1_800_000L,
        distanceMeters: Double = 5_000.0,
        avgPaceMinKm: Double = 6.0,
        avgSpeedKmh: Double = 10.0,
        calories: Int = 300,
        countryCode: String = "UNK"
    ) = RunRecord(timestampMs, durationMs, distanceMeters, avgPaceMinKm, avgSpeedKmh, calories, countryCode)

    private fun daysAgo(days: Int): Long =
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis

    private fun achievement(history: List<RunRecord>, id: String): Achievement =
        AchievementCalculator.build(history).first { it.id == id }

    @Test
    fun emptyHistory_hasNoUnlockedFirstRun() {
        val first = achievement(emptyList(), "first")
        assertFalse(first.unlocked)
        assertEquals("0", first.progressCurrent)
    }

    @Test
    fun shortRunsUnder500m_areIgnored() {
        val history = listOf(run(distanceMeters = 400.0))
        assertFalse(achievement(history, "first").unlocked)
    }

    @Test
    fun firstRun_unlocksWithOneEligibleRun() {
        val history = listOf(run())
        assertTrue(achievement(history, "first").unlocked)
    }

    @Test
    fun fiveK_unlocksAtExactlyFiveKm() {
        assertTrue(achievement(listOf(run(distanceMeters = 5_000.0)), "5k").unlocked)
        assertFalse(achievement(listOf(run(distanceMeters = 4_999.0)), "5k").unlocked)
    }

    @Test
    fun centuryClub_accumulatesAcrossRuns() {
        val history = (1..20).map { run(timestampMs = daysAgo(it), distanceMeters = 5_000.0) }
        assertTrue(achievement(history, "distance").unlocked)
    }

    @Test
    fun speedDemon_usesBestPace() {
        val history = listOf(
            run(avgPaceMinKm = 7.0),
            run(avgPaceMinKm = 4.5)
        )
        assertTrue(achievement(history, "speed").unlocked)
    }

    @Test
    fun bonfire_requiresSingleRunCalories() {
        val history = listOf(run(calories = 300), run(calories = 250))
        assertFalse(achievement(history, "calburn").unlocked)
        assertTrue(achievement(history + run(calories = 500), "calburn").unlocked)
    }

    @Test
    fun worldly_countsDistinctCountriesExcludingUnknown() {
        val history = listOf(
            run(countryCode = "DE"),
            run(countryCode = "DE"),
            run(countryCode = "UNK")
        )
        assertFalse(achievement(history, "worldly").unlocked)
        assertTrue(achievement(history + run(countryCode = "FR"), "worldly").unlocked)
    }

    @Test
    fun currentStreak_countsConsecutiveDaysEndingToday() {
        val history = listOf(run(daysAgo(0)), run(daysAgo(1)), run(daysAgo(2)))
        assertEquals(3, AchievementCalculator.currentStreak(history))
    }

    @Test
    fun currentStreak_isZeroWithoutRunToday() {
        val history = listOf(run(daysAgo(1)), run(daysAgo(2)))
        assertEquals(0, AchievementCalculator.currentStreak(history))
    }

    @Test
    fun bestStreak_findsLongestConsecutiveRun() {
        val history = listOf(
            run(daysAgo(10)), run(daysAgo(9)), run(daysAgo(8)), run(daysAgo(7)),
            run(daysAgo(2)), run(daysAgo(1))
        )
        assertEquals(4, AchievementCalculator.bestStreak(history))
    }

    @Test
    fun maxGapDays_measuresLargestGap() {
        val history = listOf(run(daysAgo(10)), run(daysAgo(3)), run(daysAgo(2)))
        assertEquals(7, AchievementCalculator.maxGapDays(history))
    }

    @Test
    fun filterByWindow_weekKeepsOnlyRecentRuns() {
        val history = listOf(run(daysAgo(1)), run(daysAgo(6)), run(daysAgo(20)))
        assertEquals(2, history.filterByWindow(StatsWindow.WEEK).size)
    }

    @Test
    fun metricValue_computesAggregates() {
        val records = listOf(
            run(durationMs = 600_000L, distanceMeters = 2_000.0),
            run(durationMs = 600_000L, distanceMeters = 3_000.0)
        )
        assertEquals(5.0, metricValue(records, StatsMetric.DISTANCE), 0.001)
        assertEquals(20.0, metricValue(records, StatsMetric.DURATION), 0.001)
        assertEquals(15.0, metricValue(records, StatsMetric.SPEED), 0.001)
    }

    @Test
    fun metricValue_emptyRecordsReturnZero() {
        assertEquals(0.0, metricValue(emptyList(), StatsMetric.SPEED), 0.0)
    }

    @Test
    fun build_returnsAllAchievements() {
        assertEquals(36, AchievementCalculator.build(emptyList()).size)
    }
}
