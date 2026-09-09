package com.fadhil.financereceipt.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadhil.financereceipt.ui.history.TransactionItem
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

private val ReceiptRed = Color(0xFFEF0012)
private val ReceiptBackground = Color(0xFFF7F9FB)
private val ReceiptInk = Color(0xFF172B46)
private val ReceiptMuted = Color(0xFF7E91B3)

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onAddTransaction, homeViewModel::loadHome)
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAddTransaction: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var anchorMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedPeriod by rememberSaveable { mutableStateOf("Harian") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    // Dihitung ulang agar perubahan zona waktu ikut tercermin saat layar dibuat ulang.
    val period = homePeriod(anchorMillis, selectedPeriod)
    val rows = remember(state.transactions, period.start, period.endExclusive) {
        state.transactions.filter {
            it.transaction.transactionDate >= period.start &&
                    it.transaction.transactionDate < period.endExclusive
        }
    }
    // BigInteger menjaga penjumlahan beberapa nominal Long agar tidak overflow.
    val income = remember(rows) {
        rows.filter { it.transaction.transactionType == "income" }
            .fold(BigInteger.ZERO) { total, row -> total + BigInteger.valueOf(row.transaction.amount) }
    }
    val expense = remember(rows) {
        rows.filter { it.transaction.transactionType == "expense" }
            .fold(BigInteger.ZERO) { total, row -> total + BigInteger.valueOf(row.transaction.amount) }
    }
    val summaryAvailable = !state.isLoading && state.errorMessage == null

    Box(Modifier.fillMaxSize().background(ReceiptBackground)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(ReceiptRed, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HeaderButton("☰", "Menu") { message = "Menu tambahan belum tersedia pada tahap ini." }
                    Spacer(Modifier.weight(1f))
                    HeaderButton("↓", "Ekspor") { message = "Ekspor belum tersedia pada tahap ini." }
                    HeaderButton("≡", "Filter") { message = "Gunakan tab periode dan tombol panah untuk memilih rentang tanggal. Filter jenis transaksi tersedia di Riwayat." }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderButton("‹", "Periode sebelumnya") { anchorMillis = shiftHomePeriod(anchorMillis, selectedPeriod, -1) }
                    Text(period.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    HeaderButton("›", "Periode berikutnya") { anchorMillis = shiftHomePeriod(anchorMillis, selectedPeriod, 1) }
                }
            }

            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-4).dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    listOf("Harian", "Mingguan", "Bulanan", "Tahunan").forEach { period ->
                        val active = period == selectedPeriod
                        Surface(
                            onClick = { selectedPeriod = period },
                            modifier = Modifier.weight(1f),
                            color = if (active) ReceiptRed else Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(Modifier.heightIn(min = 48.dp).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(period, fontSize = 11.sp, color = if (active) Color.White else ReceiptMuted)
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = { anchorMillis = System.currentTimeMillis() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Kembali ke periode saat ini", color = ReceiptRed) }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 2.dp
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    SummaryValue("PEMASUKAN", if (summaryAvailable) rupiah(income) else "—", Color(0xFF009F76), Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(32.dp).background(ReceiptBackground))
                    SummaryValue("PENGELUARAN", if (summaryAvailable) rupiah(expense) else "—", ReceiptRed, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(32.dp).background(ReceiptBackground))
                    SummaryValue("SALDO PERIODE", if (summaryAvailable) rupiah(income - expense) else "—", ReceiptInk, Modifier.weight(1f))
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(color = ReceiptRed,
                            modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Memuat transaksi...", color = ReceiptMuted)
                    }
                    state.errorMessage != null -> {
                        Text(state.errorMessage.orEmpty(), color = ReceiptRed)
                        TextButton(onClick = onRetry) { Text("Coba lagi") }
                    }
                    rows.isEmpty() -> {
                        Text("Belum ada transaksi pada periode ini", color = ReceiptInk,
                            fontWeight = FontWeight.Bold)
                        Text("Pilih periode lain atau tambahkan transaksi melalui tombol +.",
                            color = ReceiptMuted)
                    }
                    else -> {
                        Text("Transaksi terbaru", color = ReceiptInk,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${rows.size} transaksi dalam periode ini", color = ReceiptMuted, fontSize = 12.sp)
                        rows.take(5).forEach { item ->
                            key(item.transaction.transactionId) { TransactionItem(item) }
                        }
                        if (rows.size > 5) {
                            Text("Menampilkan 5 transaksi terbaru. Daftar lengkap tersedia di menu Riwayat.",
                                color = ReceiptMuted, fontSize = 12.sp)
                        }
                    }
                }
                Text("Saldo periode = pemasukan − pengeluaran pada rentang tanggal terpilih.",
                    color = ReceiptMuted, fontSize = 12.sp)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Plan Keuangan", color = ReceiptInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Fitur pengaturan anggaran belum tersedia.", color = ReceiptMuted, fontSize = 13.sp)
                    }
                }
            }

        }
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = ReceiptRed, contentColor = Color.White,
            shape = RoundedCornerShape(50)
        ) {
            Text("+", fontSize = 32.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Informasi") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Mengerti") } }
        )
    }
}

@Composable
private fun SummaryValue(label: String, amount: String, amountColor: Color, modifier: Modifier) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ReceiptMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(amount, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HeaderButton(symbol: String, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) {
        Text(symbol, color = Color.White, fontSize = 26.sp,
            modifier = Modifier.semantics { contentDescription = label })
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme { HomeContent(HomeUiState(isLoading = false)) }
}

private fun rupiah(amount: BigInteger): String =
    "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount)
