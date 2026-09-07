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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ReceiptRed = Color(0xFFEF0012)
private val ReceiptBackground = Color(0xFFF7F9FB)
private val ReceiptInk = Color(0xFF172B46)
private val ReceiptMuted = Color(0xFF7E91B3)

/** Tahap tampilan: ringkasan transaksi kosong dan plan contoh, belum memakai Room. */
@Composable
fun HomeScreen() {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedPeriod by rememberSaveable { mutableStateOf("Harian") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val month = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(month.time)

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
                    HeaderButton("≡", "Filter") { message = "Filter transaksi akan ditambahkan setelah data transaksi tersedia." }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderButton("‹", "Bulan sebelumnya") { monthOffset-- }
                    Text(monthLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    HeaderButton("›", "Bulan berikutnya") { monthOffset++ }
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

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 2.dp
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    SummaryValue("PEMASUKAN", Color(0xFF009F76), Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(32.dp).background(ReceiptBackground))
                    SummaryValue("PENGELUARAN", ReceiptRed, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(32.dp).background(ReceiptBackground))
                    SummaryValue("SALDO", ReceiptInk, Modifier.weight(1f))
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = RoundedCornerShape(50), color = Color(0xFFEEF3F8)) {
                    Box(Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                        Text("▤", fontSize = 48.sp, color = Color(0xFFCCD8E7))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Data tidak tersedia", color = ReceiptInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                Text("Mulai catat pengeluaran dan pemasukan Anda", color = ReceiptMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("$selectedPeriod • $monthLabel", color = ReceiptMuted, fontSize = 12.sp)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Plan Keuangan", color = ReceiptInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Contoh tampilan, bukan data transaksi", color = ReceiptMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("🍔 Anggaran Makanan", color = ReceiptInk, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text("Rp750.000 dari Rp1.000.000", color = ReceiptInk, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { 0.75f }, modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFFD18A00), trackColor = Color(0xFFFFF1D2)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("75% · 😐 Mendekati batas", color = Color(0xFF906000), fontSize = 13.sp)
                    Text("Sisa anggaran Rp250.000", color = ReceiptMuted, fontSize = 12.sp)
                }
            }
        }
        FloatingActionButton(
            onClick = { message = "Form tambah transaksi akan kita buat pada tahap berikutnya." },
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
private fun SummaryValue(label: String, amountColor: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ReceiptMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("Rp 0", color = amountColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    MaterialTheme { HomeScreen() }
}
