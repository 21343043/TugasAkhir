package com.fadhil.financereceipt.ui.receipt

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptPreviewScreen(imagePath: String) {
    val bitmap by produceState<ImageBitmap?>(null, imagePath) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(imagePath)?.asImageBitmap() }.getOrNull()
        }
    }
    bitmap?.let { image ->
        Image(image, contentDescription = "Foto struk yang dipilih",
            modifier = Modifier.fillMaxWidth().height(320.dp), contentScale = ContentScale.Fit)
    } ?: Text("Menyiapkan pratinjau. Jika gambar tidak muncul, pilih ulang foto.")
}
