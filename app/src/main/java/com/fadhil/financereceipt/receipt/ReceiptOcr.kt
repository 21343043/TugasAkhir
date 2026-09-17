package com.fadhil.financereceipt.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ReceiptOcrResult(val rawText: String, val readingOrderText: String)

class ReceiptOcr(private val context: Context) {
    suspend fun recognize(path: String): ReceiptOcrResult {
        val input = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, Uri.fromFile(File(path)))
        }
        return suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                recognizer.process(input)
                    .addOnSuccessListener { result ->
                        // ML Kit dapat memisahkan label dan harga ke blok berbeda.
                        // Gabungkan baris yang sejajar secara vertikal, lalu urutkan kiri ke kanan.
                        val lines = result.textBlocks.flatMap { it.lines }
                            .sortedBy { it.boundingBox?.centerY() ?: Int.MAX_VALUE }
                        val rows = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.Line>>()
                        lines.forEach { line ->
                            val box = line.boundingBox
                            val row = rows.lastOrNull()
                            val anchor = row?.firstOrNull()?.boundingBox
                            if (row != null && box != null && anchor != null &&
                                kotlin.math.abs(box.centerY() - anchor.centerY()) <=
                                minOf(box.height(), anchor.height()) * 0.45f) {
                                row.add(line)
                            } else rows.add(mutableListOf(line))
                        }
                        val ordered = rows.joinToString("\n") { row ->
                            row.sortedBy { it.boundingBox?.left ?: 0 }.joinToString(" ") { it.text }
                        }
                        if (continuation.isActive) continuation.resume(ReceiptOcrResult(result.text, ordered))
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                    .addOnCanceledListener { continuation.cancel() }
                    .addOnCompleteListener { recognizer.close() }
            } catch (error: Exception) {
                recognizer.close()
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }
}
