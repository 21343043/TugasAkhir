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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel = viewModel()) {
    val state by historyViewModel.uiState.collectAsStateWithLifecycle()
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
                        TransactionItem(it)
                    }
                }
            }
        }
    }
}
