package com.example.aistuddybuddy.domain.parser

/** A run of answer text; [isCode] marks the inside of a ``` block, which must be shown verbatim. */
data class AnswerSegment(val text: String, val isCode: Boolean)

object AnswerSegments {

    private val fence = Regex("""^\s*```.*$""")

    /** Splits on ``` fence lines. An unclosed fence (e.g. a cut-off reply) runs to the end. */
    fun split(answer: String): List<AnswerSegment> {
        val segments = mutableListOf<AnswerSegment>()
        val current = mutableListOf<String>()
        var inCode = false
        fun flush() {
            if (current.isNotEmpty()) segments += AnswerSegment(current.joinToString("\n"), inCode)
            current.clear()
        }
        answer.lines().forEach { line ->
            if (fence.matches(line)) {
                flush()
                inCode = !inCode
            } else {
                current += line
            }
        }
        flush()
        return segments
    }

    /** The answer without fence lines, for copying code straight into an editor. */
    fun plainText(answer: String): String = split(answer).joinToString("\n") { it.text }.trim()
}
