package com.example.aistuddybuddy.domain.repository

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.AiAnswer
import com.example.aistuddybuddy.domain.model.AiProvider

interface AiRepository {
    /**
     * Tries the provider's chosen model first and falls back to its other models when one is busy.
     * With an [image], only models that can read images are used. [jsonOutput] asks for a JSON reply.
     */
    suspend fun ask(
        provider: AiProvider,
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap? = null,
        jsonOutput: Boolean = false
    ): Result<AiAnswer>

    fun hasApiKey(provider: AiProvider): Boolean
}
