package com.example.aistuddybuddy.domain.parser

import com.example.aistuddybuddy.domain.model.AnswerLanguage
import com.example.aistuddybuddy.domain.model.AnswerStyle
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.model.QuestionType
import com.example.aistuddybuddy.domain.model.optionLabel

object PromptBuilder {

    val systemPrompt = """
        You are StudyBuddy, a friendly tutor helping a student who is reading a textbook.
        Explain in simple, clear language that a student understands on the first read.
        Be accurate. Keep answers focused: not too long, not too short.
        Write plain text only. Do not use markdown symbols such as **, # or tables.
        The one exception is program code: always put code inside a ``` block, never anywhere else.
        The question may have been read from a photo and may contain small recognition errors;
        silently interpret the intended question.
    """.trimIndent()

    fun userPrompt(
        question: Question,
        style: AnswerStyle = AnswerStyle.STANDARD,
        language: AnswerLanguage = AnswerLanguage.ENGLISH,
        hasImage: Boolean = false
    ): String {
        val body = when (question.type) {
            QuestionType.MCQ -> mcqPrompt(question, style)
            QuestionType.DESCRIPTIVE -> descriptivePrompt(question, style)
        }
        val extras = listOfNotNull(
            "The attached image shows the figure, diagram or table the question refers to.".takeIf { hasImage },
            languageInstruction(language)
        )
        return (listOf(body) + extras).joinToString("\n\n")
    }

    private fun mcqPrompt(question: Question, style: AnswerStyle): String {
        val options = question.options
            .filter { it.isNotBlank() }
            .mapIndexed { index, option -> "${optionLabel(index)}. $option" }
            .joinToString("\n")
        val why = when (style) {
            AnswerStyle.STANDARD -> "Why: <one or two short sentences explaining why it is correct>"
            AnswerStyle.SIMPLER -> "Why: <explain in very simple everyday words, as if to a 12-year-old, in two or three short sentences>"
            AnswerStyle.EXAMPLE -> "Why: <one short sentence>\nExample: <a short real-life or worked example that makes it click>"
            AnswerStyle.DETAILED -> "Why: <three to five sentences explaining the concept>\n" +
                "Other options: <one short line per wrong option saying why it is wrong>"
        }
        // Options are spliced in after trimIndent() so their line breaks don't skew the indent.
        return """
            This is a multiple-choice question.

            Question: ${question.text}
            Options:
            %OPTIONS%

            Reply in exactly this format and nothing else:
            Ans: <letter>. <option text>
            %WHY%
        """.trimIndent().replace("%OPTIONS%", options).replace("%WHY%", why)
    }

    private fun descriptivePrompt(question: Question, style: AnswerStyle): String {
        val instructions = when (style) {
            AnswerStyle.STANDARD -> """
                Answer in about 80 to 150 words.
                Start with "Ans: " followed by a one or two sentence direct answer.
                Then give the key points as short lines, each starting with "• ".
                Add a tiny example only if it really helps understanding.
            """
            AnswerStyle.SIMPLER -> """
                Answer in about 50 to 80 words using very simple everyday words, as if to a 12-year-old.
                Start with "Ans: " followed by a one sentence direct answer.
                Then give at most three short lines, each starting with "• ".
            """
            AnswerStyle.EXAMPLE -> """
                Start with "Ans: " followed by a one or two sentence direct answer.
                Then start a line with "Example: " and give a clear real-life or step-by-step worked example.
                Keep it under 150 words.
            """
            AnswerStyle.DETAILED -> """
                Answer in about 200 to 300 words, as a student would write it in an exam.
                Start with "Ans: " followed by a one or two sentence direct answer.
                Then explain the key points as short lines, each starting with "• ".
                End with a line starting with "Example: " if an example helps.
            """
        }.trimIndent()
        return "Question: ${question.text}\n\n$instructions\n\n$CODE_INSTRUCTION"
    }

    private const val CODE_INSTRUCTION =
        "If the question asks you to write a program or code, the word limit doesn't apply to the code: " +
            "after the \"Ans: \" line give the complete, working program in one ``` block, following every " +
            "requirement in the question (for example, a specific statement or loop it asks you to use). " +
            "Then explain how it works in two to four short lines starting with \"• \", and show a sample " +
            "input and output on a line starting with \"Example: \"."

    private fun languageInstruction(language: AnswerLanguage): String? = when (language) {
        AnswerLanguage.ENGLISH -> null
        AnswerLanguage.BANGLA ->
            "Write the whole answer in Bangla (বাংলা). Keep the labels Ans:, Why:, Example: and the option letters in English."
        AnswerLanguage.BOTH ->
            "First write the answer in English. Then add a line \"বাংলা:\" and repeat the same answer in Bangla."
    }

    val readImageSystemPrompt = """
        You read photos of textbook and exam pages for a student and copy out the questions exactly.
        You never answer the questions.
    """.trimIndent()

    val readImagePrompt = """
        Find every question in this image and reply with JSON only, in exactly this shape:
        {"questions":[{"text":"...","options":["...","..."],"needsFigure":false}]}

        Rules:
        - Copy each question in its original language. Bangla stays Bangla.
        - Remove the question number (1., Q2, ৩. and so on) from "text".
        - For multiple-choice questions put each option in "options" without its label (a., (b), C), ক., খ) and so on).
          Use an empty list for other questions.
        - Write math as plain text: x^2, 3/4, sqrt(2), H2O, ≤, ≥, π.
        - Set "needsFigure" to true only if answering needs a diagram, graph, table or picture shown in the image.
        - Ignore page numbers, headings and text that is not part of a question.
        - If there is no question, reply {"questions":[]}.
    """.trimIndent()
}
