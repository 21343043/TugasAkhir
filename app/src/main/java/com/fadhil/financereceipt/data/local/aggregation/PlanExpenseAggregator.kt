package com.fadhil.financereceipt.data.local.aggregation

import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import java.math.BigInteger

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