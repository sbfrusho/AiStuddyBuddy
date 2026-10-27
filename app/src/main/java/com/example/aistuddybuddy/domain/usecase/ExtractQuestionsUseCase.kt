package com.example.aistuddybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.parser.QuestionParser

class ExtractQuestionsUseCase(
    private val processOcrImage: ProcessOcrImageUseCase
) {
    suspend operator fun invoke(image: Bitmap): Result<List<Question>> {
        return processOcrImage(image).map { text -> QuestionParser.parse(text) }
    }
}
