package com.fadhil.financereceipt.ui.receipt

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReceiptResultScreen(
    state: ReceiptUiState, categories: List<CategoryEntity>, categoryError: String?,
    onRetryCategories: () -> Unit, onStore: (String) -> Unit, onDate: (Long) -> Unit,
    onAmount: (String) -> Unit, onCategory: (Long) -> Unit, onNote: (String) -> Unit,
    onSave: () -> Unit, onRescan: () -> Unit
) {
    val context = LocalContext.current
    var categoryOpen by remember { mutableStateOf(false) }
    var rawOpen by rememberSaveable { mutableStateOf(false) }
    var imageOpen by rememberSaveable { mutableStateOf(false) }
    var checked by rememberSaveable(state.storeName, state.dateMillis, state.amount, state.categoryId) {
        mutableStateOf(false)
    }
    val editable = !state.busy && state.savedId == null
    Text("Periksa hasil scan", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("Cocokkan hasil berikut dengan struk. Anda dapat memperbaiki setiap isian sebelum menyimpan.")
    state.result?.warnings?.forEach { warning ->
        Text("⚠️ $warning", color = MaterialTheme.colorScheme.error)
    }
    TextButton(onClick = { imageOpen = !imageOpen }) {
        Text(if (imageOpen) "Sembunyikan foto" else "🧾 Lihat foto struk")
    }
    if (imageOpen) state.imagePath?.let { ReceiptPreviewScreen(it) }
    OutlinedTextField(value = state.storeName, onValueChange = onStore,
        label = { Text("Nama toko") }, singleLine = true, enabled = editable,
        modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = {
        val initial = Calendar.getInstance().apply { state.dateMillis?.let { timeInMillis = it } }
        DatePickerDialog(context, { _, y, m, d ->
            onDate(Calendar.getInstance().apply { clear(); set(y, m, d, 12, 0, 0) }.timeInMillis)
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }, enabled = editable, modifier = Modifier.fillMaxWidth()) {
        Text(state.dateMillis?.let {
            "📅 " + SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(it)
        } ?: "📅 Pilih tanggal transaksi")
    }
    OutlinedTextField(value = state.amount, onValueChange = { value ->
        if (value.length <= 18 && value.all { it in '0'..'9' }) onAmount(value)
    }, label = { Text("Total belanja") }, prefix = { Text("Rp ") }, singleLine = true,
        supportingText = { Text("Rupiah utuh, tanpa titik atau koma. Contoh: 25000.") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = editable, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { categoryOpen = true },
        enabled = editable && categories.isNotEmpty() && categoryError == null,
        modifier = Modifier.fillMaxWidth()) {
        Text(categories.firstOrNull { it.categoryId == state.categoryId }?.let {
            "${it.emoji} ${it.categoryName}"
        } ?: "Pilih kategori pengeluaran")
    }
    if (categoryError != null) {
        Text(categoryError, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetryCategories, enabled = editable) { Text("Coba lagi") }
    } else if (categories.isEmpty()) {
        Text("Kategori belum tersedia. Jika tetap kosong, tambahkan kategori pengeluaran dari menu kategori.")
    }
    OutlinedTextField(value = state.note, onValueChange = onNote,
        label = { Text("Keterangan (opsional)") }, supportingText = { Text("Jika kosong, nama toko menjadi keterangan.") },
        minLines = 2, maxLines = 4, enabled = editable, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = { rawOpen = !rawOpen }) {
        Text(if (rawOpen) "Sembunyikan teks OCR" else "📄 Lihat teks OCR asli")
    }
    if (rawOpen) SelectionContainer {
        Text(state.rawText, modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState()), fontSize = 13.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { checked = it }, enabled = editable)
        Text("Saya sudah mencocokkan nama toko, tanggal, dan total dengan struk.", modifier = Modifier.weight(1f))
    }
    Button(onClick = onSave,
        enabled = editable && checked && categoryError == null,
        modifier = Modifier.fillMaxWidth()) { Text("💾 Simpan pengeluaran") }
    OutlinedButton(onClick = onRescan, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text("Pindai ulang") }
    if (categoryOpen) AlertDialog(onDismissRequest = { categoryOpen = false },
        title = { Text("Kategori pengeluaran") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                categories.forEach { category ->
                    TextButton(onClick = { onCategory(category.categoryId); categoryOpen = false },
                        modifier = Modifier.fillMaxWidth()) { Text("${category.emoji} ${category.categoryName}") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { categoryOpen = false }) { Text("Batal") } })
}
