package com.luckierdev.adfreenaline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSessionPolicyTest {

    @Test
    fun runsShorterThan50m_areDiscarded() {
        assertFalse(RunSessionPolicy.shouldSaveRun(49.9))
        assertTrue(RunSessionPolicy.shouldSaveRun(50.0))
        assertTrue(RunSessionPolicy.shouldSaveRun(51.0))
    }

    @Test
    fun sessionsWithin24h_areRestored() {
        assertTrue(RunSessionPolicy.shouldRestoreSession(0L))
        assertTrue(RunSessionPolicy.shouldRestoreSession(RunSessionPolicy.MAX_ACTIVE_SESSION_AGE_MS))
    }

    @Test
    fun sessionsOlderThan24h_areNotRestored() {
        assertFalse(RunSessionPolicy.shouldRestoreSession(RunSessionPolicy.MAX_ACTIVE_SESSION_AGE_MS + 1))
    }

    @Test
    fun sessionsWithNegativeAge_areNotRestored() {
        assertFalse(RunSessionPolicy.shouldRestoreSession(-1L))
    }
}
