package com.fadhil.financereceipt.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator.Group
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator.GroupSummary
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator.Status
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator.Summary
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

private val AnalysisInk = Color(0xFF172B46)
private val AnalysisMuted = Color(0xFF53657B)
private val AnalysisGreen = Color(0xFF08734D)
private val AnalysisRed = Color(0xFFB42318)

@Composable
fun MonthlyFinanceSummary(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(22.dp), shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Analisis keuangan bulanan", color = AnalysisInk,
                fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPreviousMonth, enabled = !state.isLoading,
                    modifier = Modifier.semantics { contentDescription = "Bulan analisis sebelumnya" }) {
                    Text("‹", fontSize = 24.sp)
                }
                Text("Bulan ${state.analysisMonth.label}", modifier = Modifier.weight(1f),
                    color = AnalysisInk, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onNextMonth, enabled = !state.isLoading,
                    modifier = Modifier.semantics { contentDescription = "Bulan analisis berikutnya" }) {
                    Text("›", fontSize = 24.sp)
                }
            }
            TextButton(onClick = onCurrentMonth, enabled = !state.isLoading) { Text("Bulan ini") }
            Text("Gunakan pemilih bulan di atas untuk analisis ini. Filter riwayat di Beranda berlaku terpisah.",
                color = AnalysisMuted, fontSize = 12.sp)
            val summary = state.financialSummary
            when {
                state.isLoading -> {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text("Memuat analisis bulanan…", color = AnalysisMuted)
                }
                state.errorMessage != null -> {
                    Text(state.errorMessage, color = AnalysisRed)
                    TextButton(onClick = onRetry) { Text("Coba lagi") }
                }
                summary != null -> MonthlySummaryContent(summary)
            }
        }
    }
}

@Composable
private fun MonthlySummaryContent(summary: Summary) {
    if (summary.transactionCount == 0) {
        Text("Belum ada transaksi pada bulan ini.", color = AnalysisMuted)
    }
    MonthlyAmount("Total pemasukan", summary.totalIncome, AnalysisGreen)
    MonthlyAmount("Total pengeluaran", summary.totalExpense, AnalysisRed)
    MonthlyAmount("Saldo bulan", summary.balance,
        if (summary.balance.signum() < 0) AnalysisRed else AnalysisInk)
    Text("Saldo = pemasukan − pengeluaran. Pengeluaran termasuk alokasi Savings yang dicatat.",
        color = AnalysisMuted, fontSize = 12.sp)

    HorizontalDivider()
    Text("Komposisi pengeluaran", color = AnalysisInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("Persentase diagram = nominal kelompok ÷ total pengeluaran bulan ini × 100%.",
        color = AnalysisMuted, fontSize = 12.sp)
    val visible = summary.groups.filter { it.group != Group.UNGROUPED || it.amount.signum() > 0 }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ExpensePieChart(visible, summary.totalExpense)
    }
    if (summary.totalExpense.signum() == 0) {
        Text("Belum ada pengeluaran untuk ditampilkan dalam diagram.", color = AnalysisMuted)
    }
    visible.forEach { item ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(12.dp).background(groupColor(item.group), CircleShape))
            Column(Modifier.weight(1f)) {
                Text(summaryGroupLabel(item.group), color = AnalysisInk, fontWeight = FontWeight.SemiBold)
                Text("${analysisRupiah(item.amount)} (${percentageText(item.expensePercentage)})",
                    color = AnalysisInk)
            }
        }
    }
    Text("Persentase dibulatkan hingga dua desimal.", color = AnalysisMuted, fontSize = 12.sp)

    HorizontalDivider()
    Text("Indikator 50/30/20", color = AnalysisInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("Dasar indikator: pemasukan tercatat bulan ini. Gunakan pemasukan bersih yang diterima.",
        color = AnalysisMuted, fontSize = 12.sp)
    if (summary.totalIncome.signum() == 0) {
        Text("Belum ada pemasukan. Target 50/30/20 belum dapat dinilai.", color = AnalysisMuted)
    }
    if (summary.hasUngroupedExpenses) {
        Text("Ada data pengeluaran lama yang memerlukan kategori valid. Lengkapi kategorinya agar indikator dapat dinilai.",
            color = Color(0xFF805000), fontSize = 13.sp)
    }
    summary.groups.filter { it.group != Group.UNGROUPED }.forEach { item ->
        GroupTargetIndicator(item, provisional = summary.hasUngroupedExpenses)
    }
    Text("Savings mengikuti kategori Tabungan, Hutang, Investasi, dan Dana darurat. Angka ini bukan saldo rekening tabungan.",
        color = AnalysisMuted, fontSize = 12.sp)
}

