package com.fadhil.financereceipt.ui.transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val FormRed = Color(0xFFEF0012)
private val FormInk = Color(0xFF172B46)

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    var income by rememberSaveable { mutableStateOf(false) }
    var dateMillis by rememberSaveable { mutableLongStateOf(Calendar.getInstance().timeInMillis) }
    var selectedCategoryId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var categoryOpen by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val categoryState by
    transactionViewModel.categoryState.collectAsStateWithLifecycle()

    val transactionType = if (income) "income" else "expense"

    val categories = categoryState.categories.filter {
        it.transactionType == transactionType
    }

    val selectedCategory = categories.firstOrNull {
        it.categoryId == selectedCategoryId
    }
    val validAmount = amount.toLongOrNull()?.let { it > 0L } == true
    val dateText = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(dateMillis)

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F9FB)).imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().background(FormRed).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Kembali", color = Color.White) }
            Text("Buat Transaksi", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp), color = Color.White,
            shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(false, true).forEach { isIncome ->
                        Button(
                            onClick = {
                                if (income != isIncome) {
                                    income = isIncome
                                    selectedCategoryId = null
                                    categoryOpen = false
                                    submitted = false
                                }
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (income == isIncome) { if (isIncome) Color(0xFF00B982) else FormRed } else Color(0xFFF1F4F8),
                                contentColor = if (income == isIncome) Color.White else Color(0xFF667A96)
                            )
                        ) { Text(if (isIncome) "Pemasukan" else "Pengeluaran", fontSize = 12.sp) }
                    }
                }
                Column {
                    FormLabel("TANGGAL")
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        onClick = {
                            val initial = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            DatePickerDialog(context, { _, year, month, day ->
                                dateMillis = Calendar.getInstance().apply {
                                    clear(); set(year, month, day, 12, 0, 0)
                                }.timeInMillis
                            }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
                        }
                    ) { Text("$dateText   ·   Pilih tanggal", color = FormInk) }
                }
                Column {
                    FormLabel("KATEGORI")

                    OutlinedButton(
                        onClick = { categoryOpen = true },
                        enabled = !categoryState.isLoading &&
                                categoryState.errorMessage == null &&
                                categories.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = when {
                                categoryState.isLoading -> "Memuat kategori..."
                                selectedCategory != null ->
                                    "${selectedCategory.emoji} ${selectedCategory.categoryName}"
                                else -> "Pilih kategori"
                            },
                            color = FormInk
                        )
                    }

                    categoryState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = FormRed,
                            fontSize = 12.sp
                        )

                        TextButton(
                            onClick = { transactionViewModel.loadCategories() }
                        ) {
                            Text("Coba lagi", color = FormRed)
                        }
                    }

                    if (
                        !categoryState.isLoading &&
                        categoryState.errorMessage == null &&
                        categories.isEmpty()
                    ) {
                        Text(
                            text = "Belum ada kategori untuk jenis transaksi ini.",
                            color = FormRed,
                            fontSize = 12.sp
                        )
                    }

                    if (
                        submitted &&
                        !categoryState.isLoading &&
                        categoryState.errorMessage == null &&
                        categories.isNotEmpty() &&
                        selectedCategory == null
                    ) {
                        Text(
                            text = "Pilih kategori terlebih dahulu.",
                            color = FormRed,
                            fontSize = 12.sp
                        )
                    }
                }
                Column {
                    FormLabel("JUMLAH")
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value -> if (value.all { it in '0'..'9' } && value.length <= 18) amount = value },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        prefix = { Text("Rp ") }, placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = submitted && !validAmount, shape = RoundedCornerShape(14.dp)
                    )
                    if (submitted && !validAmount) Text("Nominal harus lebih dari nol.", color = FormRed, fontSize = 12.sp)
                    Text("Masukkan angka tanpa titik atau koma, contoh 25000.", color = Color(0xFF667A96), fontSize = 11.sp)
                }
                Column {
                    FormLabel("KETERANGAN")
                    OutlinedTextField(value = note, onValueChange = { if (it.length <= 500) note = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                        placeholder = { Text("Tambah catatan... (opsional)") }, shape = RoundedCornerShape(14.dp))
                }
                if (!income) {
                    OutlinedButton(onClick = { message = "Fitur scan struk belum tersedia." }, modifier = Modifier.fillMaxWidth()) {
                        Text("SCAN STRUK", color = FormInk)
                    }
                }
                Button(
                    onClick = {
                        submitted = true
                        if (
                            !categoryState.isLoading &&
                            categoryState.errorMessage == null &&
                            selectedCategory != null &&
                            validAmount
                        ) {
                            message = "Isian sudah valid. Transaksi belum disimpan karena penyimpanan belum tersedia."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FormRed, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("SIMPAN", fontWeight = FontWeight.Bold) }
                Text("Pratinjau form. Data belum tersimpan.", color = Color(0xFF667A96), fontSize = 12.sp)
            }
        }
    }
    if (categoryOpen) {
        AlertDialog(
            onDismissRequest = { categoryOpen = false },
            title = { Text("Pilih kategori") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    categories.forEach { item ->
                        TextButton(
                            onClick = {
                                selectedCategoryId = item.categoryId
                                categoryOpen = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${item.emoji} ${item.categoryName}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { categoryOpen = false }) {
                    Text("Batal")
                }
            }
        )
    }
    message?.let { info ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Informasi") }, text = { Text(info) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Mengerti") } })
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, color = Color(0xFF667A96), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
}
