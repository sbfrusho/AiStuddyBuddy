package com.example.aistuddybuddy.domain.model

/**
 * [isAvailable] is false for models that exist but can't be called through the provider's API.
 * [supportsImages] marks models that can read a photo of the page.
 * [isPaid] marks models billed per use; fallback never moves between free and paid models.
 */
data class AiModel(
    val id: String,
    val label: String,
    val description: String,
    val isAvailable: Boolean = true,
    val supportsImages: Boolean = false,
    val isPaid: Boolean = false
)

private fun openWeight(label: String, description: String) =
    AiModel(id = "", label = label, description = "$description Open-weight only, not on the API.", isAvailable = false)

/**
 * Free providers come first; Settings lists them in this order.
 * [models] are the model choices shown in Settings; the first available one is the default.
 * [isPaidOnly] providers have no free API tier, so they only appear on question cards once the
 * student has saved a key from a paid API account.
 */
enum class AiProvider(val displayName: String, val models: List<AiModel>, val isPaidOnly: Boolean = false) {
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
    GROQ(
        displayName = "Groq",
        // Free plan, no card. Very fast. Groq retires models quickly; only Qwen reads photos.
        models = listOf(
            AiModel("qwen/qwen3.8-27b", "Qwen 3.8 27B", "Fast, multilingual. Reads photos. Preview model.", supportsImages = true),
            AiModel("openai/gpt-oss-120b", "GPT-OSS 120B", "OpenAI's open model, strong reasoning. Text only."),
            AiModel("openai/gpt-oss-20b", "GPT-OSS 20B", "Smaller, faster GPT-OSS. Text only.")
        )
    ),
    MISTRAL(
        displayName = "Mistral",
        // Free mode, no card. The only free provider that officially lists Bengali support.
        models = listOf(
            AiModel("mistral-medium-latest", "Mistral Medium", "Balanced, multimodal. Reads photos.", supportsImages = true),
            AiModel("ministral-14b-latest", "Ministral 14B", "Small and fast. Reads photos.", supportsImages = true),
            AiModel("mistral-large-latest", "Mistral Large", "Most capable Mistral model. Reads photos.", supportsImages = true)
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
            AiModel("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B A4B (free)", "Lighter, faster Gemma 4. Reads photos.", supportsImages = true),
            // Paid models need OpenRouter credits, but skip the busy free pool. Prices per 1M tokens in/out.
            AiModel("google/gemini-3.8-flash", "Gemini 3.8 Flash (paid)", "Fast and reliable. ~\$0.83 / \$4.12. Reads photos.", supportsImages = true, isPaid = true),
            AiModel("openai/gpt-6-luna", "GPT-6 Luna (paid)", "OpenAI's fast low-cost model. ~\$0.10 / \$0.50. Reads photos.", supportsImages = true, isPaid = true),
            AiModel("anthropic/claude-haiku-5.5", "Claude Haiku 5.5 (paid)", "Fast, low-cost Claude. ~\$0.10 / \$0.50. Reads photos.", supportsImages = true, isPaid = true),
            AiModel("anthropic/claude-sonnet-5.5", "Claude Sonnet 5.5 (paid)", "Stronger reasoning. ~\$2 / \$10. Reads photos.", supportsImages = true, isPaid = true),
            AiModel("deepseek/deepseek-v4.1-flash", "DeepSeek V4.1 Flash (paid)", "Very cheap. ~\$0.04 / \$0.50. Reads photos.", supportsImages = true, isPaid = true)
        )
    ),
    COHERE(
        displayName = "Cohere",
        // A free trial key is created with the account, but it allows about 1,000 calls a month.
        models = listOf(
            AiModel("command-a-plus-05-2026", "Command A+", "Cohere's newest model. Reads photos.", supportsImages = true),
            AiModel("command-a-vision-07-2025", "Command A Vision", "Tuned for images and documents. Reads photos.", supportsImages = true),
            AiModel("c4ai-aya-vision-32b", "Aya Vision 32B", "Multilingual vision model. Reads photos.", supportsImages = true)
        )
    ),
    CLAUDE(
        displayName = "Claude",
        isPaidOnly = true,
        // Prices per 1M tokens in/out. Needs an API key with credits from the Claude Console;
        // a Claude Pro/Max subscription does not include API use.
        models = listOf(
            AiModel("claude-opus-5-5", "Claude Opus 5.5", "Most capable. \$4 / \$20. Reads photos.", supportsImages = true, isPaid = true),
            AiModel("claude-sonnet-5-5", "Claude Sonnet 5.5", "Fast and strong. \$2 / \$10. Reads photos.", supportsImages = true, isPaid = true),
            AiModel("claude-haiku-5-5", "Claude Haiku 5.5", "Fastest and cheapest. \$0.10 / \$0.50. Reads photos.", supportsImages = true, isPaid = true)
        )
    ),
    CHATGPT(
        displayName = "ChatGPT",
        isPaidOnly = true,
        models = listOf(
            AiModel("gpt-5.6-luna", "GPT-5.6 Luna", "Fast, low-cost everyday model.", supportsImages = true),
            AiModel("gpt-5.6-terra", "GPT-5.6 Terra", "Stronger reasoning, higher cost.", supportsImages = true),
            AiModel("gpt-5-mini", "GPT-5 Mini", "Legacy small reasoning model. Shuts down Dec 11, 2026.", supportsImages = true)
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        isPaidOnly = true,
        models = listOf(
            AiModel("deepseek-flash", "DeepSeek V4.1 Flash", "Fast general model (DeepSeek-V4.1-Flash-1017)."),
            AiModel("deepseek-v4-pro", "DeepSeek V4 Pro", "Most capable DeepSeek model, slower and pricier."),
        )
    );

    val defaultModel: String get() = models.first { it.isAvailable }.id

    fun modelLabel(id: String): String = models.firstOrNull { it.id == id }?.label ?: id
}

data class ProviderConfig(
    val apiKey: String = "",
    val model: String = ""
)
