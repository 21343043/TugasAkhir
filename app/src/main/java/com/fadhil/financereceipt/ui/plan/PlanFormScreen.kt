package com.fadhil.financereceipt.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fadhil.financereceipt.data.local.entity.CategoryEntity

@Composable
fun PlanFormScreen(
    monthLabel: String,
    categories: List<CategoryEntity>,
    saveState: PlanSaveState,
    onSave: (Long?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    val locked = saveState.isSaving || saveState.savedPlanId != null
    val selectedCategory = categories.firstOrNull { it.categoryId == selectedCategoryId }

    AlertDialog(
        onDismissRequest = { if (!locked) onDismiss() },
        title = { Text("Tambah Plan") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(monthLabel)
                Text("Pilih kategori pengeluaran")
                if (categories.isEmpty()) {
                    Text("Kategori pengeluaran belum tersedia.")
                } else {
                    // Pemilih langsung; tidak perlu membuka dialog kedua.
                    categories.forEach { category ->
                        TextButton(
                            onClick = { selectedCategoryId = category.categoryId },
                            enabled = !locked,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                (if (selectedCategoryId == category.categoryId) "✓ " else "") +
                                        "${category.emoji} ${category.categoryName}",
                                color = if (selectedCategoryId == category.categoryId)
                                    Color(0xFFEF0012) else Color(0xFF172B46)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        if (value.length <= 18 && value.all { it in '0'..'9' }) amount = value
                    },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Batas anggaran") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Masukkan angka tanpa titik atau koma, contoh 1000000.")
                Text("Pengeluaran yang sudah tercatat pada kategori dan bulan ini ikut dihitung.")
                saveState.errorMessage?.let { Text(it, color = Color(0xFFB00020)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedCategory?.categoryId, amount) },
                enabled = !locked && categories.isNotEmpty()
            ) {
                Text(if (saveState.isSaving) "MENYIMPAN..." else "SIMPAN", color = Color(0xFFEF0012))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !locked) { Text("Batal") }
        },
        containerColor = Color.White
    )
}
