package com.example.aistuddybuddy.domain.usecase

import com.example.aistuddybuddy.domain.repository.OcrRepository
import com.google.mlkit.vision.common.InputImage

class ProcessOcrImageUseCase (
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(image: InputImage): Result<String> {
        return ocrRepository.extractText(image)
    }
}