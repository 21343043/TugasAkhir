package com.fadhil.financereceipt.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palet Beranda dengan merah/teks sekunder sedikit diperdalam untuk kontras kontrol.
internal object HistoryStyle {
    val Red = Color(0xFFD90016)
    val Background = Color(0xFFF7F9FB)
    val Ink = Color(0xFF172B46)
    val Muted = Color(0xFF526780)
    val Border = Color(0xFFE1E6EF)
    val Selected = Color(0xFFFFF0F1)
}

@Composable
internal fun HistoryPeriodFilter(
    selectedPeriod: String,
    periodLabel: String,
    onSelectPeriod: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HistoryStyle.Border)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val tabWidth = (maxWidth / 4).coerceAtLeast(76.dp)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .selectableGroup()
                ) {
                    listOf("Harian", "Mingguan", "Bulanan", "Tahunan").forEach { label ->
                        val selected = selectedPeriod == label
                        Surface(
                            color = if (selected) HistoryStyle.Red else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(tabWidth).selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelectPeriod(label) }
                            )
                        ) {
                            Box(
                                Modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label, fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else HistoryStyle.Muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = HistoryStyle.Border)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PeriodArrow("‹", "Periode sebelumnya", onPrevious)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PERIODE TERPILIH", color = HistoryStyle.Muted, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        periodLabel, color = HistoryStyle.Ink, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                    )
                }
                PeriodArrow("›", "Periode berikutnya", onNext)
            }
            TextButton(onClick = onCurrent, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    when (selectedPeriod) {
                        "Mingguan" -> "Kembali ke minggu ini"
                        "Bulanan" -> "Kembali ke bulan ini"
                        "Tahunan" -> "Kembali ke tahun ini"
                        else -> "Kembali ke hari ini"
                    }, color = HistoryStyle.Red, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PeriodArrow(symbol: String, label: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = label },
        shape = RoundedCornerShape(14.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = HistoryStyle.Background, contentColor = HistoryStyle.Ink
        )
    ) { Text(symbol, fontSize = 28.sp) }
}
