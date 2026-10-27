package com.example.aistuddybuddy.domain.repository

import android.graphics.Bitmap

interface OcrRepository {
    suspend fun extractText(image: Bitmap): Result<String>
}
