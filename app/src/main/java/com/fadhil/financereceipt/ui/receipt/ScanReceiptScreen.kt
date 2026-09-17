package com.fadhil.financereceipt.ui.receipt

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ScanReceiptScreen(onBack: () -> Unit, onSaved: () -> Unit,
                      receiptViewModel: ReceiptViewModel = viewModel()) {
    val state by receiptViewModel.state.collectAsStateWithLifecycle()
    val categories by receiptViewModel.categories.collectAsStateWithLifecycle()
    val categoryError by receiptViewModel.categoryError.collectAsStateWithLifecycle()
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { receiptViewModel.selectImage(it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingCamera?.let {
            val uri = Uri.parse(it)
            if (success) receiptViewModel.selectImage(uri, fromCamera = true)
            else receiptViewModel.images.deleteCamera(uri)
        }
        pendingCamera = null
    }
    val back = {
        if (!state.busy) {
            if (state.savedId != null) onSaved()
            else if (state.result != null) discard = true
            else onBack()
        }
    }
    BackHandler { back() }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F9FB)).imePadding()) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFEF0012)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back, enabled = !state.busy) { Text("‹ Kembali", color = Color.White) }
            Text("🧾 Scan Struk", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.result == null) {
                Text("Catat belanja dari struk", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Foto seluruh struk dengan jelas, hindari bayangan, dan pastikan tulisan tegak.")
                Button(onClick = {
                    try {
                        val uri = receiptViewModel.images.createCameraUri()
                        pendingCamera = uri.toString()
                        camera.launch(uri)
                    } catch (_: Exception) {
                        pendingCamera?.let { receiptViewModel.images.deleteCamera(Uri.parse(it)) }
                        pendingCamera = null
                        receiptViewModel.showError("Kamera tidak dapat dibuka. Anda dapat memilih foto dari galeri.")
                    }
                }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("📷 Ambil foto") }
                OutlinedButton(onClick = {
                    try { gallery.launch("image/*") }
                    catch (_: Exception) { receiptViewModel.showError("Galeri tidak dapat dibuka di perangkat ini.") }
                }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("🖼️ Pilih dari galeri") }
                state.imagePath?.let { path ->
                    ReceiptPreviewScreen(path)
                    OutlinedButton(onClick = receiptViewModel::rotateImage, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("↻ Putar foto 90°") }
                    Button(onClick = receiptViewModel::processImage, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("🔎 Baca struk") }
                }
            } else {
                ReceiptResultScreen(state, categories, categoryError,
                    onRetryCategories = receiptViewModel::loadCategories,
                    onStore = { receiptViewModel.edit(storeName = it) },
                    onDate = { receiptViewModel.edit(dateMillis = it) },
                    onAmount = { receiptViewModel.edit(amount = it) },
                    onCategory = { receiptViewModel.edit(categoryId = it) },
                    onNote = { receiptViewModel.edit(note = it) },
                    onSave = receiptViewModel::save,
                    onRescan = { discard = true })
            }
            if (state.busy) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(state.busyLabel)
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false },
        title = { Text("Ulangi pemeriksaan struk?") },
        text = { Text("Koreksi yang belum disimpan akan dihapus. Anda dapat membaca ulang atau mengganti foto.") },
        confirmButton = { TextButton(onClick = {
            discard = false
            receiptViewModel.backToPreview()
        }) { Text("Ulangi") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Lanjutkan mengisi") } })
    if (state.savedId != null) AlertDialog(onDismissRequest = onSaved,
        title = { Text("✅ Pengeluaran tersimpan") },
        text = { Text("Transaksi sudah masuk ke riwayat dan perhitungan plan keuangan sesuai kategori dan tanggalnya.") },
        confirmButton = { TextButton(onClick = onSaved) { Text("Selesai") } })
}
