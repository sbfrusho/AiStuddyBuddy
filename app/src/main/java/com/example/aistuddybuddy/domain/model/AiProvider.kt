package com.example.aistuddybuddy.domain.model

/** [isAvailable] is false for models that exist but can't be called through the provider's API. */
data class AiModel(
    val id: String,
    val label: String,
    val description: String,
    val isAvailable: Boolean = true
)

private fun openWeight(label: String, description: String) =
    AiModel(id = "", label = label, description = "$description Open-weight only, not on the API.", isAvailable = false)

/** [models] are the model choices shown in Settings; the first available one is the default. */
enum class AiProvider(val displayName: String, val models: List<AiModel>) {
    CHATGPT(
        displayName = "ChatGPT",
        models = listOf(
            AiModel("gpt-5.6-luna", "GPT-5.6 Luna", "Fast, low-cost everyday model."),
            AiModel("gpt-5.6-terra", "GPT-5.6 Terra", "Stronger reasoning, higher cost."),
            AiModel("gpt-5-mini", "GPT-5 Mini", "Legacy small reasoning model. Shuts down Dec 11, 2026.")
        )
    ),
    GEMINI(
        displayName = "Gemini",
        models = listOf(
            AiModel("gemini-3.8-flash", "Gemini 3.8 Flash", "Mainline fast Flash model for reasoning and agentic work."),
            AiModel("gemini-3.6-flash", "Gemini 3.6 Flash", "Stable, versatile mid-tier Flash model."),
            AiModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra-fast and lightweight, great for short answers."),
            AiModel("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "Ultra-fast, lightweight, high throughput."),
            AiModel("gemini-2.5-flash", "Gemini 2.5 Flash", "Previous-generation model on the free quota."),
            AiModel("gemini-2.5-pro", "Gemini 2.5 Pro", "Legacy Pro model with a restricted daily free quota.")
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        models = listOf(
            AiModel("deepseek-flash", "DeepSeek V4.1 Flash", "Fast general model (DeepSeek-V4.1-Flash-1017)."),
            AiModel("deepseek-v4-pro", "DeepSeek V4 Pro", "Most capable DeepSeek model, slower and pricier."),
        )
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        // Free models rotate often on OpenRouter; use "Custom model ID" for any other one.
        models = listOf(
            AiModel("nvidia/nemotron-3-ultra-550b-a55b:free", "Nemotron 3 Ultra (free)", "NVIDIA's largest free model, strong reasoning."),
            AiModel("google/gemma-4-31b-it:free", "Gemma 4 31B (free)", "Google's open model, good all-rounder."),
            AiModel("thinkingmachines/inkling:free", "Inkling (free)", "Thinking Machines general model."),
            AiModel("nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning (free)", "Very fast, for quick short answers."),
            AiModel("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B A4B (free)", "Lighter, faster Gemma 4.")
        )
    );

    val defaultModel: String get() = models.first { it.isAvailable }.id
}

data class ProviderConfig(
    val apiKey: String = "",
    val model: String = ""
)
