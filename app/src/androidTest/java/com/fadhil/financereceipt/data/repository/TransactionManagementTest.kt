package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import java.math.BigInteger
import java.util.Calendar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Database sementara di memori; tidak menggunakan data aplikasi pengguna. */
@RunWith(AndroidJUnit4::class)
class TransactionManagementTest {
    private lateinit var database: FinanceDatabase
    private lateinit var transactions: TransactionRepository
    private lateinit var plans: FinancialPlanRepository
    private var food = 0L
    private var travel = 0L
    private var salary = 0L

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FinanceDatabase::class.java
        ).build()
        transactions = TransactionRepository(database)
        plans = FinancialPlanRepository(database)
        food = database.categoryDao().insert(CategoryEntity(categoryName = "Makanan", transactionType = "expense", emoji = "🍔"))
        travel = database.categoryDao().insert(CategoryEntity(categoryName = "Transportasi", transactionType = "expense", emoji = "🚗"))
        salary = database.categoryDao().insert(CategoryEntity(categoryName = "Gaji", transactionType = "income", emoji = "💼"))
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun updateOnlyTargetAndPreserveIdentityAndSource() = runBlocking {
        val original = TransactionEntity(categoryId = food, transactionType = "expense",
            amount = 25000, transactionDate = date(9), note = "awal", source = "scan", createdAt = 1234L)
        val id = database.transactionDao().insert(original)
        val otherId = transactions.saveManualTransaction(food, "expense", 7000, date(9), "lain")
        val other = database.transactionDao().getById(otherId)
        transactions.updateTransaction(id, travel, "expense", 40000, date(10), "  revisi  ")
        assertEquals(original.copy(transactionId = id, categoryId = travel, amount = 40000,
            transactionDate = date(10), note = "revisi"), database.transactionDao().getById(id))
        assertEquals(other, database.transactionDao().getById(otherId))
        assertEquals(2, transactions.observeHistory().first().size)
    }

    @Test
    fun invalidEditsLeaveOriginalUntouched() = runBlocking {
        val id = transactions.saveManualTransaction(food, "expense", 25000, date(9), "awal")
        val original = database.transactionDao().getById(id)
        rejected { transactions.updateTransaction(id, salary, "expense", 30000, date(9), "") }
        rejected { transactions.updateTransaction(id, food, "expense", 0, date(9), "") }
        rejected { transactions.updateTransaction(id, food, "expense", -1, date(9), "") }
        rejected { transactions.updateTransaction(id, food, "invalid", 30000, date(9), "") }
        rejected { transactions.updateTransaction(id, Long.MAX_VALUE, "expense", 30000, date(9), "") }
        rejected { transactions.updateTransaction(id, food, "expense", 30000, date(9), "x".repeat(501)) }
        rejected { transactions.updateTransaction(Long.MAX_VALUE, food, "expense", 30000, date(9), "") }
        assertEquals(original, database.transactionDao().getById(id))
        assertEquals(1, transactions.observeHistory().first().size)
    }

    @Test
    fun deleteOnlyTargetKeepsCategoryAndPlan() = runBlocking {
        val planId = plans.createPlan(food, 2026, 9, 100000)
        val id = transactions.saveManualTransaction(food, "expense", 25000, date(9), "hapus")
        val otherId = transactions.saveManualTransaction(food, "expense", 7000, date(9), "tetap")
        transactions.deleteTransaction(id)
        assertNull(database.transactionDao().getById(id))
        assertNotNull(database.transactionDao().getById(otherId))
        assertNotNull(database.categoryDao().getById(food))
        assertNotNull(database.financialPlanDao().getById(planId))
        assertEquals(BigInteger.valueOf(7000), spent(9, food))
        rejected { transactions.deleteTransaction(id) }
        assertEquals(1, transactions.observeHistory().first().size)
    }

    @Test
    fun editsMovePlanUsageAcrossCategoryMonthAndType() = runBlocking {
        plans.createPlan(food, 2026, 9, 100000)
        plans.createPlan(travel, 2026, 9, 100000)
        plans.createPlan(travel, 2026, 10, 100000)
        val id = transactions.saveManualTransaction(food, "expense", 25000, date(9), "")
        transactions.updateTransaction(id, travel, "expense", 40000, date(9), "")
        assertEquals(BigInteger.ZERO, spent(9, food))
        assertEquals(BigInteger.valueOf(40000), spent(9, travel))
        transactions.updateTransaction(id, travel, "expense", 40000, date(10), "")
        assertEquals(BigInteger.ZERO, spent(9, travel))
        assertEquals(BigInteger.valueOf(40000), spent(10, travel))
        transactions.updateTransaction(id, salary, "income", 50000, date(10), "")
        assertEquals(BigInteger.ZERO, spent(10, travel))
        assertEquals("income", transactions.observeHistory().first().single().transaction.transactionType)
        transactions.deleteTransaction(id)
        assertTrue(transactions.observeHistory().first().isEmpty())
    }

    @Test
    fun activeHistoryAndPlanCollectorsReceiveChanges() = runBlocking {
        withTimeout(10000) {
            plans.createPlan(food, 2026, 9, 100000)
            val id = transactions.saveManualTransaction(food, "expense", 25000, date(9), "")
            val historyReady = CompletableDeferred<Unit>()
            val planReady = CompletableDeferred<Unit>()
            val nextHistory = async {
                transactions.observeHistory().onEach { historyReady.complete(Unit) }
                    .first { it.singleOrNull()?.transaction?.amount == 50000L }
            }
            val nextPlan = async {
                plans.observeMonth(2026, 9).onEach { planReady.complete(Unit) }
                    .first { it.plans.singleOrNull()?.usage?.spent == BigInteger.valueOf(50000) }
            }
            historyReady.await()
            planReady.await()
            transactions.updateTransaction(id, food, "expense", 50000, date(9), "")
            assertEquals(50000L, nextHistory.await().single().transaction.amount)
            assertEquals(BigInteger.valueOf(50000), nextPlan.await().plans.single().usage.spent)
        }
        Unit
    }

    private suspend fun spent(month: Int, category: Long): BigInteger =
        withTimeout(5000) {
            plans.observeMonth(2026, month).first().plans.single { it.category.categoryId == category }.usage.spent
        }

    private suspend fun rejected(operation: suspend () -> Unit) {
        try {
            operation()
            fail("Seharusnya isian ditolak")
        } catch (_: IllegalArgumentException) {
            // Validasi repository diharapkan menolak operasi ini.
        }
    }

    private fun date(month: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, month - 1, 10, 12, 0, 0)
    }.timeInMillis
}
