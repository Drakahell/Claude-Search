package com.drakahell.claudesearch.data.ai

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Decides how long Gemini stays paused after a "quota exceeded" error.
 *
 * The free tier has per-minute and per-day limits. Daily quotas reset at midnight
 * Pacific Time, per-minute ones within a minute, so the pause length depends on
 * which limit was hit (the server mentions it in the error message).
 */
object QuotaPolicy {

    private val QUOTA_RESET_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
    const val SHORT_PAUSE_MILLIS = 60_000L

    fun isDailyLimit(errorMessage: String?): Boolean {
        val text = errorMessage?.lowercase() ?: return false
        return "perday" in text || "per_day" in text || "per day" in text || "daily" in text
    }

    fun pausedUntil(errorMessage: String?, nowMillis: Long): Long =
        if (isDailyLimit(errorMessage)) nextPacificMidnight(nowMillis) else nowMillis + SHORT_PAUSE_MILLIS

    fun nextPacificMidnight(nowMillis: Long): Long =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), QUOTA_RESET_ZONE)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(QUOTA_RESET_ZONE)
            .toInstant()
            .toEpochMilli()
}
