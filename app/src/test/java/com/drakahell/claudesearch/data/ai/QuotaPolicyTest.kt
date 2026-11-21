package com.drakahell.claudesearch.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class QuotaPolicyTest {

    private val pacific = ZoneId.of("America/Los_Angeles")

    @Test
    fun detectsDailyLimit() {
        assertTrue(QuotaPolicy.isDailyLimit("Quota exceeded for metric: GenerateRequestsPerDayPerProjectPerModel-FreeTier"))
        assertFalse(QuotaPolicy.isDailyLimit("Quota exceeded for metric: GenerateRequestsPerMinutePerProjectPerModel-FreeTier"))
        assertFalse(QuotaPolicy.isDailyLimit(null))
    }

    @Test
    fun minuteLimit_pausesForOneMinute() {
        val now = 1_000_000L
        assertEquals(now + 60_000L, QuotaPolicy.pausedUntil("PerMinute", now))
    }

    @Test
    fun dailyLimit_pausesUntilNextPacificMidnight() {
        val now = ZonedDateTime.of(2026, 11, 21, 15, 30, 0, 0, pacific).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 11, 22, 0, 0, 0, 0, pacific).toInstant().toEpochMilli()
        assertEquals(expected, QuotaPolicy.pausedUntil("RequestsPerDay", now))
    }
}
