package com.example.aistuddybuddy.domain.parser

import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.model.QuestionType
import com.example.aistuddybuddy.domain.model.optionLabel

object PromptBuilder {

    val systemPrompt = """
        You are StudyBuddy, a friendly tutor helping a student who is reading a textbook.
        Explain in simple, clear language that a student understands on the first read.
        Be accurate. Keep answers focused: not too long, not too short.
        Write plain text only. Do not use markdown symbols such as **, #, ``` or tables.
        The question was read from a photo using OCR and may contain small recognition errors;
        silently interpret the intended question.
    """.trimIndent()

    fun userPrompt(question: Question): String = when (question.type) {
        QuestionType.MCQ -> {
            val options = question.options
                .filter { it.isNotBlank() }
                .mapIndexed { index, option -> "${optionLabel(index)}. $option" }
                .joinToString("\n")
            // Options are spliced in after trimIndent() so their line breaks don't skew the indent.
            """
                This is a multiple-choice question.

                Question: ${question.text}
                Options:
                %OPTIONS%

                Reply in exactly this format and nothing else:
                Ans: <letter>. <option text>
                Why: <one or two short sentences explaining why it is correct>
            """.trimIndent().replace("%OPTIONS%", options)
        }

        QuestionType.DESCRIPTIVE -> """
            Question: ${question.text}

            Answer in about 80 to 150 words.
            Start with "Ans: " followed by a one or two sentence direct answer.
            Then give the key points as short lines, each starting with "• ".
            Add a tiny example only if it really helps understanding.
        """.trimIndent()
    }
}
