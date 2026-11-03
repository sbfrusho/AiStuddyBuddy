package com.example.aistuddybuddy.domain.repository

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.OcrText

interface OcrRepository {
    suspend fun extractText(image: Bitmap): Result<OcrText>
}
