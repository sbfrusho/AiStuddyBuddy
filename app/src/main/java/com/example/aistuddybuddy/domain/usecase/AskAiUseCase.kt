package com.example.aistuddybuddy.domain.usecase

import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.parser.PromptBuilder
import com.example.aistuddybuddy.domain.repository.AiRepository

class AskAiUseCase(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(provider: AiProvider, question: Question): Result<String> {
        return aiRepository.ask(
            provider = provider,
            systemPrompt = PromptBuilder.systemPrompt,
            userPrompt = PromptBuilder.userPrompt(question)
        )
    }
}
