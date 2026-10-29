package com.example.aistuddybuddy.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// OpenAI-compatible chat completions (used by ChatGPT and DeepSeek)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatRequestMessage>,
    val temperature: Double? = null
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

// Error body shared by all three providers: {"error": {"message": "...", "code": ...}}

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
