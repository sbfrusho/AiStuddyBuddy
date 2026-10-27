package com.example.aistuddybuddy.domain.repository

import com.example.aistuddybuddy.domain.model.AiProvider

interface AiRepository {
    suspend fun ask(provider: AiProvider, systemPrompt: String, userPrompt: String): Result<String>
}
