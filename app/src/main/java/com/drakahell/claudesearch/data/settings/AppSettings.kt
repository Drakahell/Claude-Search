package com.drakahell.claudesearch.data.settings

/** Which service answers "what is this?" after a selection. */
enum class SearchProvider {
    GEMINI,
    GOOGLE_LENS,
}

/** All user settings. New settings are added here as the app grows. */
data class AppSettings(
    /** Provider chosen by the user. */
    val preferredProvider: SearchProvider = SearchProvider.GEMINI,
    /**
     * When Gemini hits its quota, it is paused until this moment (epoch millis)
     * and Google Lens is used instead. 0 means "not paused".
     */
    val geminiPausedUntilMillis: Long = 0L,
) {
    fun isGeminiPaused(nowMillis: Long): Boolean = nowMillis < geminiPausedUntilMillis

    /** Provider that should actually be used right now. */
    fun effectiveProvider(nowMillis: Long): SearchProvider =
        if (preferredProvider == SearchProvider.GEMINI && isGeminiPaused(nowMillis)) {
            SearchProvider.GOOGLE_LENS
        } else {
            preferredProvider
        }
}
