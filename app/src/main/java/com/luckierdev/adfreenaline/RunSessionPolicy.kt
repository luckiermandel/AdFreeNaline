package com.luckierdev.adfreenaline

/** Pure policy rules for run sessions, extracted for unit testing. */
object RunSessionPolicy {
    /** Runs shorter than this are discarded on finish. */
    const val MIN_SAVED_RUN_DISTANCE_METERS = 50.0

    /** An interrupted active run older than this is not restored. */
    const val MAX_ACTIVE_SESSION_AGE_MS = 24 * 60 * 60 * 1000L

    fun shouldSaveRun(distanceMeters: Double): Boolean =
        distanceMeters >= MIN_SAVED_RUN_DISTANCE_METERS

    fun shouldRestoreSession(ageMs: Long): Boolean =
        ageMs in 0..MAX_ACTIVE_SESSION_AGE_MS
}
