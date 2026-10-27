package com.example.aistuddybuddy.domain.model

enum class QuestionType { MCQ, DESCRIPTIVE }

/**
 * A question recovered from a scanned page. A question with two or more
 * options is treated as an MCQ, anything else as a descriptive question.
 */
data class Question(
    val text: String,
    val options: List<String> = emptyList()
) {
    val type: QuestionType
        get() = if (options.count { it.isNotBlank() } >= 2) QuestionType.MCQ else QuestionType.DESCRIPTIVE
}

fun optionLabel(index: Int): String = ('a' + index).toString()
