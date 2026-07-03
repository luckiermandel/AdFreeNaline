package com.luckierdev.adfreenaline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GoalAlertFormatTest {

    @Test
    fun formatGoalDistance_kilometers() {
        assertEquals("5.00 km", GoalAlertController.formatGoalDistance(5.0, DistanceUnit.KM))
    }

    @Test
    fun formatGoalDistance_tinyGoalShownInMeters() {
        assertEquals("5.0 m", GoalAlertController.formatGoalDistance(0.005, DistanceUnit.KM))
    }

    @Test
    fun formatGoalDistance_miles() {
        assertEquals("3.11 mi", GoalAlertController.formatGoalDistance(5.0, DistanceUnit.MI))
    }

    @Test
    fun formatProgressDistance_kilometers() {
        assertEquals("1.50 km", GoalAlertController.formatProgressDistance(1_500.0, DistanceUnit.KM))
    }

    @Test
    fun formatProgressDistance_smallValuesInMeters() {
        assertEquals("9.0 m", GoalAlertController.formatProgressDistance(9.0, DistanceUnit.KM))
    }

    @Test
    fun calendarWeekDistance_sumsCurrentWeekPlusLiveRun() {
        val now = System.currentTimeMillis()
        val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        val history = listOf(
            RunRecord(now, 1_800_000L, 4_000.0, 6.0, 10.0, 300),
            RunRecord(lastMonth, 1_800_000L, 9_999.0, 6.0, 10.0, 300)
        )
        val total = GoalAlertController.calendarWeekDistanceMeters(history, currentRunMeters = 1_000.0)
        assertEquals(5_000.0, total, 0.001)
    }
}
