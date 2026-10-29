package com.example.aistuddybuddy.domain.parser

import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.model.ScannedQuestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Parses the JSON a vision model returns for [PromptBuilder.readImagePrompt]. */
object AiQuestionParser {

    @Serializable
    private data class Reply(val questions: List<Item> = emptyList())

    @Serializable
    private data class Item(
        val text: String = "",
        val options: List<String> = emptyList(),
        val needsFigure: Boolean = false
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Returns null when the reply isn't the expected JSON. */
    fun parse(reply: String): List<ScannedQuestion>? {
        // Models sometimes wrap the JSON in ``` fences or add a sentence around it.
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        val parsed = runCatching { json.decodeFromString<Reply>(reply.substring(start, end + 1)) }.getOrNull()
            ?: return null
        return parsed.questions
            .filter { it.text.isNotBlank() }
            .map { item ->
                ScannedQuestion(
                    question = Question(
                        text = item.text.trim(),
                        options = item.options.map { it.trim() }.filter { it.isNotEmpty() }
                    ),
                    needsImage = item.needsFigure
                )
            }
    }
}
