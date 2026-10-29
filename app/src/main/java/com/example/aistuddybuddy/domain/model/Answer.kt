package com.example.aistuddybuddy.domain.model

/** [model] is the model that actually answered, which may differ from the chosen one after a fallback. */
data class AiAnswer(
    val text: String,
    val provider: AiProvider,
    val model: String
) {
    val modelLabel: String get() = provider.modelLabel(model)
}

/** The follow-up versions of an answer a student can ask for. */
enum class AnswerStyle(val label: String) {
    STANDARD("Answer"),
    SIMPLER("Simpler"),
    EXAMPLE("Example"),
    DETAILED("More detail")
}

enum class AnswerLanguage(val label: String) {
    ENGLISH("English"),
    BANGLA("বাংলা"),
    BOTH("Both")
}
