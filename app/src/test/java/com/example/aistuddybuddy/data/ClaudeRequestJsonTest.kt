package com.example.aistuddybuddy.data

import com.example.aistuddybuddy.data.ai.ClaudeContent
import com.example.aistuddybuddy.data.ai.ClaudeImageSource
import com.example.aistuddybuddy.data.ai.ClaudeMessage
import com.example.aistuddybuddy.data.ai.ClaudeOutputConfig
import com.example.aistuddybuddy.data.ai.ClaudeRequest
import com.example.aistuddybuddy.data.ai.ClaudeResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the Claude Messages API wire format, using the app's Json settings from di.kt. */
class ClaudeRequestJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    @Test
    fun `request with image matches the Messages API shape`() {
        val request = ClaudeRequest(
            model = "claude-opus-5-5",
            maxTokens = 16_000,
            system = "sys",
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(
                        ClaudeContent(type = "image", source = ClaudeImageSource(type = "base64", mediaType = "image/jpeg", data = "AAAA")),
                        ClaudeContent(type = "text", text = "Read this")
                    )
                )
            ),
            fallbacks = "default",
            outputConfig = ClaudeOutputConfig(effort = "low")
        )

        assertEquals(
            """{"model":"claude-opus-5-5","max_tokens":16000,"system":"sys","messages":[{"role":"user","content":[""" +
                """{"type":"image","source":{"type":"base64","media_type":"image/jpeg","data":"AAAA"}},""" +
                """{"type":"text","text":"Read this"}]}],"fallbacks":"default","output_config":{"effort":"low"}}""",
            json.encodeToString(request)
        )
    }

    @Test
    fun `optional fields are left out`() {
        val request = ClaudeRequest(
            model = "claude-haiku-5-5",
            maxTokens = 16_000,
            system = "sys",
            messages = listOf(ClaudeMessage("user", listOf(ClaudeContent(type = "text", text = "Hi"))))
        )

        assertEquals(
            """{"model":"claude-haiku-5-5","max_tokens":16000,"system":"sys","messages":[{"role":"user","content":[{"type":"text","text":"Hi"}]}]}""",
            json.encodeToString(request)
        )
    }

    @Test
    fun `response keeps only text blocks and reads stop reason`() {
        val body = """{"id":"msg_1","model":"claude-opus-5-5","content":[{"type":"thinking","thinking":"","signature":"x"},""" +
            """{"type":"text","text":"Ans: a. Dhaka"}],"stop_reason":"end_turn","usage":{"input_tokens":5,"output_tokens":7}}"""

        val response = json.decodeFromString<ClaudeResponse>(body)

        assertEquals("end_turn", response.stopReason)
        assertEquals("Ans: a. Dhaka", response.content.filter { it.type == "text" }.mapNotNull { it.text }.joinToString(""))
    }
}
