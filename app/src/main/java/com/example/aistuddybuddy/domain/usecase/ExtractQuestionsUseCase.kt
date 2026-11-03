package com.example.aistuddybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.parser.OcrQuality
import com.example.aistuddybuddy.domain.parser.QuestionParser

/** [isReliable] is false when the text looks misread, e.g. Bangla or math read as Latin letters. */
data class OcrQuestions(
    val questions: List<Question>,
    val isReliable: Boolean
)

class ExtractQuestionsUseCase(
    private val processOcrImage: ProcessOcrImageUseCase
) {
    suspend operator fun invoke(image: Bitmap): Result<OcrQuestions> {
        return processOcrImage(image).map { ocr ->
            val questions = QuestionParser.parse(ocr.text)
            OcrQuestions(questions, isReliable = OcrQuality.isReliable(questions, ocr.confidence))
        }
    }
}
