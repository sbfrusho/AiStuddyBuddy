package com.example.aistuddybuddy.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// OpenAI-compatible chat completions (used by ChatGPT and DeepSeek)

/** [reasoningEffort] "none" turns off Groq's default thinking, which only slows short answers. */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatRequestMessage>,
    val temperature: Double? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null
)

/** [content] is a plain string, or an array of text and image_url parts when a photo is attached. */
@Serializable
data class ChatRequestMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatMessage? = null
)

// Gemini generateContent

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val thought: Boolean? = null,
    @SerialName("inlineData") val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mimeType") val mimeType: String,
    val data: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    @SerialName("responseMimeType") val responseMimeType: String? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerialName("finishReason") val finishReason: String? = null
)

// Claude Messages API (https://api.anthropic.com/v1/messages)

/**
 * [fallbacks] "default" lets the API re-run a request its safety classifiers decline on a
 * recommended fallback model (Opus 5.5 / Sonnet 5.5 only, with the matching beta header).
 */
@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>,
    val fallbacks: String? = null,
    @SerialName("output_config") val outputConfig: ClaudeOutputConfig? = null
)

@Serializable
data class ClaudeOutputConfig(val effort: String)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: List<ClaudeContent>
)

/** A text block, or an image block when [source] is set. */
@Serializable
data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val source: ClaudeImageSource? = null
)

/** [type] has no default on purpose: the serializer drops default values, and the API requires it. */
@Serializable
data class ClaudeImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String
)

/** [content] also holds thinking and fallback blocks; only "text" blocks are the answer. */
@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContent> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null
)

// Error body shared by all providers: {"error": {"message": "...", "code": ...}}

@Serializable
data class ApiErrorResponse(
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val message: String? = null,
    // A string for OpenAI/DeepSeek (e.g. "insufficient_quota"), a number for Gemini.
    val code: JsonElement? = null,
    val type: String? = null
)
