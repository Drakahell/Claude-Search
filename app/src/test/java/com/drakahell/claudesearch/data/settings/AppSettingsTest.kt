package com.drakahell.claudesearch.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun geminiIsDefault() {
        assertEquals(SearchProvider.GEMINI, AppSettings().effectiveProvider(nowMillis = 0L))
    }

    @Test
    fun pausedGemini_fallsBackToLens_untilPauseEnds() {
        val settings = AppSettings(geminiPausedUntilMillis = 1_000L)
        assertEquals(SearchProvider.GOOGLE_LENS, settings.effectiveProvider(nowMillis = 999L))
        assertEquals(SearchProvider.GEMINI, settings.effectiveProvider(nowMillis = 1_000L))
    }

    @Test
    fun userChoiceOfLens_isRespected() {
        val settings = AppSettings(preferredProvider = SearchProvider.GOOGLE_LENS)
        assertEquals(SearchProvider.GOOGLE_LENS, settings.effectiveProvider(nowMillis = 0L))
    }
}
