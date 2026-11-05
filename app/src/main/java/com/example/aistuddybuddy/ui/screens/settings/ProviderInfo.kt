package com.example.aistuddybuddy.ui.screens.settings

import com.example.aistuddybuddy.domain.model.AiProvider

/** Where the student creates a key for each AI. */
val AiProvider.apiKeyUrl: String
    get() = when (this) {
        AiProvider.GEMINI -> "https://aistudio.google.com/apikey"
        AiProvider.GROQ -> "https://console.groq.com/keys"
        AiProvider.MISTRAL -> "https://console.mistral.ai/api-keys"
        AiProvider.OPENROUTER -> "https://openrouter.ai/settings/keys"
        AiProvider.COHERE -> "https://dashboard.cohere.com/api-keys"
        AiProvider.CLAUDE -> "https://console.anthropic.com/settings/keys"
        AiProvider.CHATGPT -> "https://platform.openai.com/api-keys"
        AiProvider.DEEPSEEK -> "https://platform.deepseek.com/api_keys"
    }

/** One line on why to pick this AI, shown under its name. */
val AiProvider.tagline: String
    get() = when (this) {
        AiProvider.GEMINI -> "Free · Sign in with your Google account · Reads photos"
        AiProvider.GROQ -> "Free · Very fast · Reads photos"
        AiProvider.MISTRAL -> "Free · Supports Bengali · Reads photos"
        AiProvider.OPENROUTER -> "Free models · Can be busy at peak times"
        AiProvider.COHERE -> "Free trial · About 1,000 questions a month"
        AiProvider.CLAUDE -> "Paid API credit · Very capable · Reads photos"
        AiProvider.CHATGPT -> "Paid API credit · Reads photos"
        AiProvider.DEEPSEEK -> "Paid API credit · Low cost · Text only"
    }

/** Plain steps a student can follow on their phone to get a key. */
val AiProvider.setupSteps: List<String>
    get() = when (this) {
        AiProvider.GEMINI -> listOf(
            "Tap “Get a free key” and sign in with your Google account.",
            "Tap “Create API key”, then copy it.",
            "Come back and tap Paste."
        )
        AiProvider.GROQ -> listOf(
            "Tap “Get a free key” and sign up or sign in.",
            "Tap “Create API Key”, give it any name, then copy it.",
            "Come back and tap Paste."
        )
        AiProvider.MISTRAL -> listOf(
            "Tap “Get a free key” and sign up. Mistral may ask you to verify your phone number.",
            "Choose the free plan if asked, then tap “Create new key” and copy it.",
            "Come back and tap Paste."
        )
        AiProvider.OPENROUTER -> listOf(
            "Tap “Get a free key” and sign in.",
            "Tap “Create Key”, then copy it. It is only shown once.",
            "Come back and tap Paste."
        )
        AiProvider.COHERE -> listOf(
            "Tap “Get a free key” and sign up.",
            "A trial key is already waiting on the API Keys page. Copy it.",
            "Come back and tap Paste."
        )
        AiProvider.CLAUDE, AiProvider.CHATGPT, AiProvider.DEEPSEEK -> listOf(
            "Tap “Get a key” and sign in to the developer console.",
            "Add API credit under Billing. A chat subscription doesn't include API use.",
            "Create an API key, copy it, then come back and tap Paste."
        )
    }

/** The quickest free AIs to set up, offered on the welcome screen. */
val RecommendedProviders = listOf(AiProvider.GEMINI, AiProvider.GROQ, AiProvider.MISTRAL)
