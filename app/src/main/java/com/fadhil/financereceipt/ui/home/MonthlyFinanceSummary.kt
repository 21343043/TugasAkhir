package com.fadhil.financereceipt.ui.home

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fadhil.financereceipt.domain.finance.FinancialSummaryCalculator.Group
import com.fadhil.financereceipt.domain.finance.FinancialSummaryCalculator.GroupSummary
import com.fadhil.financereceipt.domain.finance.FinancialSummaryCalculator.Status
import com.fadhil.financereceipt.domain.finance.FinancialSummaryCalculator.Summary
import com.fadhil.financereceipt.domain.finance.FinancialSummaryCalculator
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val AnalysisInk = Color(0xFF1E293B)
private val AnalysisMuted = Color(0xFF64748B)
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
            Text("Gunakan pemilih bulan di atas untuk analisis ini.",
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
    Text("Diagram Pengeluaran bulanan", color = AnalysisInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("Persentase di dalam diagram dihitung dari total pemasukan bulan terpilih.",
        color = AnalysisMuted, fontSize = 12.sp)

    // Satu sumber data untuk warna, potongan diagram, dan legenda di luar.
    val slices = summary.groups.filter { it.group != Group.UNGROUPED }.map { item ->
        IncomePieSlice(summaryGroupLabel(item.group), item.amount, groupColor(item.group))
    } + IncomePieSlice("Saldo bulan", summary.balance, Color(0xFF8B5CF6))

    // Saldo negatif dan data tanpa kategori tidak boleh digambar sebagai
    // empat bagian yang seolah-olah berjumlah 100% dari pemasukan.
    val chartMessage: String? = when {
        summary.totalIncome.signum() <= 0 ->
            "Belum ada pemasukan. Persentase diagram belum dapat dihitung."
        summary.balance.signum() < 0 ->
            "Bulan ini defisit ${analysisRupiah(summary.balance.abs())}. Diagram tidak ditampilkan karena pengeluaran melebihi pemasukan."
        summary.hasUngroupedExpenses ->
            "Lengkapi kategori pengeluaran lama agar diagram empat bagian dapat ditampilkan dengan benar."
        else -> null
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        IncomePieChart(slices, summary.totalIncome, chartMessage)
    }
    if (chartMessage != null) {
        Text(chartMessage, color = AnalysisMuted, fontSize = 13.sp)
    }
    slices.forEach { item ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(12.dp).background(item.color, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(item.label, color = AnalysisInk, fontWeight = FontWeight.SemiBold)
                // Legenda hanya nama dan nominal. Persentase ada di dalam piechart.
                Text(analysisRupiah(item.amount), color = AnalysisInk)
            }
        }
    }
    Text("Persentase dibulatkan hingga dua desimal. Bagian bernilai nol tidak membentuk potongan diagram.",
        color = AnalysisMuted, fontSize = 12.sp)

    HorizontalDivider()
    Text("Target Pengeluaran Bulanan", color = AnalysisInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Text("Dasar indikator: pemasukan tercatat bulan terpilih. Gunakan pemasukan bersih yang diterima.",
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

private data class IncomePieSlice(
    val label: String,
    val amount: BigInteger,
    val color: Color
)

// Jangan memakai persen tampilan yang sudah dibulatkan untuk menentukan sudut.
private fun incomePieFraction(amount: BigInteger, totalIncome: BigInteger): Float =
    if (totalIncome.signum() <= 0) 0f else BigDecimal(amount)
        .divide(BigDecimal(totalIncome), 12, RoundingMode.HALF_UP).toFloat()

private fun incomePiePercentage(amount: BigInteger, totalIncome: BigInteger): String =
    if (totalIncome.signum() <= 0) "—" else percentageText(
        BigDecimal(amount.multiply(BigInteger.valueOf(100)))
            .divide(BigDecimal(totalIncome), 2, RoundingMode.HALF_UP)
    )

@Composable
private fun IncomePieChart(
    slices: List<IncomePieSlice>,
    totalIncome: BigInteger,
    unavailableMessage: String?
) {
    val description = unavailableMessage ?: (
            "Diagram pengeluaran bulanan. " + slices.joinToString(". ") {
                "${it.label}: ${analysisRupiah(it.amount)}, ${incomePiePercentage(it.amount, totalIncome)}"
            }
            )
    val nonZero = slices.filter { it.amount.signum() > 0 }
    Canvas(Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f)
        .semantics { contentDescription = description }) {
        if (unavailableMessage != null || totalIncome.signum() <= 0 || nonZero.isEmpty()) {
            drawCircle(Color(0xFFE8EDF3))
        } else {
            // Catat posisi teks, lalu gambar semua teks setelah semua potongan.
            val labels = mutableListOf<Triple<String, Float, Float>>()
            var startAngle = -90f
            var drawn = 0f
            nonZero.forEachIndexed { index, item ->
                // Bagian terakhir mengisi sisa akibat pembulatan Float.
                val sweep = if (index == nonZero.lastIndex) (360f - drawn).coerceAtLeast(0f)
                else (incomePieFraction(item.amount, totalIncome) * 360f)
                    .coerceIn(0f, (360f - drawn).coerceAtLeast(0f))
                drawArc(color = item.color, startAngle = startAngle,
                    sweepAngle = sweep, useCenter = true)
                labels.add(Triple(incomePiePercentage(item.amount, totalIncome), startAngle + sweep / 2f, sweep))
                startAngle += sweep
                drawn += sweep
            }

            val radius = size.minDimension / 2f
            val labelRadius = radius * 0.64f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(1.dp.toPx(), 0f, 1.dp.toPx(), android.graphics.Color.DKGRAY)
            }
            labels.forEach { (text, middleAngle, sweep) ->
                if (sweep > 0f) {
                    val radians = Math.toRadians(middleAngle.toDouble())
                    // Satu bagian 100%: angka tepat di tengah lingkaran.
                    val distance = if (nonZero.size == 1) 0f else labelRadius
                    val x = center.x + cos(radians).toFloat() * distance
                    val y = center.y + sin(radians).toFloat() * distance
                    paint.textSize = 13.sp.toPx()

                    // Perkecil teks pada potongan sempit agar tetap di dalamnya.
                    // Pada persentase sangat kecil, teks juga akan sangat kecil;
                    // angka lengkap tetap tersedia di indikator dan aksesibilitas.
                    if (nonZero.size > 1) {
                        val halfAngle = Math.toRadians((sweep / 2f).coerceAtMost(90f).toDouble())
                        val available = (2f * labelRadius * sin(halfAngle).toFloat() * 0.82f)
                            .coerceAtLeast(1f)
                        val metrics = paint.fontMetrics
                        val diagonal = kotlin.math.hypot(paint.measureText(text), metrics.descent - metrics.ascent)
                        if (diagonal > available) paint.textSize *= available / diagonal
                    }
                    // drawText memakai baseline; koreksi agar teks terpusat vertikal.
                    val baseline = y - (paint.ascent() + paint.descent()) / 2f
                    drawContext.canvas.nativeCanvas.drawText(text, x, baseline, paint)
                }
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
    Group.WANTS -> Color(0xFFF97316)
    Group.SAVINGS -> Color(0xFF059669)
    Group.UNGROUPED -> Color(0xFF64748B)
}

private fun analysisRupiah(amount: BigInteger): String =
    (if (amount.signum() < 0) "−Rp " else "Rp ") +
            NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount.abs())

private fun percentageText(value: BigDecimal?): String = value?.let {
    it.stripTrailingZeros().toPlainString().replace('.', ',') + "%"
} ?: "—"

/** Data contoh hanya untuk Android Studio Preview, tidak dimasukkan ke database. */
@Preview(name = "Analisis bulanan dengan saldo", showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun MonthlyFinanceSummaryPreview() {
    val month = homePeriod(System.currentTimeMillis(), "Bulanan")
    val entries = listOf(
        FinancialSummaryCalculator.Entry(month.start, "income", 1300000, null),
        FinancialSummaryCalculator.Entry(month.start, "expense", 270000, "NEEDS"),
        FinancialSummaryCalculator.Entry(month.start, "expense", 110000, "WANTS"),
        FinancialSummaryCalculator.Entry(month.start, "expense", 200000, "SAVINGS")
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
