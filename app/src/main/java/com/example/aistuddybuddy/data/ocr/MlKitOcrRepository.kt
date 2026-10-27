package com.example.aistuddybuddy.data.ocr

import android.graphics.Bitmap
import com.example.aistuddybuddy.domain.model.NoTextFoundException
import com.example.aistuddybuddy.domain.repository.OcrRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** On-device OCR using ML Kit's bundled Latin text recognizer. */
class MlKitOcrRepository : OcrRepository {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractText(image: Bitmap): Result<String> {
        return try {
            val result = recognizer.process(InputImage.fromBitmap(image, 0)).await()
            // Rebuild the text line by line so that layout (one option per line, etc.) is preserved.
            val text = result.textBlocks
                .flatMap { block -> block.lines }
                .joinToString("\n") { line -> line.text }
            if (text.isBlank()) {
                Result.failure(NoTextFoundException())
            } else {
                Result.success(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
