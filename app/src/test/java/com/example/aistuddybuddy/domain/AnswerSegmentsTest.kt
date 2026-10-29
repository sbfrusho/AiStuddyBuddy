package com.example.aistuddybuddy.domain

import com.example.aistuddybuddy.domain.parser.AnswerSegment
import com.example.aistuddybuddy.domain.parser.AnswerSegments
import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerSegmentsTest {

    private val answer = """
        Ans: This program finds the LCM using goto.
        ```c
        #include <stdio.h>
        int main() {
            int a = 4, b = 6, lcm;
            lcm = a > b ? a : b;
        check:
            if (lcm % a != 0 || lcm % b != 0) { lcm++; goto check; }
            printf("%d", lcm);
            return 0;
        }
        ```
        • goto jumps back to check until lcm divides both.
    """.trimIndent()

    @Test
    fun `splits code from text and keeps code verbatim`() {
        val segments = AnswerSegments.split(answer)

        assertEquals(3, segments.size)
        assertEquals(false, segments[0].isCode)
        assertEquals(true, segments[1].isCode)
        assertEquals("#include <stdio.h>", segments[1].text.lines().first())
        assertEquals("• goto jumps back to check until lcm divides both.", segments[2].text)
    }

    @Test
    fun `unclosed fence runs to the end`() {
        val segments = AnswerSegments.split("Ans: code\n```\nint x;\nint **p;")

        assertEquals(AnswerSegment("int x;\nint **p;", isCode = true), segments.last())
    }

    @Test
    fun `plain text drops fence lines`() {
        val plain = AnswerSegments.plainText(answer)

        assertEquals(false, plain.contains("```"))
        assertEquals(true, plain.contains("#include <stdio.h>"))
    }
}
