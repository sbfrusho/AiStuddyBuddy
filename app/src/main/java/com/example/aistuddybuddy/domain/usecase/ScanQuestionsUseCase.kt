package com.example.aistuddybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.NoQuestionsFoundException
import com.example.aistuddybuddy.domain.model.ScanResult
import com.example.aistuddybuddy.domain.model.ScannedQuestion
import com.example.aistuddybuddy.domain.parser.AiQuestionParser
import com.example.aistuddybuddy.domain.parser.PromptBuilder
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException

/**
 * Turns the selected part of a page into questions. A vision model reads the photo when one is
 * set up (handles Bangla, math and figures); otherwise, or if that fails, on-device OCR is used.
 */
class ScanQuestionsUseCase(
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository,
    private val extractWithOcr: ExtractQuestionsUseCase
) {
    /** The provider that will read the photo, or null when OCR will be used. */
    fun reader(): AiProvider? {
        if (!settingsRepository.preferences.value.readWithAi) return null
        return READERS.firstOrNull { it.isEnabled && aiRepository.hasApiKey(it) }
    }

    suspend operator fun invoke(image: Bitmap): Result<ScanResult> {
        val reader = reader() ?: return ocr(image, notice = null)

        val aiFailure = try {
            val questions = readWithAi(reader, image).getOrThrow()
            if (questions.isNotEmpty()) return Result.success(ScanResult(questions, readBy = reader))
            NoQuestionsFoundException()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }

        // The AI looked and found nothing; OCR won't do better, so report that directly.
        if (aiFailure is NoQuestionsFoundException) return Result.failure(aiFailure)
        val reason = aiFailure.message?.lineSequence()?.firstOrNull().orEmpty()
        return ocr(image, notice = "${reader.displayName} couldn't read the photo, so on-device OCR was used (English only). $reason".trim())
    }

    private suspend fun readWithAi(reader: AiProvider, image: Bitmap): Result<List<ScannedQuestion>> =
        aiRepository.ask(
            provider = reader,
            systemPrompt = PromptBuilder.readImageSystemPrompt,
            userPrompt = PromptBuilder.readImagePrompt,
            image = image,
            jsonOutput = true
        ).mapCatching { answer ->
            AiQuestionParser.parse(answer.text) ?: error("The reply wasn't in the expected format.")
        }

    private suspend fun ocr(image: Bitmap, notice: String?): Result<ScanResult> =
        extractWithOcr(image).map { questions ->
            ScanResult(questions.map { ScannedQuestion(it) }, readBy = null, notice = notice)
        }

    private companion object {
        /** Gemini first: every Gemini model reads images, only some free OpenRouter models do. */
        val READERS = listOf(AiProvider.GEMINI, AiProvider.OPENROUTER)
    }
}
