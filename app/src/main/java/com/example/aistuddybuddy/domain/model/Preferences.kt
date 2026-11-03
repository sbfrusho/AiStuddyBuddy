package com.example.aistuddybuddy.domain.model

/**
 * [readWithAi] sends photos OCR can't read reliably to a vision model.
 * [onboardingDone] is set once the welcome screen has been finished or skipped.
 */
data class AppPreferences(
    val answerLanguage: AnswerLanguage = AnswerLanguage.ENGLISH,
    val readWithAi: Boolean = true,
    val onboardingDone: Boolean = false
)
