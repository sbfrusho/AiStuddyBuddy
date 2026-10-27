package com.example.aistuddybuddy.data.ai

import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.MissingApiKeyException
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException

class AiRepositoryImpl(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository,
    private val json: Json
) : AiRepository {

    override suspend fun ask(
        provider: AiProvider,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        val config = settingsRepository.config(provider)
        if (config.apiKey.isBlank()) return Result.failure(MissingApiKeyException(provider))
        val model = config.model.ifBlank { provider.defaultModel }

        return try {
            val answer = when (provider) {
                AiProvider.CHATGPT -> chatCompletion(
                    provider = provider,
                    url = OPENAI_URL,
                    apiKey = config.apiKey,
                    // Newer OpenAI models only accept the default temperature, so leave it unset.
                    request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt))
                )

                AiProvider.DEEPSEEK -> chatCompletion(
                    provider = provider,
                    url = DEEPSEEK_URL,
                    apiKey = config.apiKey,
                    request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt), temperature = 0.3)
                )

                AiProvider.OPENROUTER -> chatCompletion(
                    provider = provider,
                    url = OPENROUTER_URL,
                    apiKey = config.apiKey,
                    request = ChatCompletionRequest(model, messages(systemPrompt, userPrompt), temperature = 0.3)
                )

                AiProvider.GEMINI -> gemini(model, config.apiKey, systemPrompt, userPrompt)
            }
            if (answer.isBlank()) {
                Result.failure(IllegalStateException("${provider.displayName} returned an empty answer. Please try again."))
            } else {
                Result.success(answer.cleanUp())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(IOException("Network error. Check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun chatCompletion(provider: AiProvider, url: String, apiKey: String, request: ChatCompletionRequest): String {
        val response = httpClient.post(url) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.throwIfFailed(provider)
        return response.body<ChatCompletionResponse>()
            .choices.firstOrNull()?.message?.content.orEmpty()
    }

    private suspend fun gemini(model: String, apiKey: String, systemPrompt: String, userPrompt: String): String {
        val response = httpClient.post("$GEMINI_BASE_URL/$model:generateContent") {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userPrompt)))),
                    // Gemini 3 models are tuned for the default temperature, so leave it unset.
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
                )
            )
        }
        response.throwIfFailed(AiProvider.GEMINI)
        val candidate = response.body<GeminiResponse>().candidates.firstOrNull()
        if (candidate?.content == null && candidate?.finishReason == "SAFETY") {
            throw IllegalStateException("Gemini declined to answer this question.")
        }
        return candidate?.content?.parts
            ?.filter { it.thought != true }
            ?.mapNotNull { it.text }
            ?.joinToString("")
            .orEmpty()
    }

    private suspend fun HttpResponse.throwIfFailed(provider: AiProvider) {
        if (status.isSuccess()) return
        val error = runCatching {
            json.decodeFromString<ApiErrorResponse>(bodyAsText()).error
        }.getOrNull()
        val serverMessage = error?.message
        val errorCode = (error?.code as? JsonPrimitive)?.contentOrNull
        val message = when {
            errorCode == "insufficient_quota" || error?.type == "insufficient_quota" ||
                status == HttpStatusCode.PaymentRequired ->
                "Your ${provider.displayName} API account has no credit. The free ${provider.displayName} app " +
                    "doesn't include API use, so add balance on its website, or switch to Gemini or " +
                    "OpenRouter, which have free models."
            status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden ->
                "The API key was rejected. Check it in Settings."
            status == HttpStatusCode.TooManyRequests && provider == AiProvider.OPENROUTER ->
                "This free model is busy or you hit OpenRouter's daily free limit. Try another free model in Settings."
            status == HttpStatusCode.TooManyRequests ->
                "Too many requests right now (rate limit). Wait a minute and try again, or pick another model."
            else -> serverMessage ?: "Request failed (${status.value})."
        }
        throw IllegalStateException(
            if (serverMessage != null && message != serverMessage) "$message\n$serverMessage" else message
        )
    }

    private fun messages(systemPrompt: String, userPrompt: String) = listOf(
        ChatMessage(role = "system", content = systemPrompt),
        ChatMessage(role = "user", content = userPrompt)
    )

    /** Strips markdown emphasis that models sometimes add despite instructions. */
    private fun String.cleanUp(): String = this
        .replace("**", "")
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        .replace(Regex("""(?m)^\s*[*-]\s+"""), "• ")
        .trim()

    private companion object {
        const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
        const val DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
        const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
        const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
