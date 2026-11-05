package com.example.aistuddybuddy.domain.model

/** On-device OCR output. [confidence] is the recognizer's average line confidence (0..1), if it reports one. */
data class OcrText(
    val text: String,
    val confidence: Float? = null
)
