package com.luckierdev.adfreenaline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunStatsCalculatorTest {

    @Test
    fun computeAvgPaceMinKm_returnsZeroForShortDistance() {
        assertEquals(0.0, computeAvgPaceMinKm(1.0, 60_000L), 0.001)
        assertEquals(0.0, computeAvgPaceMinKm(0.5, 60_000L), 0.001)
    }

    @Test
    fun computeAvgPaceMinKm_calculatesFiveMinuteKilometer() {
        // 1 km in 5 minutes = 5.0 min/km
        val pace = computeAvgPaceMinKm(1000.0, 300_000L)
        assertEquals(5.0, pace, 0.01)
    }

    @Test
    fun computeAvgSpeedKmh_calculatesTenKmh() {
        // 1000 m in 6 minutes = 10 km/h
        val speed = computeAvgSpeedKmh(1000.0, 360_000L)
        assertEquals(10.0, speed, 0.01)
    }

    @Test
    fun computeAvgSpeedKmh_returnsZeroWhenElapsedIsZero() {
        assertEquals(0.0, computeAvgSpeedKmh(500.0, 0L), 0.001)
    }

    @Test
    fun shouldAcceptGpsSegment_rejectsStandingStillDrift() {
        // 0.1 m over 2 seconds ≈ 0.18 km/h — below 0.5 km/h filter
        assertFalse(shouldAcceptGpsSegment(0.1, 2000L))
    }

    @Test
    fun shouldAcceptGpsSegment_acceptsWalkingPace() {
        // 2 m over 2 seconds = 3.6 km/h
        assertTrue(shouldAcceptGpsSegment(2.0, 2000L))
    }

    @Test
    fun estimateCaloriesBurned_returnsZeroForZeroDuration() {
        assertEquals(0, estimateCaloriesBurned(0L, 10.0, RunSettings()))
    }

    @Test
    fun estimateCaloriesBurned_scalesWithWeightAndDuration() {
        val light = estimateCaloriesBurned(3_600_000L, 9.0, RunSettings(weightKg = 60.0))
        val heavy = estimateCaloriesBurned(3_600_000L, 9.0, RunSettings(weightKg = 90.0))
        assertTrue(heavy > light)
    }

    @Test
    fun estimateCaloriesBurned_femaleAdjustsLowerThanMale() {
        val male = estimateCaloriesBurned(
            3_600_000L,
            9.0,
            RunSettings(weightKg = 70.0, sex = BiologicalSex.MALE)
        )
        val female = estimateCaloriesBurned(
            3_600_000L,
            9.0,
            RunSettings(weightKg = 70.0, sex = BiologicalSex.FEMALE)
        )
        assertTrue(female < male)
    }
}
