package com.fadhil.financereceipt.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel = viewModel()) {
    val state by historyViewModel.uiState.collectAsStateWithLifecycle()
    val manageState by historyViewModel.manageState.collectAsStateWithLifecycle()
    var editTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteSummary by rememberSaveable { mutableStateOf("") }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = manageState.isWorking || manageState.completedMessage != null
    val actionsEnabled = !busy && editTransactionId == null &&
            deleteTransactionId == null && successMessage == null

    LaunchedEffect(manageState.completedMessage) {
        manageState.completedMessage?.let {
            editTransactionId = null
            deleteTransactionId = null
            successMessage = it
            historyViewModel.resetManageState()
        }
    }
    var selectedType by rememberSaveable { mutableStateOf("all") }
    val listState = rememberLazyListState()
    val visibleTransactions = remember(state.transactions, selectedType) {
        state.transactions.filter {
            selectedType == "all" || it.transaction.transactionType == selectedType
        }
    }
    LaunchedEffect(selectedType) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F9FB))) {
        Column(
            Modifier.fillMaxWidth().background(Color(0xFFEF0012)).padding(20.dp)
        ) {
            Text("Riwayat Transaksi", color = Color.White,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Catatan pemasukan dan pengeluaran Anda", color = Color.White,
                fontSize = 13.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all" to "Semua", "income" to "Pemasukan", "expense" to "Pengeluaran")
                .forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF0012),
                            selectedLabelColor = Color.White
                        )
                    )
                }
        }
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEF0012))
            }
            state.errorMessage != null -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.errorMessage.orEmpty(), color = Color(0xFF172B46))
                TextButton(onClick = historyViewModel::loadHistory) { Text("Coba lagi") }
            }
            visibleTransactions.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    when (selectedType) {
                        "income" -> "Belum ada transaksi pemasukan"
                        "expense" -> "Belum ada transaksi pengeluaran"
                        else -> "Belum ada transaksi"
                    }, color = Color(0xFF172B46), fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text("Tambahkan transaksi melalui tombol + di Beranda.",
                    color = Color(0xFF667A96), fontSize = 13.sp)
            }
            else -> {
                Text("${visibleTransactions.size} transaksi • Semua tanggal",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(0xFF667A96), fontSize = 12.sp)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleTransactions, key = { it.transaction.transactionId }) {
                        TransactionItem(
                            item = it,
                            actionsEnabled = actionsEnabled,
                            onEdit = {
                                historyViewModel.resetManageState()
                                editTransactionId = it.transaction.transactionId
                            },
                            onDelete = {
                                historyViewModel.resetManageState()
                                deleteTransactionId = it.transaction.transactionId
                                val locale = Locale.forLanguageTag("id-ID")
                                val date = SimpleDateFormat("dd MMMM yyyy", locale)
                                    .format(it.transaction.transactionDate)
                                val amount = NumberFormat.getIntegerInstance(locale)
                                    .format(it.transaction.amount)
                                val type = if (it.transaction.transactionType == "income")
                                    "Pemasukan" else "Pengeluaran"
                                deleteSummary = "$type • ${it.categoryEmoji} ${it.categoryName}\n$date\nRp$amount"
                            }
                        )
                    }
                }
            }
        }
    }

    val editing = state.transactions.firstOrNull { it.transaction.transactionId == editTransactionId }
    if (editTransactionId != null && editing != null) {
        TransactionEditDialog(
            transaction = editing.transaction,
            categories = state.categories,
            manageState = manageState,
            onDismiss = {
                editTransactionId = null
                historyViewModel.resetManageState()
            },
            onSave = { categoryId, type, amount, date, note ->
                historyViewModel.updateTransaction(
                    editing.transaction.transactionId, categoryId, type, amount, date, note
                )
            }
        )
    } else if (editTransactionId != null && !state.isLoading && !busy) {
        AlertDialog(
            onDismissRequest = { editTransactionId = null },
            title = { Text("Transaksi tidak tersedia") },
            text = { Text("Tutup form lalu coba muat ulang riwayat.") },
            confirmButton = { TextButton(onClick = {
                editTransactionId = null
                historyViewModel.resetManageState()
            }) { Text("Tutup") } }
        )
    }

    deleteTransactionId?.let { id ->
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    deleteTransactionId = null
                    historyViewModel.resetManageState()
                }
            },
            properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
            title = { Text("Hapus transaksi?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(deleteSummary, fontWeight = FontWeight.SemiBold)
                    Text("Transaksi ini akan dihapus permanen. Saldo dan penggunaan anggaran akan dihitung ulang.")
                    manageState.errorMessage?.let { Text(it, color = Color(0xFFEF0012)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyViewModel.deleteTransaction(id) }, enabled = !busy) {
                    Text(if (manageState.isWorking) "Menghapus..." else "Hapus", color = Color(0xFFEF0012))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteTransactionId = null
                    historyViewModel.resetManageState()
                }, enabled = !busy) { Text("Batal") }
            }
        )
    }

    successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { successMessage = null },
            title = { Text("Berhasil") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { successMessage = null }) { Text("Selesai") } }
        )
    }

}
