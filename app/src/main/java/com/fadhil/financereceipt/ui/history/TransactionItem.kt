package com.fadhil.financereceipt.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItem(item: TransactionWithCategory) {
    val transaction = item.transaction
    val isIncome = transaction.transactionType == "income"
    val amountText = remember(transaction.amount) {
        "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID"))
            .format(transaction.amount)
    }
    val dateText = remember(transaction.transactionDate) {
        SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
            .format(Date(transaction.transactionDate))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${item.categoryEmoji} ${item.categoryName}",
                color = Color(0xFF172B46), fontWeight = FontWeight.SemiBold, fontSize = 16.sp
            )
            Text(dateText, color = Color(0xFF667A96), fontSize = 12.sp)
            Text(
                text = (if (isIncome) "+ " else "− ") + amountText,
                color = if (isIncome) Color(0xFF00875F) else Color(0xFFEF0012),
                fontWeight = FontWeight.Bold, fontSize = 20.sp
            )
            Text(
                text = if (isIncome) "Pemasukan" else "Pengeluaran",
                color = Color(0xFF667A96), fontSize = 12.sp
            )
            if (transaction.note.isNotBlank()) {
                Text(transaction.note, color = Color(0xFF172B46), fontSize = 14.sp)
            }
        }
    }
}
