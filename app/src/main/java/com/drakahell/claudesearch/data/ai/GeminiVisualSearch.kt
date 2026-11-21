package com.drakahell.claudesearch.data.ai

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Locale
import kotlin.math.max

/**
 * Visual search with Gemini (Gemini Developer API through Firebase AI Logic).
 *
 * Usage: create a [Session] per selection, call [Session.describe] once,
 * then [Session.ask] for follow-up questions. Each call streams text chunks.
 */
class GeminiVisualSearch(private val modelName: String = MODEL_NAME) {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            systemInstruction = content { text(systemPrompt(Locale.getDefault())) },
        )
    }

    fun newSession(): Session = Session(model.startChat())

    class Session internal constructor(private val chat: Chat) {

        /** Asks what is in [crop]; [fullImage] is sent as context. */
        fun describe(crop: Bitmap, fullImage: Bitmap?): Flow<String> {
            val prompt = content {
                image(crop.limitSide(CROP_MAX_SIDE))
                fullImage?.let { image(it.limitSide(CONTEXT_MAX_SIDE)) }
                text(
                    if (fullImage != null) {
                        "The first image is the area I selected. The second image is the whole " +
                            "screen, for context only. What is in the selected area?"
                    } else {
                        "What is in this image?"
                    }
                )
            }
            return chat.sendMessageStream(prompt)
                .map { it.text.orEmpty() }
                .flowOn(Dispatchers.Default)
        }

        /** Follow-up question about the same selection. */
        fun ask(question: String): Flow<String> =
            chat.sendMessageStream(question).map { it.text.orEmpty() }
    }

    companion object {
        /** Stable Flash model, works on the free tier. See firebase.google.com/docs/ai-logic/models. */
        const val MODEL_NAME = "gemini-3.8-flash"

        private const val CROP_MAX_SIDE = 1024
        private const val CONTEXT_MAX_SIDE = 768

        internal fun systemPrompt(locale: Locale): String {
            val language = locale.getDisplayLanguage(Locale.ENGLISH).ifBlank { "English" }
            return """
                You are a visual search assistant inside a phone app. The user circles something
                on their screen and wants to know what it is.
                - Identify the main object, place, product, text, plant, animal, landmark, etc.
                - Start with a short title line naming what it is, then 2–4 short sentences of
                  the most useful facts (what it is used for, notable details, where to find it,
                  approximate price for products when reasonable).
                - If the selection contains text: transcribe it, and translate it if it is not in
                  $language.
                - If you are not sure, say so and give the most likely options.
                - Always answer in $language.
                - Plain text only: no Markdown, no asterisks, no headings. Use "• " for lists.
                - Keep answers to follow-up questions short and to the point.
            """.trimIndent()
        }

        private fun Bitmap.limitSide(maxSide: Int): Bitmap {
            val longest = max(width, height)
            if (longest <= maxSide) return this
            val factor = maxSide.toFloat() / longest
            return scale((width * factor).toInt().coerceAtLeast(1), (height * factor).toInt().coerceAtLeast(1))
        }
    }
}
