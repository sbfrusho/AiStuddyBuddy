package com.example.aistuddybuddy.domain

import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.parser.OcrQuality
import com.example.aistuddybuddy.domain.parser.QuestionParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrQualityTest {

    private fun reliable(ocrText: String, confidence: Float? = 0.95f) =
        OcrQuality.isReliable(QuestionParser.parse(ocrText), confidence)

    @Test
    fun `clear English MCQ is reliable`() {
        assertTrue(reliable("Q. What is the capital of Bangladesh?\na. Dhaka b. Rajshahi c. Chittagong d. Sylhet"))
    }

    @Test
    fun `clear English descriptive question is reliable`() {
        assertTrue(reliable("1. Write a C program to find the LCM of two numbers using goto statement."))
    }

    @Test
    fun `Bangla misread as Latin is not reliable`() {
        // Real OCR output from a Bangla MCQ page.
        assertFalse(reliable("b. aJ d. qa faT"))
        assertFalse(reliable("a. fas 1S3 c. qg#egi d. aAReg EE"))
        assertFalse(reliable("a. af b. t c. Q9 d. f"))
    }

    @Test
    fun `math equation is sent to AI`() {
        assertFalse(reliable("Solve x2 + 5x + 6 = 0"))
    }

    @Test
    fun `low recognizer confidence is not reliable`() {
        assertFalse(reliable("Q. What is the capital of Bangladesh?", confidence = 0.4f))
    }

    @Test
    fun `missing confidence falls back to the text check`() {
        assertTrue(reliable("Q. What is the capital of Bangladesh?", confidence = null))
    }

    @Test
    fun `empty question text is not reliable`() {
        assertFalse(OcrQuality.isReliable(listOf(Question("", listOf("Dhaka", "Rajshahi"))), 0.95f))
        assertFalse(OcrQuality.isReliable(emptyList(), 0.95f))
    }
}
