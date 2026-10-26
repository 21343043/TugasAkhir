package com.fadhil.financereceipt.domain.finance

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Collections

/** Perhitungan murni JVM, terpisah dari Android dan database. */
object FinancialSummaryCalculator {
    private val hundred = BigInteger.valueOf(100)

    enum class Group(@JvmField val label: String, @JvmField val targetPercentage: Int) {
        NEEDS("Needs", 50), WANTS("Wants", 30), SAVINGS("Savings", 20),
        UNGROUPED("Belum dikelompokkan", 0);

        companion object {
            @JvmStatic fun fromValue(value: String?): Group = when (value) {
                "NEEDS" -> NEEDS
                "WANTS" -> WANTS
                "SAVINGS" -> SAVINGS
                else -> UNGROUPED
            }
        }
    }

    enum class Status {
        NO_INCOME, WITHIN_LIMIT, AT_LIMIT, OVER_LIMIT,
        BELOW_TARGET, TARGET_REACHED, UNCLASSIFIED
    }

    data class Entry(
        @JvmField val transactionDate: Long,
        @JvmField val transactionType: String?,
        @JvmField val amount: Long,
        @JvmField val financialGroup: String?
    )

    class GroupSummary internal constructor(
        @JvmField val group: Group,
        @JvmField val amount: BigInteger,
        expense: BigInteger,
        income: BigInteger
    ) {
        /** Null berarti tidak ada denominator, bukan persentase nol. */
        @JvmField val expensePercentage: BigDecimal? = percentage(amount, expense)
        @JvmField val incomePercentage: BigDecimal? = percentage(amount, income)
        @JvmField val expenseFraction: Float = if (expense.signum() == 0) 0f else
            BigDecimal(amount).divide(BigDecimal(expense), 12, RoundingMode.HALF_UP).toFloat()
        @JvmField val status: Status = status(group, amount, income)
    }

    class Summary internal constructor(
        @JvmField val totalIncome: BigInteger,
        @JvmField val totalExpense: BigInteger,
        amounts: Map<Group, BigInteger>,
        @JvmField val transactionCount: Int
    ) {
        @JvmField val balance: BigInteger = totalIncome - totalExpense
        @JvmField val hasUngroupedExpenses: Boolean = amounts.getValue(Group.UNGROUPED).signum() > 0
        @JvmField val groups: List<GroupSummary> = Collections.unmodifiableList(
            Group.values().map { GroupSummary(it, amounts.getValue(it), totalExpense, totalIncome) }
        )
    }

    /** Awal inklusif dan akhir eksklusif mengikuti batas kalender pemanggil. */
    @JvmStatic fun calculate(entries: List<Entry>, start: Long, endExclusive: Long): Summary {
        require(start < endExclusive) { "Rentang tanggal tidak valid." }
        var income = BigInteger.ZERO
        var expense = BigInteger.ZERO
        val amounts = Group.values().associateWith { BigInteger.ZERO }.toMutableMap()
        var count = 0
        for (entry in entries) {
            if (entry.transactionDate < start || entry.transactionDate >= endExclusive) continue
            require(entry.amount > 0) { "Nominal transaksi tidak valid." }
            val amount = BigInteger.valueOf(entry.amount)
            when (entry.transactionType) {
                "income" -> income += amount
                "expense" -> {
                    expense += amount
                    val group = Group.fromValue(entry.financialGroup)
                    amounts[group] = amounts.getValue(group) + amount
                }
                else -> throw IllegalArgumentException("Jenis transaksi tidak valid.")
            }
            count++
        }
        return Summary(income, expense, amounts, count)
    }

    private fun percentage(amount: BigInteger, denominator: BigInteger): BigDecimal? =
        if (denominator.signum() == 0) null else
            BigDecimal(amount * hundred).divide(BigDecimal(denominator), 2, RoundingMode.HALF_UP)

    private fun status(group: Group, amount: BigInteger, income: BigInteger): Status {
        if (group == Group.UNGROUPED) return Status.UNCLASSIFIED
        if (income.signum() == 0) return Status.NO_INCOME
        val comparison = (amount * hundred).compareTo(income * BigInteger.valueOf(group.targetPercentage.toLong()))
        return when {
            group == Group.SAVINGS -> if (comparison >= 0) Status.TARGET_REACHED else Status.BELOW_TARGET
            comparison > 0 -> Status.OVER_LIMIT
            comparison == 0 -> Status.AT_LIMIT
            else -> Status.WITHIN_LIMIT
        }
    }
}
