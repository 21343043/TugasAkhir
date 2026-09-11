package com.fadhil.financereceipt.ui.history

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TransactionEditDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    manageState: TransactionManageState,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, Long, String) -> Unit
) {
    val context = LocalContext.current
    val id = transaction.transactionId
    var type by rememberSaveable(id) { mutableStateOf(transaction.transactionType) }
    var categoryId by rememberSaveable(id) { mutableStateOf<Long?>(transaction.categoryId) }
    var amount by rememberSaveable(id) { mutableStateOf(transaction.amount.toString()) }
    var dateMillis by rememberSaveable(id) { mutableLongStateOf(transaction.transactionDate) }
    var note by rememberSaveable(id) { mutableStateOf(transaction.note) }
    var categoryOpen by rememberSaveable(id) { mutableStateOf(false) }
    var dateOpen by rememberSaveable(id) { mutableStateOf(false) }
    var submitted by rememberSaveable(id) { mutableStateOf(false) }
    val busy = manageState.isWorking || manageState.completedMessage != null
    val availableCategories = categories.filter { it.transactionType == type }
    val selectedCategory = availableCategories.firstOrNull { it.categoryId == categoryId }
    val validAmount = amount.toLongOrNull()?.let { it > 0L } == true
    val red = Color(0xFFEF0012)
    val dateText = remember(dateMillis) {
        SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(dateMillis)
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        containerColor = Color.White,
        title = { Text("Edit Transaksi", color = Color(0xFF172B46)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("expense" to "Pengeluaran", "income" to "Pemasukan").forEach { (value, label) ->
                        Button(
                            onClick = {
                                if (type != value) {
                                    type = value
                                    categoryId = null
                                    categoryOpen = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == value) {
                                    if (value == "income") Color(0xFF00875F) else red
                                } else Color(0xFFF1F4F8),
                                contentColor = if (type == value) Color.White else Color(0xFF667A96)
                            )
                        ) { Text(label, fontSize = 12.sp) }
                    }
                }
                Text("Tanggal")
                OutlinedButton(
                    onClick = { dateOpen = true }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("$dateText · Ubah") }

                Column {
                    Text("Kategori")
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryOpen = true },
                            enabled = !busy && availableCategories.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(selectedCategory?.let { "${it.emoji} ${it.categoryName}" } ?: "Pilih kategori")
                        }
                        DropdownMenu(
                            expanded = categoryOpen && !busy,
                            onDismissRequest = { categoryOpen = false },
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text("${category.emoji} ${category.categoryName}") },
                                    onClick = {
                                        categoryId = category.categoryId
                                        categoryOpen = false
                                    }
                                )
                            }
                        }
                    }
                    if (availableCategories.isEmpty()) {
                        Text("Belum ada kategori untuk jenis transaksi ini.", color = red, fontSize = 12.sp)
                    } else if (submitted && selectedCategory == null) {
                        Text("Pilih kategori yang sesuai dengan jenis transaksi.", color = red, fontSize = 12.sp)
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        if (value.length <= 19 && value.all { it in '0'..'9' }) amount = value
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Nominal") }, prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = submitted && !validAmount,
                    supportingText = {
                        Text(if (submitted && !validAmount) "Masukkan nominal yang valid dan lebih dari nol."
                        else "Angka tanpa titik atau koma, contoh 25000.")
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 500) note = it },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                    label = { Text("Keterangan (opsional)") },
                    supportingText = { Text("${note.length}/500 karakter") },
                    shape = RoundedCornerShape(14.dp)
                )
                manageState.errorMessage?.let { Text(it, color = red) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    if (selectedCategory != null && validAmount) {
                        onSave(categoryId, type, amount, dateMillis, note)
                    }
                },
                enabled = !busy && availableCategories.isNotEmpty()
            ) { Text(if (manageState.isWorking) "Menyimpan..." else "Simpan", color = red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Batal") }
        }
    )

    if (dateOpen && !busy) {
        DisposableEffect(id, context) {
            val initial = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val picker = DatePickerDialog(context, { _, year, month, day ->
                // Jam lokal tengah hari menghindari pergeseran tanggal akibat konversi UTC.
                dateMillis = Calendar.getInstance().apply {
                    clear()
                    set(year, month, day, 12, 0, 0)
                }.timeInMillis
                dateOpen = false
            }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
            picker.setOnCancelListener { dateOpen = false }
            picker.show()
            onDispose { picker.dismiss() }
        }
    }
}
