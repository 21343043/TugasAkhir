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

@Composable
fun PlanEditDialog(
    planId: Long,
    categoryLabel: String,
    monthLabel: String,
    initialAmount: String,
    state: PlanManageState,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by rememberSaveable(planId) { mutableStateOf(initialAmount) }
    val locked = state.isWorking || state.completedMessage != null
    AlertDialog(
        onDismissRequest = { if (!locked) onDismiss() },
        title = { Text("Edit batas anggaran") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(categoryLabel)
                Text(monthLabel)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        if (value.length <= 18 && value.all { it in '0'..'9' }) amount = value
                    },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Batas anggaran baru") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Masukkan nominal tanpa titik atau koma. Pengeluaran terpakai tetap dihitung dari transaksi bulan ini.")
                state.errorMessage?.let { Text(it, color = Color(0xFFB00020)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amount) }, enabled = !locked) {
                Text(if (state.isWorking) "MENYIMPAN..." else "Simpan", color = Color(0xFFEF0012))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !locked) { Text("Batal") }
        },
        containerColor = Color.White
    )
}
