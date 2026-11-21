package com.drakahell.claudesearch.data.ai

import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import java.io.IOException

/** User-meaningful categories of Gemini errors. */
enum class SearchFailure {
    /** Free-tier limit reached: switch to Google Lens for a while. */
    QUOTA,
    /** Gemini is not available in the user's country. */
    REGION,
    /** The project/API key is not allowed to call Gemini (HTTP 403 PERMISSION_DENIED). */
    ACCESS_DENIED,
    NO_INTERNET,
    /** The request or answer was blocked by safety filters. */
    BLOCKED,
    OTHER;

    companion object {
        fun from(error: Throwable): SearchFailure {
            val message = error.message.orEmpty()
            return when {
                error is QuotaExceededException -> QUOTA
                // Some quota errors arrive as a generic server error (HTTP 429).
                "RESOURCE_EXHAUSTED" in message || "quota" in message.lowercase() -> QUOTA
                error is UnsupportedUserLocationException -> REGION
                "location is not supported" in message.lowercase() -> REGION
                "permission denied" in message.lowercase() -> ACCESS_DENIED
                error is PromptBlockedException || error is ContentBlockedException -> BLOCKED
                error is RequestTimeoutException -> NO_INTERNET
                error.causes().any { it is IOException } -> NO_INTERNET
                else -> OTHER
            }
        }

        private fun Throwable.causes(): Sequence<Throwable> =
            generateSequence(this) { it.cause.takeIf { cause -> cause !== it } }
    }
}
