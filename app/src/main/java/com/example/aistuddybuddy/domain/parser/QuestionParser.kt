package com.example.aistuddybuddy.domain.parser

import com.example.aistuddybuddy.domain.model.Question

/**
 * Turns raw OCR text into questions.
 *
 * Questions are split on numbering such as "1.", "2)", "(3)", "Q.", "Q4:" or "Question 5".
 * MCQ options are detected as a run of consecutive letter markers starting at "a"
 * ("a." "b)" "(c)" or upper case), either one per line or inline on the same line.
 */
object QuestionParser {

    private val questionMarker = Regex(
        """^\s*(?:Q(?:ues(?:tion)?)?\s*\.?\s*\d{0,3}\s*[.:)\-]|Q(?:ues(?:tion)?)?\s*\d{1,3}|\(?\d{1,3}\s*[.)])\s+""",
        RegexOption.IGNORE_CASE
    )

    // A letter marker that is not glued to a preceding word, e.g. " b. " or "(c) ".
    private val optionMarker = Regex("""(?<![\p{L}\p{N}])\(?([a-fA-F])[.)]\s+""")

    fun parse(rawText: String): List<Question> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val blocks = mutableListOf<MutableList<String>>()
        for (line in lines) {
            if (blocks.isEmpty() || startsNewQuestion(line)) {
                blocks += mutableListOf(line)
            } else {
                blocks.last() += line
            }
        }

        return blocks
            .map { parseBlock(it.joinToString(" ")) }
            .filter { it.text.isNotBlank() || it.options.isNotEmpty() }
    }

    private fun startsNewQuestion(line: String): Boolean = questionMarker.containsMatchIn(line)

    private fun parseBlock(block: String): Question {
        val body = stripQuestionMarker(block)
        val markers = optionMarker.findAll(body).toList()

        val sequence = mutableListOf<MatchResult>()
        for (marker in markers) {
            val letter = marker.groupValues[1]
            if (sequence.isEmpty()) {
                if (letter.equals("a", ignoreCase = true)) sequence += marker
            } else {
                val first = sequence.first().groupValues[1]
                val expected = first[0] + sequence.size
                if (letter[0] == expected) sequence += marker
            }
        }

        if (sequence.size < 2) return Question(text = body.normalizeSpaces())

        val questionText = body.substring(0, sequence.first().range.first)
        val options = sequence.mapIndexed { index, marker ->
            val end = sequence.getOrNull(index + 1)?.range?.first ?: body.length
            body.substring(marker.range.last + 1, end).normalizeSpaces()
        }
        return Question(text = questionText.normalizeSpaces(), options = options)
    }

    private fun stripQuestionMarker(block: String): String {
        val match = questionMarker.find(block) ?: return block
        return block.substring(match.range.last + 1)
    }

    private fun String.normalizeSpaces(): String = trim().replace(Regex("""\s+"""), " ")
}
