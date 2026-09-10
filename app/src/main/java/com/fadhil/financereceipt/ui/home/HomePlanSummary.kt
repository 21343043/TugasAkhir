package com.fadhil.financereceipt.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadhil.financereceipt.data.repository.PlanProgressItem
import com.fadhil.financereceipt.ui.plan.PlanViewModel
import com.fadhil.financereceipt.utils.BudgetStatus
import com.fadhil.financereceipt.utils.planRupiah
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun HomePlanSummary(onOpenPlan: () -> Unit) {
    // Preview tidak mengakses database atau membuat AndroidViewModel.
    if (LocalInspectionMode.current) {
        Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Plan bulan ini", fontWeight = FontWeight.Bold)
                Text("Ringkasan anggaran tampil dari data aplikasi.")
            }
        }
        return
    }
    val planViewModel: PlanViewModel = viewModel(key = "home_plan_summary")
    val state by planViewModel.uiState.collectAsStateWithLifecycle()
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    // Muat saat Beranda dibuka; cek pergantian bulan bila layar tetap terbuka.
    LaunchedEffect(planViewModel) {
        while (isActive) {
            val today = Calendar.getInstance()
            currentYear = today.get(Calendar.YEAR)
            currentMonth = today.get(Calendar.MONTH) + 1
            planViewModel.loadMonth(currentYear, currentMonth)
            delay(60_000L)
        }
    }
    val monthLabel = remember(currentYear, currentMonth) {
        val calendar = Calendar.getInstance().apply { clear(); set(currentYear, currentMonth - 1, 1) }
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(calendar.time)
    }
    val current = state.year == currentYear && state.month == currentMonth
    val highlights = remember(state.plans) {
        state.plans.sortedWith(
            compareByDescending<PlanProgressItem> { it.usage.status.ordinal }
                .thenByDescending { it.usage.progress }
        ).take(3)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Plan bulan ini", color = Color(0xFF172B46), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(monthLabel, color = Color(0xFF667A96), fontSize = 13.sp)
            Text("Anggaran bulanan ditampilkan terpisah dari filter periode transaksi di atas.",
                color = Color(0xFF667A96), fontSize = 12.sp)
            when {
                !current || state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFEF0012))
                    Text("Memuat anggaran...", color = Color(0xFF667A96))
                }
                state.errorMessage != null -> {
                    Text("Ringkasan anggaran gagal dimuat.", color = Color(0xFFB00020))
                    TextButton(onClick = { planViewModel.loadMonth(currentYear, currentMonth, force = true) }) {
                        Text("Coba lagi")
                    }
                }
                highlights.isEmpty() -> Text("Belum ada plan untuk bulan ini.", color = Color(0xFF667A96))
                else -> {
                    highlights.forEach { item ->
                        key(item.plan.planId) { HomeBudgetItem(item) }
                    }
                    if (state.plans.size > 3) {
                        Text("${state.plans.size - 3} plan lainnya tersedia di menu Plan.",
                            color = Color(0xFF667A96), fontSize = 12.sp)
                    }
                }
            }
            OutlinedButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Plan bulan ini", color = Color(0xFFEF0012))
            }
        }
    }
}

@Composable
private fun HomeBudgetItem(item: PlanProgressItem) {
    val usage = item.usage
    val color = when (usage.status) {
        BudgetStatus.SAFE -> Color(0xFF00875F)
        BudgetStatus.NEAR_LIMIT -> Color(0xFF906000)
        BudgetStatus.AT_LIMIT, BudgetStatus.OVER_LIMIT -> Color(0xFFEF0012)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${item.category.emoji} ${item.category.categoryName}",
            color = Color(0xFF172B46), fontWeight = FontWeight.SemiBold)
        Text("${planRupiah(usage.spent)} dari ${planRupiah(BigInteger.valueOf(item.plan.budgetAmount))}",
            color = Color(0xFF172B46), fontSize = 13.sp)
        LinearProgressIndicator(
            progress = { usage.progress }, modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color, trackColor = Color(0xFFEEF1F5)
        )
        Text("${usage.percentage}% · ${usage.status.emoji} ${usage.status.label}", color = color, fontSize = 13.sp)
        Text(
            if (usage.remaining.signum() < 0) "Melebihi anggaran ${planRupiah(usage.remaining.abs())}"
            else "Sisa anggaran ${planRupiah(usage.remaining)}",
            color = Color(0xFF667A96), fontSize = 12.sp
        )
    }
}
