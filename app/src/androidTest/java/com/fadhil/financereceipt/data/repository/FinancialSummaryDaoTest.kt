package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.domain.finance.FinancialSummaryCalculator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

/** Memerlukan emulator/perangkat. Memeriksa query Room dan pembaruan Flow nyata. */
@RunWith(AndroidJUnit4::class)
class FinancialSummaryDaoTest {
    @Test fun joinAndFlowFollowCategoryTransactionChanges() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val categoryId = db.categoryDao().insert(CategoryEntity(
                categoryName = "Makanan", transactionType = "expense", emoji = "🍔",
                financialGroup = CategoryEntity.NEEDS
            ))
            val incomeCategoryId = db.categoryDao().insert(CategoryEntity(
                categoryName = "Gaji", transactionType = "income", emoji = "💼"
            ))
            val repo = TransactionRepository(db)
            repo.saveManualTransaction(incomeCategoryId, "income", 100, 10, "")
            val expenseId = repo.saveManualTransaction(categoryId, "expense", 50, 10, "")
            val before = repo.observeHistory().first()
            assertEquals("NEEDS", before.first { it.transaction.transactionId == expenseId }.financialGroup)
            val beforeSummary = FinancialSummaryCalculator.calculate(before.map {
                FinancialSummaryCalculator.Entry(it.transaction.transactionDate,
                    it.transaction.transactionType, it.transaction.amount, it.financialGroup)
            }, 0, 100)
            assertEquals(BigInteger.valueOf(100), beforeSummary.totalIncome)
            assertEquals(BigInteger.valueOf(50), beforeSummary.totalExpense)

            val changedGroup = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5000) {
                    repo.observeHistory().first { rows ->
                        rows.any { it.transaction.transactionId == expenseId && it.financialGroup == "WANTS" }
                    }
                }
            }
            val category = requireNotNull(db.categoryDao().getById(categoryId))
            db.categoryDao().update(category.copy(financialGroup = CategoryEntity.WANTS, recommendedPercentage = 30))
            assertEquals("WANTS", changedGroup.await().first { it.transaction.transactionId == expenseId }.financialGroup)

            val edited = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5000) {
                    repo.observeHistory().first { rows -> rows.any { it.transaction.amount == 60L } }
                }
            }
            repo.updateTransaction(expenseId, categoryId, "expense", 60, 10, "edit")
            val after = FinancialSummaryCalculator.calculate(edited.await().map {
                FinancialSummaryCalculator.Entry(it.transaction.transactionDate,
                    it.transaction.transactionType, it.transaction.amount, it.financialGroup)
            }, 0, 100)
            assertEquals(BigInteger.valueOf(40), after.balance)
            assertEquals(FinancialSummaryCalculator.Status.OVER_LIMIT,
                after.groups.first { it.group == FinancialSummaryCalculator.Group.WANTS }.status)

            repo.deleteTransaction(expenseId)
            assertEquals(1, repo.observeHistory().first().size)
        } finally { db.close() }
    }
}
