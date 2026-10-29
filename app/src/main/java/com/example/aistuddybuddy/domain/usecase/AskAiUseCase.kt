package com.example.aistuddybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.AiAnswer
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AnswerStyle
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.parser.AnswerSegments
import com.example.aistuddybuddy.domain.parser.PromptBuilder
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository

class AskAiUseCase(
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository
) {
    /** [image] is the photo of a figure the question depends on, if any. */
    suspend operator fun invoke(
        provider: AiProvider,
        question: Question,
        style: AnswerStyle = AnswerStyle.STANDARD,
        image: Bitmap? = null
    ): Result<AiAnswer> {
        return aiRepository.ask(
            provider = provider,
            systemPrompt = PromptBuilder.systemPrompt,
            userPrompt = PromptBuilder.userPrompt(
                question = question,
                style = style,
                language = settingsRepository.preferences.value.answerLanguage,
                hasImage = image != null
            ),
            image = image
        ).mapCatching { answer ->
            val text = answer.text.cleanUp()
            if (text.isBlank()) error("${provider.displayName} returned an empty answer. Please try again.")
            answer.copy(text = text)
        }
    }

    /**
     * Strips markdown emphasis that models sometimes add despite instructions. Code blocks are left
     * untouched: `#include`, `int **p` and `- x` are code there, not markdown.
     */
    private fun String.cleanUp(): String = AnswerSegments.split(this).joinToString("\n") { segment ->
        if (segment.isCode) {
            "```\n${segment.text}\n```"
        } else {
            segment.text
                .replace("**", "")
                .replace(Regex("""(?m)^#{1,6}\s*"""), "")
                .replace(Regex("""(?m)^\s*[*-]\s+"""), "• ")
        }
    }.trim()
}
