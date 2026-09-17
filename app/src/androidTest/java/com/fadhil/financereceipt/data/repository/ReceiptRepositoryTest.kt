package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.ReceiptExtractionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptRepositoryTest {
    private lateinit var database: FinanceDatabase
    private lateinit var repository: ReceiptRepository
    private var categoryId = 0L

    @Before fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, FinanceDatabase::class.java
        ).build()
        repository = ReceiptRepository(database)
        categoryId = database.categoryDao().insert(CategoryEntity(
            categoryName = "Belanja", transactionType = "expense", emoji = "🛒"))
    }
    @After fun tearDown() { database.close() }

    @Test fun savesExpenseAndKeepsUncorrectedMachineOutput() = runBlocking {
        val id = repository.save(categoryId, "", receipt())
        val transaction = requireNotNull(database.transactionDao().getById(id))
        val extraction = requireNotNull(database.receiptExtractionDao().getByTransactionId(id))
        assertEquals("expense", transaction.transactionType)
        assertEquals("scan_struk", transaction.source)
        assertEquals(27500L, transaction.amount)
        assertEquals("TOKO MAJU", transaction.note)
        assertEquals(25000L, extraction.detectedTotalAmount)
        assertEquals(27500L, extraction.totalAmount)
        assertEquals(receipt().rawOcrText, extraction.rawOcrText)
    }

    @Test fun rejectsIncomeCategoryWithoutInsertingRows() = runBlocking {
        val income = database.categoryDao().insert(CategoryEntity(
            categoryName = "Gaji", transactionType = "income", emoji = "💼"))
        try { repository.save(income, "", receipt()); fail("Harus menolak kategori pemasukan") }
        catch (_: IllegalArgumentException) { }
        assertTrue(database.transactionDao().observeWithCategory().first().isEmpty())
        assertTrue(database.receiptExtractionDao().getImagePaths().isEmpty())
    }

    @Test fun extractionFailureRollsBackExpense() = runBlocking {
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_receipt BEFORE INSERT ON receipt_extractions
            BEGIN SELECT RAISE(ABORT, 'Simulasi kegagalan penyimpanan hasil OCR'); END
        """.trimIndent())
        var failed = false
        try { repository.save(categoryId, "", receipt()) }
        catch (_: android.database.sqlite.SQLiteException) { failed = true }
        assertTrue("Penyimpanan seharusnya gagal", failed)
        assertTrue(database.transactionDao().observeWithCategory().first().isEmpty())
    }

    @Test fun deletingExpenseCascadesExtraction() = runBlocking {
        val id = repository.save(categoryId, "", receipt())
        TransactionRepository(database).deleteTransaction(id)
        assertNull(database.receiptExtractionDao().getByTransactionId(id))
        assertNotNull(database.categoryDao().getById(categoryId))
    }

    @Test fun laterTransactionEditDoesNotOverwriteOriginalScan() = runBlocking {
        val id = repository.save(categoryId, "", receipt())
        val snapshot = database.receiptExtractionDao().getByTransactionId(id)
        TransactionRepository(database).updateTransaction(id, categoryId, "expense", 30000, 1234L, "revisi")
        assertEquals(snapshot, database.receiptExtractionDao().getByTransactionId(id))
        assertEquals(30000L, database.transactionDao().getById(id)?.amount)
    }

    private fun receipt() = ReceiptExtractionEntity(transactionId = 0,
        storeName = "TOKO MAJU", receiptDate = 1789128000000L, totalAmount = 27500,
        rawOcrText = "TOKO MAJU\nTotal 25000", normalizedText = "TOKO MAJU\nTotal 25000",
        imagePath = "receipts/test.jpg", detectedStoreName = "TOKO MAJU",
        detectedReceiptDate = null, detectedTotalAmount = 25000, parserVersion = "rules-1.0")
}
