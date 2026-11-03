package com.example.aistuddybuddy.data.ai

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.AiAnswer
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.MissingApiKeyException
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import com.example.aistuddybuddy.util.toJpegBase64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException

class AiRepositoryImpl(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository,
    private val json: Json
) : AiRepository {

    /** A failed request; [retryable] means another model may succeed where this one didn't. */
    private class ApiException(message: String, val retryable: Boolean) : Exception(message)

    override fun hasApiKey(provider: AiProvider): Boolean = settingsRepository.config(provider).apiKey.isNotBlank()

    override suspend fun ask(
        provider: AiProvider,
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap?,
        jsonOutput: Boolean
    ): Result<AiAnswer> {
        val config = settingsRepository.config(provider)
        if (config.apiKey.isBlank()) return Result.failure(MissingApiKeyException(provider))

        val models = candidateModels(provider, config.model.ifBlank { provider.defaultModel }, needsImages = image != null)
        if (models.isEmpty()) {
            return Result.failure(IllegalStateException("${provider.displayName} can't read photos. Pick Gemini or OpenRouter for this question."))
        }
        val imageBase64 = image?.let { withContext(Dispatchers.Default) { it.toJpegBase64() } }

        var lastError: Exception? = null
        for (model in models) {
            try {
                val text = request(provider, model, config.apiKey, systemPrompt, userPrompt, imageBase64, jsonOutput)
                if (text.isNotBlank()) return Result.success(AiAnswer(text, provider, model))
                lastError = IllegalStateException("${provider.displayName} returned an empty answer. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (!e.retryable) return Result.failure(e)
                lastError = e
            } catch (e: IOException) {
                return Result.failure(IOException("Network error. Check your internet connection and try again.", e))
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Request failed."))
    }

    override suspend fun verifyKey(provider: AiProvider, apiKey: String, model: String): Result<Unit> = try {
        request(provider, model, apiKey.trim(), VERIFY_SYSTEM_PROMPT, VERIFY_USER_PROMPT, imageBase64 = null, jsonOutput = false)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IOException("Network error. Check your internet connection and try again.", e))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * The chosen model first, then the provider's other listed models as fallbacks. On OpenRouter,
     * fallbacks stay on the chosen model's side of free/paid, so a busy free model never quietly
     * spends credits and a paid model never drops to the busy free pool.
     */
    private fun candidateModels(provider: AiProvider, chosen: String, needsImages: Boolean): List<String> {
        val chosenIsPaid = provider.models.firstOrNull { it.id == chosen }?.isPaid ?: !chosen.endsWith(":free")
        val listed = provider.models
            .filter { it.isAvailable && (!needsImages || it.supportsImages) }
            .filter { provider != AiProvider.OPENROUTER || it.isPaid == chosenIsPaid }
            .map { it.id }
        // A custom model ID isn't in the list, so we can't tell whether it reads images; let it try.
        val chosenKnownTextOnly = provider.models.any { it.id == chosen && !it.supportsImages }
        val first = chosen.takeUnless { needsImages && chosenKnownTextOnly }
        return (listOfNotNull(first) + listed).distinct().take(MAX_ATTEMPTS)
    }

    private suspend fun request(
        provider: AiProvider,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        jsonOutput: Boolean
    ): String = when (provider) {
        AiProvider.CHATGPT -> chatCompletion(
            provider = provider,
            url = OPENAI_URL,
            apiKey = apiKey,
            // Newer OpenAI models only accept the default temperature, so leave it unset.
            request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt, imageBase64))
        )

        AiProvider.DEEPSEEK -> chatCompletion(
            provider = provider,
            url = DEEPSEEK_URL,
            apiKey = apiKey,
            request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt, imageBase64 = null), temperature = 0.3)
        )

        AiProvider.OPENROUTER -> chatCompletion(
            provider = provider,
            url = OPENROUTER_URL,
            apiKey = apiKey,
            request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt, imageBase64), temperature = 0.3)
        )

        AiProvider.GROQ -> chatCompletion(
            provider = provider,
            url = GROQ_URL,
            apiKey = apiKey,
            request = ChatCompletionRequest(
                model,
                messages(systemPrompt, userPrompt, imageBase64),
                temperature = 0.3,
                reasoningEffort = "none".takeIf { model.startsWith("qwen/") }
            )
        )

        AiProvider.MISTRAL -> chatCompletion(
            provider = provider,
            url = MISTRAL_URL,
            apiKey = apiKey,
            // Mistral's docs show image_url as a plain data-URL string rather than {"url": ...}.
            request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt, imageBase64, imageUrlAsString = true), temperature = 0.3)
        )

        AiProvider.COHERE -> chatCompletion(
            provider = provider,
            url = COHERE_URL,
            apiKey = apiKey,
            request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt, imageBase64), temperature = 0.3)
        )

        AiProvider.GEMINI -> gemini(model, apiKey, systemPrompt, userPrompt, imageBase64, jsonOutput)

        AiProvider.CLAUDE -> claude(model, apiKey, systemPrompt, userPrompt, imageBase64, jsonOutput)
    }

    private suspend fun chatCompletion(provider: AiProvider, url: String, apiKey: String, request: ChatCompletionRequest): String {
        val response = httpClient.post(url) {
            bearerAuth(apiKey)
            if (provider == AiProvider.OPENROUTER) header("X-Title", "StudyBuddy AI")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.throwIfFailed(provider)
        return response.body<ChatCompletionResponse>()
            .choices.firstOrNull()?.message?.content.orEmpty()
    }

    private suspend fun gemini(
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        jsonOutput: Boolean
    ): String {
        val parts = listOfNotNull(
            imageBase64?.let { GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = it)) },
            GeminiPart(text = userPrompt)
        )
        val response = httpClient.post("$GEMINI_BASE_URL/$model:generateContent") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = parts)),
                    // Gemini 3 models are tuned for the default temperature, so leave it unset.
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                    generationConfig = if (jsonOutput) GeminiGenerationConfig(responseMimeType = "application/json") else null
                )
            )
        }
        response.throwIfFailed(AiProvider.GEMINI)
        val candidate = response.body<GeminiResponse>().candidates.firstOrNull()
        if (candidate?.content == null && candidate?.finishReason == "SAFETY") {
            throw ApiException("Gemini declined to answer this question.", retryable = false)
        }
        return candidate?.content?.parts
            ?.filter { it.thought != true }
            ?.mapNotNull { it.text }
            ?.joinToString("")
            .orEmpty()
    }

    private suspend fun claude(
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        jsonOutput: Boolean
    ): String {
        // The image goes before the text it is about.
        val content = listOfNotNull(
            imageBase64?.let {
                ClaudeContent(type = "image", source = ClaudeImageSource(type = "base64", mediaType = "image/jpeg", data = it))
            },
            ClaudeContent(type = "text", text = userPrompt)
        )
        val serverFallback = model in CLAUDE_SERVER_FALLBACK_MODELS
        val response = httpClient.post(CLAUDE_URL) {
            header("x-api-key", apiKey)
            header("anthropic-version", CLAUDE_API_VERSION)
            if (serverFallback) header("anthropic-beta", CLAUDE_FALLBACK_BETA)
            contentType(ContentType.Application.Json)
            setBody(
                ClaudeRequest(
                    model = model,
                    maxTokens = CLAUDE_MAX_TOKENS,
                    system = systemPrompt,
                    messages = listOf(ClaudeMessage(role = "user", content = content)),
                    fallbacks = if (serverFallback) "default" else null,
                    // Copying questions out of a photo is transcription, not reasoning: low effort is
                    // faster and cheaper. Answers keep the model's default effort.
                    outputConfig = if (jsonOutput) ClaudeOutputConfig(effort = "low") else null
                )
            )
        }
        response.throwIfFailed(AiProvider.CLAUDE)
        val body = response.body<ClaudeResponse>()
        if (body.stopReason == "refusal") {
            throw ApiException("Claude declined to answer this question.", retryable = false)
        }
        return body.content
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("")
    }

    private suspend fun HttpResponse.throwIfFailed(provider: AiProvider) {
        if (status.isSuccess()) return
        val error = runCatching {
            json.decodeFromString<ApiErrorResponse>(bodyAsText()).error
        }.getOrNull()
        val serverMessage = error?.message
        val errorCode = (error?.code as? JsonPrimitive)?.contentOrNull
        val noCredit = errorCode == "insufficient_quota" || error?.type == "insufficient_quota" ||
            error?.type == "billing_error" || status == HttpStatusCode.PaymentRequired ||
            serverMessage?.contains("credit balance", ignoreCase = true) == true
        val message = when {
            noCredit ->
                "Your ${provider.displayName} API account has no credit. A ${provider.displayName} app or chat " +
                    "subscription doesn't include API use, so add API credit on its website, or switch to " +
                    "Gemini or OpenRouter, which have free models."
            status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden ->
                "The API key was rejected. Check it in Settings."
            status == HttpStatusCode.TooManyRequests && provider == AiProvider.OPENROUTER ->
                "The free models are busy or you hit OpenRouter's daily free limit. Try again in a minute."
            status == HttpStatusCode.TooManyRequests ->
                "Too many requests right now (rate limit). Wait a minute and try again."
            else -> serverMessage ?: "Request failed (${status.value})."
        }
        // Busy, overloaded or retired models are worth retrying on another model; bad keys and credit aren't.
        val retryable = status == HttpStatusCode.TooManyRequests ||
            status == HttpStatusCode.NotFound ||
            status.value >= 500
        throw ApiException(
            message = if (serverMessage != null && message != serverMessage) "$message\n$serverMessage" else message,
            retryable = retryable
        )
    }

    private fun messages(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String?,
        imageUrlAsString: Boolean = false
    ) = listOf(
        ChatRequestMessage(role = "system", content = JsonPrimitive(systemPrompt)),
        ChatRequestMessage(role = "user", content = userContent(userPrompt, imageBase64, imageUrlAsString))
    )

    private fun userContent(text: String, imageBase64: String?, imageUrlAsString: Boolean): JsonElement {
        if (imageBase64 == null) return JsonPrimitive(text)
        return buildJsonArray {
            addJsonObject {
                put("type", "text")
                put("text", text)
            }
            addJsonObject {
                put("type", "image_url")
                val dataUrl = "data:image/jpeg;base64,$imageBase64"
                if (imageUrlAsString) put("image_url", dataUrl) else putJsonObject("image_url") { put("url", dataUrl) }
            }
        }
    }

    private companion object {
        const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
        const val DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
        const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
        const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
        const val COHERE_URL = "https://api.cohere.ai/compatibility/v1/chat/completions"
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val CLAUDE_URL = "https://api.anthropic.com/v1/messages"
        const val CLAUDE_API_VERSION = "2026-10-21"
        const val CLAUDE_FALLBACK_BETA = "server-side-fallback-2026-07-01"
        const val CLAUDE_MAX_TOKENS = 16_000

        /** Models that accept `fallbacks: "default"`; Haiku has no server-side fallback. */
        val CLAUDE_SERVER_FALLBACK_MODELS = setOf("claude-opus-5-5", "claude-sonnet-5-5")

        const val VERIFY_SYSTEM_PROMPT = "Reply with the single word OK."
        const val VERIFY_USER_PROMPT = "OK?"

        /** How many models to try before giving up, so a bad moment doesn't take minutes. */
        const val MAX_ATTEMPTS = 3
    }
}
