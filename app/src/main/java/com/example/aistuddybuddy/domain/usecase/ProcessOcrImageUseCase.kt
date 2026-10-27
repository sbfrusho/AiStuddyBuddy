package com.example.aistuddybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.repository.OcrRepository

class ProcessOcrImageUseCase(
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(image: Bitmap): Result<String> {
        return ocrRepository.extractText(image)
    }
}
