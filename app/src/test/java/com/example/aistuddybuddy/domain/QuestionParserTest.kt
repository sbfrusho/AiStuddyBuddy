package com.example.aistuddybuddy.domain

import com.example.aistuddybuddy.domain.model.QuestionType
import com.example.aistuddybuddy.domain.parser.PromptBuilder
import com.example.aistuddybuddy.domain.parser.QuestionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionParserTest {

    @Test
    fun `inline MCQ options are split`() {
        val questions = QuestionParser.parse(
            "Q. What is the capital of Bangladesh?\na. Dhaka b. Rajshahi c. Chittagong d. Sylhet"
        )

        assertEquals(1, questions.size)
        val question = questions.single()
        assertEquals("What is the capital of Bangladesh?", question.text)
        assertEquals(listOf("Dhaka", "Rajshahi", "Chittagong", "Sylhet"), question.options)
        assertEquals(QuestionType.MCQ, question.type)
    }

    @Test
    fun `options on separate lines with parentheses are detected`() {
        val question = QuestionParser.parse(
            """
            1. Which gas do plants absorb
            during photosynthesis?
            (a) Oxygen
            (b) Carbon dioxide
            (c) Nitrogen
            (d) Hydrogen
            """.trimIndent()
        ).single()

        assertEquals("Which gas do plants absorb during photosynthesis?", question.text)
        assertEquals(listOf("Oxygen", "Carbon dioxide", "Nitrogen", "Hydrogen"), question.options)
    }

    @Test
    fun `descriptive question has no options`() {
        val question = QuestionParser.parse("Q. Explain IPv4?").single()

        assertEquals("Explain IPv4?", question.text)
        assertTrue(question.options.isEmpty())
        assertEquals(QuestionType.DESCRIPTIVE, question.type)
    }

    @Test
    fun `numbered questions are split into separate questions`() {
        val questions = QuestionParser.parse(
            """
            1. What is an IP address?
            2) Explain the OSI model.
            Q3. Which layer handles routing?
            A) Physical B) Network C) Session
            """.trimIndent()
        )

        assertEquals(3, questions.size)
        assertEquals("What is an IP address?", questions[0].text)
        assertEquals("Explain the OSI model.", questions[1].text)
        assertEquals("Which layer handles routing?", questions[2].text)
        assertEquals(listOf("Physical", "Network", "Session"), questions[2].options)
    }

    @Test
    fun `text without numbering is kept as one question`() {
        val question = QuestionParser.parse("Describe the water cycle\nwith a diagram.").single()

        assertEquals("Describe the water cycle with a diagram.", question.text)
    }

    @Test
    fun `stray letters inside text are not treated as options`() {
        val question = QuestionParser.parse("Q. What does Vitamin A. do for the eyes?").single()

        assertTrue(question.options.isEmpty())
    }

    @Test
    fun `decimal numbers do not start a new question`() {
        val questions = QuestionParser.parse("1. Convert the value\n2.5 kg into grams.")

        assertEquals(1, questions.size)
        assertEquals("Convert the value 2.5 kg into grams.", questions.single().text)
    }

    @Test
    fun `MCQ prompt lists lettered options`() {
        val question = QuestionParser.parse("Q. Capital of Bangladesh?\na. Dhaka b. Sylhet").single()
        val prompt = PromptBuilder.userPrompt(question)

        assertTrue(prompt.contains("a. Dhaka\nb. Sylhet"))
        assertTrue(prompt.contains("Ans: <letter>. <option text>"))
        assertTrue(prompt.lines().none { it.startsWith(" ") })
    }
}
