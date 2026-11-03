package com.example.aistuddybuddy.domain.model

/** [needsImage] is true when the question refers to a figure, so the photo must go along with it. */
data class ScannedQuestion(
    val question: Question,
    val needsImage: Boolean = false
)

/** [readBy] is null when on-device OCR was used. */
data class ScanResult(
    val questions: List<ScannedQuestion>,
    val readBy: AiProvider?
)
