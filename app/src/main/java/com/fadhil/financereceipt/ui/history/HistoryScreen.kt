package com.fadhil.financereceipt.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadhil.financereceipt.ui.receipt.ReceiptPhotoDialog
import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import com.fadhil.financereceipt.ui.home.homePeriod
import com.fadhil.financereceipt.ui.home.shiftHomePeriod
import java.util.Calendar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel = viewModel()) {
    val state by historyViewModel.uiState.collectAsStateWithLifecycle()
    val manageState by historyViewModel.manageState.collectAsStateWithLifecycle()
    val receiptPhotoState by historyViewModel.receiptPhotoState.collectAsStateWithLifecycle()
    var viewingReceiptId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteSummary by rememberSaveable { mutableStateOf("") }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = manageState.isWorking || manageState.completedMessage != null
    val actionsEnabled = !busy && editTransactionId == null &&
            deleteTransactionId == null && successMessage == null && viewingReceiptId == null

    LaunchedEffect(viewingReceiptId) {
        val id = viewingReceiptId
        if (id == null) historyViewModel.closeReceiptPhoto()
        else historyViewModel.loadReceiptPhoto(id)
    }
    DisposableEffect(historyViewModel) {
        onDispose { historyViewModel.closeReceiptPhoto() }
    }

    LaunchedEffect(manageState.completedMessage) {
        manageState.completedMessage?.let {
            editTransactionId = null
            deleteTransactionId = null
            successMessage = it
            historyViewModel.resetManageState()
        }
    }
    HistoryContent(
        state = state,
        actionsEnabled = actionsEnabled,
        onRetry = historyViewModel::loadHistory,
        onViewReceipt = { viewingReceiptId = it.transaction.transactionId },
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
            val amount = NumberFormat.getIntegerInstance(locale).format(it.transaction.amount)
            val type = if (it.transaction.transactionType == "income") "Pemasukan" else "Pengeluaran"
            deleteSummary = "$type • ${it.categoryEmoji} ${it.categoryName}\n$date\nRp$amount"
        }
    )

    val editing = state.transactions.firstOrNull { it.transaction.transactionId == editTransactionId }
    if (editTransactionId != null && editing != null) {
        TransactionEditDialog(
            transaction = editing.transaction,
            categories = state.categories,
            manageState = manageState,
            onViewReceipt = { viewingReceiptId = editing.transaction.transactionId },
            onDismiss = {
                if (viewingReceiptId == null) {
                    editTransactionId = null
                    historyViewModel.resetManageState()
                }
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

    // Tetap compose dialog edit di bawah penampil agar seluruh draf isian terjaga.
    viewingReceiptId?.let { id ->
        ReceiptPhotoDialog(
            state = if (receiptPhotoState.transactionId == id) receiptPhotoState
                else ReceiptPhotoUiState(transactionId = id, isLoading = true),
            onDismiss = {
                viewingReceiptId = null
                historyViewModel.closeReceiptPhoto()
            },
            onRetry = { historyViewModel.loadReceiptPhoto(id) }
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

/** Konten dipisahkan agar bisa dilihat di Compose Preview tanpa database/ViewModel. */
@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    actionsEnabled: Boolean = true,
    onRetry: () -> Unit = {},
    onViewReceipt: (TransactionWithCategory) -> Unit = {},
    onEdit: (TransactionWithCategory) -> Unit = {},
    onDelete: (TransactionWithCategory) -> Unit = {},
    initialAnchorMillis: Long = System.currentTimeMillis()
) {
    var selectedType by rememberSaveable { mutableStateOf("all") }
    var selectedPeriod by rememberSaveable { mutableStateOf("Harian") }
    var anchorMillis by rememberSaveable { mutableLongStateOf(initialAnchorMillis) }
    val listState = rememberLazyListState()
    // Sama persis dengan Beranda: awal inklusif, akhir eksklusif, zona perangkat.
    val period = homePeriod(anchorMillis, selectedPeriod)
    val visibleTransactions = remember(
        state.transactions, selectedType, period.start, period.endExclusive
    ) {
        state.transactions.filter {
            it.transaction.transactionDate >= period.start &&
                it.transaction.transactionDate < period.endExclusive &&
                (selectedType == "all" || it.transaction.transactionType == selectedType)
        }.sortedWith(
            compareByDescending<TransactionWithCategory> { it.transaction.transactionDate }
                .thenByDescending { it.transaction.transactionId }
        )
    }
    // groupBy mempertahankan urutan daftar yang sudah diurutkan, terbaru dahulu.
    val dateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    val groups = visibleTransactions.groupBy { dateFormatter.format(it.transaction.transactionDate) }
    LaunchedEffect(selectedType, selectedPeriod, period.start) { listState.scrollToItem(0) }

    // Satu pemilik scroll: header/filter ikut dapat digulir pada layar pendek/font besar.
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(HistoryStyle.Background)
            .semantics { paneTitle = "Riwayat transaksi" },
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "history_header") {
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        HistoryStyle.Red,
                        RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    ).padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    "Riwayat Transaksi", color = Color.White,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(6.dp))
                Text("Telusuri keuangan, satu periode setiap waktu.", color = Color.White, fontSize = 13.sp)
            }
        }
        item(key = "history_period") {
            Box(Modifier.padding(horizontal = 16.dp)) {
                HistoryPeriodFilter(
                    selectedPeriod = selectedPeriod,
                    periodLabel = period.label,
                    onSelectPeriod = { selectedPeriod = it },
                    onPrevious = { anchorMillis = shiftHomePeriod(anchorMillis, selectedPeriod, -1) },
                    onNext = { anchorMillis = shiftHomePeriod(anchorMillis, selectedPeriod, 1) },
                    onCurrent = { anchorMillis = System.currentTimeMillis() }
                )
            }
        }
        item(key = "history_types") {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Jenis transaksi", color = HistoryStyle.Ink, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("all" to "Semua", "income" to "Pemasukan", "expense" to "Pengeluaran")
                        .forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(label, fontSize = 13.sp) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(
                                    1.dp, if (selectedType == type) HistoryStyle.Red else HistoryStyle.Border
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    labelColor = HistoryStyle.Muted,
                                    selectedContainerColor = HistoryStyle.Selected,
                                    selectedLabelColor = HistoryStyle.Red
                                )
                            )
                        }
                }
            }
        }
        when {
            state.isLoading -> item(key = "history_loading") {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = HistoryStyle.Red)
                    Spacer(Modifier.height(12.dp))
                    Text("Memuat riwayat...", color = HistoryStyle.Muted)
                }
            }
            state.errorMessage != null -> item(key = "history_error") {
                HistoryNotice(
                    title = "Riwayat belum dapat dimuat",
                    description = state.errorMessage,
                    actionLabel = "Coba lagi", onAction = onRetry
                )
            }
            visibleTransactions.isEmpty() -> item(key = "history_empty") {
                HistoryNotice(
                    title = when (selectedType) {
                        "income" -> "Belum ada pemasukan pada periode ini"
                        "expense" -> "Belum ada pengeluaran pada periode ini"
                        else -> "Belum ada transaksi pada periode ini"
                    },
                    description = "Pilih periode lain dengan tab dan panah di atas, atau tambahkan transaksi melalui tombol + di Beranda.",
                    actionLabel = if (selectedType != "all") "Tampilkan semua jenis" else null,
                    onAction = { selectedType = "all" }
                )
            }
            else -> {
                item(key = "history_count") {
                    Text(
                        "${visibleTransactions.size} transaksi • Terbaru dahulu",
                        color = HistoryStyle.Muted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                groups.forEach { (date, rows) ->
                    item(key = "date_$date") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                date, color = HistoryStyle.Ink, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).semantics { heading() }
                            )
                            Text("${rows.size} transaksi", color = HistoryStyle.Muted, fontSize = 11.sp)
                        }
                    }
                    items(rows, key = { "transaction_${it.transaction.transactionId}" }) { item ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            TransactionItem(
                                item = item,
                                actionsEnabled = actionsEnabled,
                                onViewReceipt = { onViewReceipt(item) },
                                onEdit = { onEdit(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryNotice(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = Color.White, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HistoryStyle.Border)
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = HistoryStyle.Ink, fontWeight = FontWeight.SemiBold)
            Text(description, color = HistoryStyle.Muted, fontSize = 13.sp)
            if (actionLabel != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = HistoryStyle.Red) }
            }
        }
    }
}

private fun historyPreviewDate(day: Int): Long = Calendar.getInstance().apply {
    clear()
    set(2026, Calendar.SEPTEMBER, day, 12, 0, 0)
}.timeInMillis

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Riwayat • Harian")
@Composable
private fun HistoryScreenPreview() {
    MaterialTheme {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
                transactions = listOf(
                    TransactionWithCategory(
                        TransactionEntity(1, 1, "expense", 50000, historyPreviewDate(29)), "Makanan", "🍔"
                    ),
                    TransactionWithCategory(
                        TransactionEntity(2, 2, "income", 250000, historyPreviewDate(29)), "Freelance", "💻"
                    )
                )
            ),
            initialAnchorMillis = historyPreviewDate(29)
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f, name = "Riwayat • Kosong / font besar")
@Composable
private fun HistoryEmptyPreview() {
    MaterialTheme { HistoryContent(HistoryUiState(isLoading = false)) }
}
