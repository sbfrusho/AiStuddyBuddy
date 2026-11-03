package com.example.aistuddybuddy.domain.usecase

import android.graphics.Bitmap
import android.util.Log
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AiReadFailedException
import com.example.aistuddybuddy.domain.model.NoQuestionsFoundException
import com.example.aistuddybuddy.domain.model.ScanResult
import com.example.aistuddybuddy.domain.model.ScannedQuestion
import com.example.aistuddybuddy.domain.parser.AiQuestionParser
import com.example.aistuddybuddy.domain.parser.PromptBuilder
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException

/**
 * Turns the selected part of a page into questions. On-device OCR runs first because it is instant
 * and offline; a vision model only reads the photo when OCR's result looks misread (Bangla, math,
 * figures, poor light). If no AI can read it, this fails with [AiReadFailedException] rather than
 * quietly returning garbled text, so the student decides what to do.
 */
class ScanQuestionsUseCase(
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository,
    private val extractWithOcr: ExtractQuestionsUseCase
) {
    /** Every provider set up to read photos, in the order they are tried. */
    private fun readers(): List<AiProvider> =
        READERS.filter { aiRepository.hasApiKey(it) }

    /** [onReadingWithAi] is called each time a provider starts reading, so the UI can say which. */
    suspend operator fun invoke(
        image: Bitmap,
        onReadingWithAi: (AiProvider) -> Unit = {}
    ): Result<ScanResult> {
        val ocr = extractWithOcr(image)
        val ocrQuestions = ocr.getOrNull()?.questions.orEmpty().map { ScannedQuestion(it) }

        if (!settingsRepository.preferences.value.readWithAi) {
            return ocr.map { ScanResult(ocrQuestions, readBy = null) }
        }
        if (ocr.getOrNull()?.isReliable == true) {
            return Result.success(ScanResult(ocrQuestions, readBy = null))
        }

        val readers = readers()
        if (readers.isEmpty()) {
            return Result.failure(
                AiReadFailedException(
                    message = "This looks like Bangla, math or hard-to-read text, which needs AI to read. " +
                        "Add a free key in Settings (Gemini, Groq or Mistral take a minute).",
                    ocrQuestions = ocrQuestions,
                    needsApiKey = true
                )
            )
        }

        // Try each provider in turn, so a used-up Gemini quota falls back to OpenRouter.
        val failures = mutableListOf<String>()
        for (reader in readers) {
            onReadingWithAi(reader)
            try {
                val questions = readWithAi(reader, image).getOrThrow()
                if (questions.isNotEmpty()) return Result.success(ScanResult(questions, readBy = reader))
                // The AI looked and found nothing; OCR won't do better, so report that directly.
                return Result.failure(NoQuestionsFoundException())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "${reader.displayName} couldn't read the photo: ${e.message}", e)
                failures += "${reader.displayName}: ${e.message?.lineSequence()?.firstOrNull().orEmpty()}"
            }
        }
        return Result.failure(
            AiReadFailedException(
                message = "AI couldn't read the photo right now. The free models may be busy, so try again in a moment.\n" +
                    failures.joinToString("\n"),
                ocrQuestions = ocrQuestions
            )
        )
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

    private companion object {
        const val TAG = "StudyBuddyScan"

        /**
         * Free tiers first: Gemini, then Groq (fast) and Mistral (documents Bengali), then OpenRouter's
         * busy free pool and Cohere's small monthly quota. Paid-only providers are a last resort, so a
         * paid key is only spent when free ones fail. DeepSeek's API doesn't read images.
         */
        val READERS = listOf(
            AiProvider.GEMINI, AiProvider.GROQ, AiProvider.MISTRAL, AiProvider.OPENROUTER, AiProvider.COHERE,
            AiProvider.CLAUDE, AiProvider.CHATGPT
        )
    }
}
