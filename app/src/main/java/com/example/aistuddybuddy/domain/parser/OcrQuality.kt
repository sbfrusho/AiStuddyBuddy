package com.example.aistuddybuddy.domain.parser

import com.example.aistuddybuddy.domain.model.Question

/**
 * Decides whether on-device OCR read the page well enough to skip the AI. The Latin recognizer
 * turns Bangla into short junk like "b. aJ d. qa faT" and mangles math, so text that doesn't look
 * like real words is sent to the AI instead.
 */
object OcrQuality {

    private const val MIN_CONFIDENCE = 0.7f
    private const val MIN_REAL_WORD_RATIO = 0.6
    private const val MIN_REAL_WORDS = 2

    /** A word is 3+ letters, cased like normal text (Word, word, WORD) and has a vowel. */
    private val realWord = Regex("""^([A-Z]?[a-z]{2,}|[A-Z]{3,})$""")
    private val vowel = Regex("""[aeiouyAEIOUY]""")
    private val token = Regex("""[\p{L}\p{N}#@&]+""")

    fun isReliable(questions: List<Question>, confidence: Float?): Boolean {
        if (questions.isEmpty() || questions.any { it.text.isBlank() }) return false
        if (confidence != null && confidence > 0f && confidence < MIN_CONFIDENCE) return false

        // Score the parsed text, not the raw OCR, so MCQ labels like "a." don't count against it.
        val tokens = questions
            .flatMap { listOf(it.text) + it.options }
            .flatMap { part -> token.findAll(part).map { it.value }.toList() }
        val wordLike = tokens.filter { it.any(Char::isLetter) }
        val real = wordLike.count { realWord.matches(it) && vowel.containsMatchIn(it) }
        if (real < MIN_REAL_WORDS) return false
        return real.toDouble() / wordLike.size >= MIN_REAL_WORD_RATIO
    }
}
