package com.example.aistuddybuddy.domain.model

/** [readWithAi] sends the photo to a vision model instead of using on-device OCR. */
data class AppPreferences(
    val answerLanguage: AnswerLanguage = AnswerLanguage.ENGLISH,
    val readWithAi: Boolean = true
)
