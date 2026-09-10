package com.fadhil.financereceipt.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadhil.financereceipt.data.repository.PlanProgressItem
import com.fadhil.financereceipt.utils.BudgetStatus
import com.fadhil.financereceipt.utils.planRupiah
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val PlanRed = Color(0xFFEF0012)
private val PlanInk = Color(0xFF172B46)
private val PlanMuted = Color(0xFF667A96)

@Composable
fun PlanScreen(
    planViewModel: PlanViewModel = viewModel(),
    openCurrentMonth: Boolean = false,
    onCurrentMonthOpened: () -> Unit = {}
) {
    val initial = remember { Calendar.getInstance() }
    var year by rememberSaveable { mutableIntStateOf(initial.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableIntStateOf(initial.get(Calendar.MONTH) + 1) }
    var formOpen by rememberSaveable { mutableStateOf(false) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var editPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editCategory by rememberSaveable { mutableStateOf("") }
    var editAmount by rememberSaveable { mutableStateOf("") }
    var deletePlanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCategory by rememberSaveable { mutableStateOf("") }
    val state by planViewModel.uiState.collectAsStateWithLifecycle()
    val saveState by planViewModel.saveState.collectAsStateWithLifecycle()
    val manageState by planViewModel.manageState.collectAsStateWithLifecycle()
    val dialogOpen = formOpen || editPlanId != null || deletePlanId != null
    val busy = saveState.isSaving || manageState.isWorking
    val current = state.year == year && state.month == month
    val ready = current && !state.isLoading && state.errorMessage == null
    val monthLabel = remember(year, month) {
        val calendar = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")).format(calendar.time)
    }

    // Konsumsi permintaan sekali; navigasi tab biasa tetap mempertahankan bulan pilihan.
    LaunchedEffect(openCurrentMonth, busy) {
        if (openCurrentMonth && !busy) {
            val today = Calendar.getInstance()
            year = today.get(Calendar.YEAR)
            month = today.get(Calendar.MONTH) + 1
            onCurrentMonthOpened()
        }
    }

    LaunchedEffect(year, month) { planViewModel.loadMonth(year, month) }
    LaunchedEffect(saveState.savedPlanId) {
        if (saveState.savedPlanId != null) {
            formOpen = false
            successMessage = "Plan keuangan berhasil disimpan."
            planViewModel.resetSaveState()
        }
    }

    LaunchedEffect(manageState.completedMessage) {
        manageState.completedMessage?.let { message ->
            editPlanId = null
            deletePlanId = null
            successMessage = message
            planViewModel.resetManageState()
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F9FB))) {
        Column(Modifier.fillMaxWidth().background(PlanRed).padding(16.dp)) {
            Text("Plan Keuangan", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Atur anggaran pengeluaran setiap bulan", color = Color.White, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (month == 1) { month = 12; year-- } else month-- },
                    enabled = !dialogOpen && !busy && !(year == 1900 && month == 1),
                    modifier = Modifier.semantics { contentDescription = "Bulan sebelumnya" }
                ) { Text("‹", color = Color.White, fontSize = 28.sp) }
                Text(monthLabel, modifier = Modifier.weight(1f), color = Color.White,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(
                    onClick = { if (month == 12) { month = 1; year++ } else month++ },
                    enabled = !dialogOpen && !busy && !(year == 9999 && month == 12),
                    modifier = Modifier.semantics { contentDescription = "Bulan berikutnya" }
                ) { Text("›", color = Color.White, fontSize = 28.sp) }
            }
        }
        TextButton(
            onClick = {
                val today = Calendar.getInstance()
                year = today.get(Calendar.YEAR)
                month = today.get(Calendar.MONTH) + 1
            },
            enabled = !dialogOpen && !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Kembali ke bulan ini", color = PlanRed) }
        Button(
            onClick = { planViewModel.resetSaveState(); planViewModel.resetManageState(); formOpen = true },
            enabled = ready && !busy && !dialogOpen,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlanRed),
            shape = RoundedCornerShape(14.dp)
        ) { Text("+ Tambah Plan") }

        when {
            !current || state.isLoading -> Box(
                Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PlanRed) }
            state.errorMessage != null -> Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage.orEmpty(), color = PlanInk)
                TextButton(onClick = { planViewModel.loadMonth(year, month, force = true) }) { Text("Coba lagi") }
            }
            state.plans.isEmpty() -> Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada plan untuk bulan ini", color = PlanInk, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Tekan Tambah Plan untuk menetapkan batas anggaran kategori.",
                    color = PlanMuted, textAlign = TextAlign.Center)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${state.plans.size} kategori memiliki anggaran", color = PlanMuted, fontSize = 12.sp)
                }
                items(state.plans, key = { it.plan.planId }) { item ->
                    PlanCard(
                        item = item,
                        enabled = !busy && !dialogOpen,
                        onEdit = {
                            planViewModel.resetManageState()
                            editCategory = "${item.category.emoji} ${item.category.categoryName}"
                            editAmount = item.plan.budgetAmount.toString()
                            editPlanId = item.plan.planId
                        },
                        onDelete = {
                            planViewModel.resetManageState()
                            deleteCategory = "${item.category.emoji} ${item.category.categoryName}"
                            deletePlanId = item.plan.planId
                        }
                    )
                }
                item {
                    Text("Terpakai dihitung dari seluruh pengeluaran pada kategori dan bulan yang sama, termasuk transaksi sebelum plan dibuat.",
                        color = PlanMuted, fontSize = 12.sp)
                }
            }
        }
    }

    if (formOpen) {
        PlanFormScreen(
            monthLabel = monthLabel,
            categories = if (ready) state.categories else emptyList(),
            saveState = saveState,
            onSave = { categoryId, amount -> planViewModel.savePlan(categoryId, year, month, amount) },
            onDismiss = {
                if (!saveState.isSaving) { formOpen = false; planViewModel.resetSaveState() }
            }
        )
    }
    editPlanId?.let { id ->
        PlanEditDialog(
            planId = id,
            categoryLabel = editCategory,
            monthLabel = monthLabel,
            initialAmount = editAmount,
            state = manageState,
            onSave = { amount -> planViewModel.updateBudget(id, amount) },
            onDismiss = {
                if (!manageState.isWorking) { editPlanId = null; planViewModel.resetManageState() }
            }
        )
    }
    deletePlanId?.let { id ->
        val locked = manageState.isWorking || manageState.completedMessage != null
        AlertDialog(
            onDismissRequest = {
                if (!locked) { deletePlanId = null; planViewModel.resetManageState() }
            },
            title = { Text("Hapus Plan?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hapus anggaran $deleteCategory untuk $monthLabel?")
                    Text("Data pemasukan dan pengeluaran tetap tersimpan. Anda dapat membuat kembali plan untuk kategori ini.")
                    manageState.errorMessage?.let { Text(it, color = PlanRed) }
                }
            },
            confirmButton = {
                TextButton(onClick = { planViewModel.deletePlan(id) }, enabled = !locked) {
                    Text(if (manageState.isWorking) "MENGHAPUS..." else "Hapus", color = PlanRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletePlanId = null; planViewModel.resetManageState() }, enabled = !locked
                ) { Text("Batal") }
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

@Composable
private fun PlanCard(
    item: PlanProgressItem,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val usage = item.usage
    val statusColor = when (usage.status) {
        BudgetStatus.SAFE -> Color(0xFF00875F)
        BudgetStatus.NEAR_LIMIT -> Color(0xFF906000)
        BudgetStatus.AT_LIMIT, BudgetStatus.OVER_LIMIT -> PlanRed
    }
    Surface(
        modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${item.category.emoji} ${item.category.categoryName}", color = PlanInk,
                fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Anggaran ${planRupiah(BigInteger.valueOf(item.plan.budgetAmount))}", color = PlanInk)
            Text("Terpakai ${planRupiah(usage.spent)}", color = PlanInk)
            LinearProgressIndicator(
                progress = { usage.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = statusColor, trackColor = Color(0xFFEEF1F5)
            )
            Text("${usage.percentage}% · ${usage.status.emoji} ${usage.status.label}",
                color = statusColor, fontWeight = FontWeight.Medium)
            Text(
                if (usage.remaining.signum() < 0) "Melebihi anggaran ${planRupiah(usage.remaining.abs())}"
                else "Sisa anggaran ${planRupiah(usage.remaining)}",
                color = PlanMuted, fontSize = 13.sp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("Edit anggaran", color = PlanInk)
                }
                TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("Hapus", color = PlanRed)
                }
            }
        }
    }
}
