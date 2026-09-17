package com.fadhil.financereceipt.ui.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fadhil.financereceipt.ui.history.ReceiptPhotoUiState
import kotlin.math.roundToInt

/** Penampil hanya membaca foto. Tidak mengubah transaksi, hasil OCR, atau berkas struk. */
@Composable
fun ReceiptPhotoDialog(
    state: ReceiptPhotoUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF101820)).safeDrawingPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Foto Struk", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Tutup", color = Color.White) }
            }
            val bitmap = state.bitmap
            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = Color.White)
                        Text("Memuat foto struk…", color = Color.White)
                    }
                }
                bitmap != null -> {
                    val image = remember(bitmap) { bitmap.asImageBitmap() }
                    var viewport by remember(bitmap) { mutableStateOf(IntSize.Zero) }
                    var scale by remember(bitmap, viewport) { mutableFloatStateOf(1f) }
                    var offset by remember(bitmap, viewport) { mutableStateOf(Offset.Zero) }

                    // Batas mengikuti ukuran foto setelah ContentScale.Fit, termasuk struk panjang.
                    fun boundedOffset(candidate: Offset, zoom: Float): Offset {
                        if (viewport.width == 0 || viewport.height == 0) return Offset.Zero
                        val fit = minOf(viewport.width.toFloat() / image.width,
                            viewport.height.toFloat() / image.height)
                        val maxX = ((image.width * fit * zoom - viewport.width) / 2f).coerceAtLeast(0f)
                        val maxY = ((image.height * fit * zoom - viewport.height) / 2f).coerceAtLeast(0f)
                        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
                    }

                    fun zoomTo(requested: Float, anchor: Offset = Offset.Zero, pan: Offset = Offset.Zero) {
                        val next = requested.coerceIn(1f, 6f)
                        offset = boundedOffset((offset - anchor) * (next / scale) + anchor + pan, next)
                        scale = next
                    }

                    Box(
                        Modifier.fillMaxWidth().weight(1f).clipToBounds()
                            .onSizeChanged { viewport = it }
                            .pointerInput(bitmap, viewport) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                                    zoomTo(scale * zoom, centroid - center, pan)
                                }
                            }
                            .pointerInput(bitmap, viewport) {
                                detectTapGestures(onDoubleTap = { tap ->
                                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                                    zoomTo(if (scale > 1f) 1f else 3f, tap - center)
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = image,
                            contentDescription = "Foto struk transaksi tersimpan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { zoomTo(scale / 1.5f) }, enabled = scale > 1f,
                            modifier = Modifier.semantics { contentDescription = "Perkecil foto" }) {
                            Text("−", color = if (scale > 1f) Color.White else Color.Gray)
                        }
                        Text("${(scale * 100).roundToInt()}%", color = Color.White)
                        TextButton(onClick = { zoomTo(scale * 1.5f) }, enabled = scale < 6f,
                            modifier = Modifier.semantics { contentDescription = "Perbesar foto" }) {
                            Text("+", color = if (scale < 6f) Color.White else Color.Gray)
                        }
                        TextButton(onClick = { zoomTo(1f) }) { Text("Reset", color = Color.White) }
                    }
                    Text(
                        "Cubit untuk memperbesar, lalu geser foto. Ketuk dua kali untuk zoom/reset.",
                        color = Color(0xFFCBD5E1), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }
                else -> Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(state.errorMessage ?: "Foto struk tidak tersedia.",
                            color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = onRetry) { Text("Coba lagi") }
                    }
                }
            }
        }
    }
}
