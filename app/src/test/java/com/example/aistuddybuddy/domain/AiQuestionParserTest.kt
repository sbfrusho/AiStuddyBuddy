package com.example.aistuddybuddy.domain

import com.example.aistuddybuddy.domain.parser.AiQuestionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiQuestionParserTest {

    @Test
    fun `parses MCQ and descriptive questions`() {
        val reply = """
            {"questions":[
              {"text":"What is the capital of Bangladesh?","options":["Dhaka","Rajshahi"],"needsFigure":false},
              {"text":"বাংলাদেশের জাতীয় ফুল কী?","options":[]}
            ]}
        """.trimIndent()

        val questions = AiQuestionParser.parse(reply)!!

        assertEquals(2, questions.size)
        assertEquals(listOf("Dhaka", "Rajshahi"), questions[0].question.options)
        assertEquals("বাংলাদেশের জাতীয় ফুল কী?", questions[1].question.text)
        assertTrue(questions[1].question.options.isEmpty())
    }

    @Test
    fun `strips code fences and surrounding text`() {
        val reply = "Here you go:\n```json\n{\"questions\":[{\"text\":\"Find x\",\"needsFigure\":true}]}\n```"

        val questions = AiQuestionParser.parse(reply)!!

        assertEquals("Find x", questions.single().question.text)
        assertTrue(questions.single().needsImage)
    }

    @Test
    fun `drops blank questions and options`() {
        val reply = """{"questions":[{"text":"  "},{"text":"Q","options":["a"," ",""]}]}"""

        val questions = AiQuestionParser.parse(reply)!!

        assertEquals(listOf("a"), questions.single().question.options)
    }

    @Test
    fun `returns null for non-JSON replies`() {
        assertNull(AiQuestionParser.parse("I can't read this image."))
    }
}
