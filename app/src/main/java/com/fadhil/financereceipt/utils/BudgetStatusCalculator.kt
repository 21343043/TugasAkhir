package com.fadhil.financereceipt.utils

import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

enum class BudgetStatus(val label: String, val emoji: String) {
    SAFE("Aman", "😊"),
    NEAR_LIMIT("Mendekati batas", "😐"),
    AT_LIMIT("Batas tercapai", "⚠️"),
    OVER_LIMIT("Melebihi anggaran", "😟")
}

data class BudgetUsage(
    val spent: BigInteger,
    val remaining: BigInteger,
    val progress: Float,
    val percentage: String,
    val status: BudgetStatus
)

object BudgetStatusCalculator {
    fun calculate(budgetAmount: Long, spent: BigInteger): BudgetUsage {
        require(budgetAmount > 0) { "Anggaran harus lebih dari nol." }
        require(spent >= BigInteger.ZERO) { "Pengeluaran tidak boleh negatif." }
        val budget = BigInteger.valueOf(budgetAmount)
        val status = when {
            spent > budget -> BudgetStatus.OVER_LIMIT
            spent == budget -> BudgetStatus.AT_LIMIT
            spent * BigInteger.valueOf(100) >= budget * BigInteger.valueOf(75) -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.SAFE
        }
        // Pembulatan ke bawah mencegah penggunaan di bawah batas tampil sebagai 100%.
        val percentage = BigDecimal(spent).multiply(BigDecimal(100))
            .divide(BigDecimal(budget), 2, RoundingMode.DOWN)
            .stripTrailingZeros().toPlainString().replace('.', ',')
        val progress = if (spent >= budget) 1f else (spent.toDouble() / budgetAmount.toDouble()).toFloat()
        return BudgetUsage(spent, budget - spent, progress, percentage, status)
    }
}

fun planRupiah(amount: BigInteger): String =
    "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount)

/** Batas bulan dalam zona waktu perangkat: awal termasuk, akhir tidak termasuk. */
fun planMonthBounds(year: Int, month: Int): Pair<Long, Long> {
    require(year in 1900..9999 && month in 1..12) { "Periode tidak valid." }
    val start = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1, 0, 0, 0)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return start.timeInMillis to end.timeInMillis
}

fun sumPlanExpenses(
    transactions: List<TransactionEntity>, start: Long, endExclusive: Long
): Map<Long, BigInteger> {
    val spentByCategory = mutableMapOf<Long, BigInteger>()
    transactions.forEach { transaction ->
        if (transaction.transactionType == "expense" &&
            transaction.transactionDate >= start && transaction.transactionDate < endExclusive
        ) {
            spentByCategory[transaction.categoryId] =
                (spentByCategory[transaction.categoryId] ?: BigInteger.ZERO) +
                        BigInteger.valueOf(transaction.amount)
        }
    }
    return spentByCategory
}
