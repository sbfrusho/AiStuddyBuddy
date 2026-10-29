package com.example.aistuddybuddy.domain.model

/**
 * [isAvailable] is false for models that exist but can't be called through the provider's API.
 * [supportsImages] marks models that can read a photo of the page.
 */
data class AiModel(
    val id: String,
    val label: String,
    val description: String,
    val isAvailable: Boolean = true,
    val supportsImages: Boolean = false
)

private fun openWeight(label: String, description: String) =
    AiModel(id = "", label = label, description = "$description Open-weight only, not on the API.", isAvailable = false)

/**
 * [models] are the model choices shown in Settings; the first available one is the default.
 * [isEnabled] hides a provider from the UI without removing its integration.
 */
enum class AiProvider(val displayName: String, val models: List<AiModel>, val isEnabled: Boolean = true) {
    CHATGPT(
        displayName = "ChatGPT",
        // Hidden until paid API access is set up.
        isEnabled = false,
        models = listOf(
            AiModel("gpt-5.6-luna", "GPT-5.6 Luna", "Fast, low-cost everyday model.", supportsImages = true),
            AiModel("gpt-5.6-terra", "GPT-5.6 Terra", "Stronger reasoning, higher cost.", supportsImages = true),
            AiModel("gpt-5-mini", "GPT-5 Mini", "Legacy small reasoning model. Shuts down Dec 11, 2026.", supportsImages = true)
        )
    ),
    GEMINI(
        displayName = "Gemini",
        models = listOf(
            AiModel("gemini-3.8-flash", "Gemini 3.8 Flash", "Mainline fast Flash model for reasoning and agentic work.", supportsImages = true),
            AiModel("gemini-3.6-flash", "Gemini 3.6 Flash", "Stable, versatile mid-tier Flash model.", supportsImages = true),
            AiModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra-fast and lightweight, great for short answers.", supportsImages = true),
            AiModel("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "Ultra-fast, lightweight, high throughput.", supportsImages = true),
            AiModel("gemini-2.5-flash", "Gemini 2.5 Flash", "Previous-generation model on the free quota.", supportsImages = true),
            AiModel("gemini-2.5-pro", "Gemini 2.5 Pro", "Legacy Pro model with a restricted daily free quota.", supportsImages = true)
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        // Hidden until paid API access is set up.
        isEnabled = false,
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
            AiModel("google/gemma-4-31b-it:free", "Gemma 4 31B (free)", "Google's open model, good all-rounder. Reads photos.", supportsImages = true),
            AiModel("thinkingmachines/inkling:free", "Inkling (free)", "Thinking Machines general model."),
            AiModel("nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning (free)", "Very fast, for quick short answers."),
            AiModel("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B A4B (free)", "Lighter, faster Gemma 4. Reads photos.", supportsImages = true)
        )
    );

    val defaultModel: String get() = models.first { it.isAvailable }.id

    fun modelLabel(id: String): String = models.firstOrNull { it.id == id }?.label ?: id

    companion object {
        /** Providers shown in the UI. */
        val enabled: List<AiProvider> get() = entries.filter { it.isEnabled }
    }
}

data class ProviderConfig(
    val apiKey: String = "",
    val model: String = ""
)