@Composable
private fun MonthlyAmount(label: String, amount: BigInteger, color: Color) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = AnalysisMuted, fontSize = 13.sp)
        Text(analysisRupiah(amount), color = color, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpensePieChart(groups: List<GroupSummary>, totalExpense: BigInteger) {
    val description = if (totalExpense.signum() == 0) "Diagram pengeluaran kosong" else
        "Diagram komposisi pengeluaran. " + groups.joinToString(". ") {
            "${summaryGroupLabel(it.group)}: ${analysisRupiah(it.amount)}, ${percentageText(it.expensePercentage)}"
        }
    val nonZero = groups.filter { it.amount.signum() > 0 }
    Canvas(Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(1f)
        .semantics { contentDescription = description }) {
        if (totalExpense.signum() == 0) {
            drawCircle(Color(0xFFE8EDF3))
        } else {
            var startAngle = -90f
            var drawn = 0f
            nonZero.forEachIndexed { index, item ->
                // Bagian terakhir mengisi sisa akibat pembulatan Float.
                val sweep = if (index == nonZero.lastIndex) (360f - drawn).coerceAtLeast(0f)
                    else (item.expenseFraction * 360f).coerceIn(0f, (360f - drawn).coerceAtLeast(0f))
                drawArc(color = groupColor(item.group), startAngle = startAngle,
                    sweepAngle = sweep, useCenter = true)
                startAngle += sweep
                drawn += sweep
            }
        }
    }
}

@Composable
private fun GroupTargetIndicator(item: GroupSummary, provisional: Boolean) {
    val target = item.group.targetPercentage
    val statusText = when (item.status) {
        Status.NO_INCOME -> "Belum dapat dinilai"
        Status.WITHIN_LIMIT -> "Tidak melebihi $target%"
        Status.AT_LIMIT -> "Tepat pada batas $target%"
        Status.OVER_LIMIT -> "Melebihi $target%"
        Status.BELOW_TARGET -> "Belum mencapai $target%"
        Status.TARGET_REACHED -> "Mencapai target $target%"
        Status.UNCLASSIFIED -> "Kategori data lama perlu diperbarui"
    }
    val color = when {
        item.status == Status.NO_INCOME -> AnalysisMuted
        provisional -> Color(0xFF805000)
        item.status == Status.OVER_LIMIT -> AnalysisRed
        item.status == Status.BELOW_TARGET -> Color(0xFF805000)
        else -> AnalysisGreen
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${summaryGroupLabel(item.group)} · ${if (item.group == Group.SAVINGS) "Target minimal" else "Batas"} $target%",
            color = AnalysisInk, fontWeight = FontWeight.SemiBold)
        Text("${analysisRupiah(item.amount)} · ${percentageText(item.incomePercentage)} dari pemasukan",
            color = AnalysisMuted, fontSize = 13.sp)
        Text((if (provisional && item.status != Status.NO_INCOME) "Sementara: " else "") + statusText,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}

// Label cadangan untuk histori tak terduga, bukan kelompok yang dapat dipilih pengguna.
private fun summaryGroupLabel(group: Group): String =
    if (group == Group.UNGROUPED) "Data kategori lama" else group.label

private fun groupColor(group: Group): Color = when (group) {
    Group.NEEDS -> Color(0xFF2563EB)
    Group.WANTS -> Color(0xFFE58A17)
    Group.SAVINGS -> Color(0xFF12906C)
    Group.UNGROUPED -> Color(0xFF64748B)
}

private fun analysisRupiah(amount: BigInteger): String =
    (if (amount.signum() < 0) "−Rp " else "Rp ") +
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount.abs())

private fun percentageText(value: BigDecimal?): String = value?.let {
    it.stripTrailingZeros().toPlainString().replace('.', ',') + "%"
} ?: "—"

/** Data contoh hanya untuk Android Studio Preview, tidak dimasukkan ke database. */
@Preview(name = "Analisis bulanan 50 30 20", showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun MonthlyFinanceSummaryPreview() {
    val month = homePeriod(System.currentTimeMillis(), "Bulanan")
    val entries = listOf(
        FinancialSummaryCalculator.Entry(month.start, "income", 10000000, null),
        FinancialSummaryCalculator.Entry(month.start, "expense", 5000000, "NEEDS"),
        FinancialSummaryCalculator.Entry(month.start, "expense", 3000000, "WANTS"),
        FinancialSummaryCalculator.Entry(month.start, "expense", 2000000, "SAVINGS")
    )
    val state = HomeUiState(
        isLoading = false, analysisMonth = month,
        financialSummary = FinancialSummaryCalculator.calculate(entries, month.start, month.endExclusive)
    )
    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            MonthlyFinanceSummary(state, {}, {}, {}, {})
        }
    }
}
