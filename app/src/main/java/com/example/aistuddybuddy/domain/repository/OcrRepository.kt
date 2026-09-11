package com.example.aistuddybuddy.domain.repository

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

interface OcrRepository {
    suspend fun extractText(image: InputImage) : Result<String>
}

class OcrRepositoryImpl: OcrRepository {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractText(image: InputImage): Result<String> {
        return try {
            val result = recognizer.process(image).await()
            if(result.text.isBlank()) {
                Result.failure(Exception("Text not found"))
            } else {
                Result.success(result.text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}