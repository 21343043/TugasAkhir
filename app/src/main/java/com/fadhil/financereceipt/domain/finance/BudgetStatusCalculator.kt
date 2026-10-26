package com.fadhil.financereceipt.domain.finance

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

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
