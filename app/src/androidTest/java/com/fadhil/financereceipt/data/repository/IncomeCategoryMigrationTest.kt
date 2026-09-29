package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.IncomeGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomeCategoryMigrationTest {
    @Test fun version5UpgradePreservesHistoryAndSeedsWithoutReplacingIds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "income_migration_${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                old.execSQL("""CREATE TABLE IF NOT EXISTS `categories` (`category_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_name` TEXT NOT NULL, `transaction_type` TEXT NOT NULL, `emoji` TEXT NOT NULL, `created_at` INTEGER NOT NULL)""")
                old.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_category_name_transaction_type` ON `categories` (`category_name`, `transaction_type`)""")
                old.execSQL("""CREATE TABLE IF NOT EXISTS `transactions` (`transaction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `transaction_type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `transaction_date` INTEGER NOT NULL, `note` TEXT NOT NULL, `source` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )""")
                old.execSQL("""CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)""")
                old.execSQL("""CREATE INDEX IF NOT EXISTS `index_transactions_transaction_date` ON `transactions` (`transaction_date`)""")
                old.execSQL("""CREATE TABLE IF NOT EXISTS `financial_plans` (`plan_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `plan_year` INTEGER NOT NULL, `plan_month` INTEGER NOT NULL, `budget_amount` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )""")
                old.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_plans_category_id_plan_year_plan_month` ON `financial_plans` (`category_id`, `plan_year`, `plan_month`)""")
                old.execSQL("""CREATE INDEX IF NOT EXISTS `index_financial_plans_plan_year_plan_month` ON `financial_plans` (`plan_year`, `plan_month`)""")
                old.execSQL("""CREATE TABLE IF NOT EXISTS `receipt_extractions` (`extraction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `store_name` TEXT NOT NULL, `receipt_date` INTEGER NOT NULL, `total_amount` INTEGER NOT NULL, `raw_ocr_text` TEXT NOT NULL, `normalized_text` TEXT NOT NULL, `image_path` TEXT NOT NULL, `detected_store_name` TEXT, `detected_receipt_date` INTEGER, `detected_total_amount` INTEGER, `parser_version` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`transaction_id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
                old.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_receipt_extractions_transaction_id` ON `receipt_extractions` (`transaction_id`)""")
                old.execSQL("ALTER TABLE categories ADD COLUMN financial_group TEXT DEFAULT NULL")
                old.execSQL("ALTER TABLE categories ADD COLUMN recommended_percentage INTEGER DEFAULT NULL")
                old.execSQL("INSERT INTO categories VALUES(1, 'Gaji', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(2, 'Bonus', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(3, 'Uang Saku', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(4, 'Lainnya', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(5, 'Kategori khusus', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(6, 'Makanan', 'expense', 'X', 1234, 'NEEDS', 50)")
                old.execSQL("INSERT INTO categories VALUES(7, 'Business Profit', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(8, 'Dividend', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO categories VALUES(9, 'Refund', 'income', 'X', 1234, NULL, NULL)")
                old.execSQL("INSERT INTO transactions VALUES(1, 1, 'income', 5000000, 1234, 'gaji lama', 'manual', 1234)")
                old.execSQL("INSERT INTO transactions VALUES(2, 6, 'expense', 25000, 1234, 'struk lama', 'scan_struk', 1234)")
                old.execSQL("INSERT INTO financial_plans VALUES(1, 6, 2026, 9, 1000000, 1234, 1234)")
                old.execSQL("""INSERT INTO receipt_extractions VALUES(1, 2, 'Toko Lama', 1234, 25000,
                    'teks asli', 'teks normal', 'receipts/old.jpg', 'Toko Lama', 1234, 25000, 'lama', 1234)""")
                old.version = 5
            }
            val db = Room.databaseBuilder(context, FinanceDatabase::class.java, name)
                .addMigrations(FinanceDatabase.MIGRATION_5_6, FinanceDatabase.MIGRATION_6_7, FinanceDatabase.MIGRATION_7_8).build()
            try {
                val dao = db.categoryDao()
                assertEquals("EMPLOYMENT", dao.getById(1)?.incomeGroup)
                assertEquals("EMPLOYMENT", dao.getById(2)?.incomeGroup)
                assertEquals("OTHER", dao.getById(3)?.incomeGroup)
                assertEquals("OTHER", dao.getById(4)?.incomeGroup)
                assertNull(dao.getById(5)?.incomeGroup)
                assertEquals("BUSINESS", dao.getById(7)?.incomeGroup)
                assertEquals("INVESTMENT", dao.getById(8)?.incomeGroup)
                assertEquals("OTHER", dao.getById(9)?.incomeGroup)
                val before = withTimeout(5000) { dao.observeAll().first() }
                val expense = dao.getById(6)
                val oldIncome = db.transactionDao().getById(1)
                val oldExpense = db.transactionDao().getById(2)
                val oldPlan = db.financialPlanDao().getById(1)
                val oldReceipt = db.receiptExtractionDao().getByTransactionId(2)
                assertEquals(5000000L, oldIncome?.amount)
                assertEquals(1L, oldIncome?.categoryId)
                assertEquals("gaji lama", oldIncome?.note)
                assertEquals("Toko Lama", oldReceipt?.storeName)
                assertEquals(25000L, oldReceipt?.totalAmount)
                assertEquals("NEEDS", expense?.financialGroup)
                assertEquals(50, expense?.recommendedPercentage)
                assertNull(expense?.incomeGroup)
                val repository = CategoryRepository(db)
                repository.initializeDefaultCategories()
                val after = withTimeout(5000) { dao.observeAll().first() }
                repository.initializeDefaultCategories()
                assertEquals(after, withTimeout(5000) { dao.observeAll().first() })
                before.forEach { assertEquals(it, dao.getById(it.categoryId)) }
                assertEquals(oldIncome, db.transactionDao().getById(1))
                assertEquals(oldExpense, db.transactionDao().getById(2))
                assertEquals(oldPlan, db.financialPlanDao().getById(1))
                assertEquals(oldReceipt, db.receiptExtractionDao().getByTransactionId(2))
                val freelancer = withTimeout(5000) {
                    repository.observeByIncomeGroup(IncomeGroup.BUSINESS).first()
                }.single { it.categoryName == "Freelance" }
                val txRepository = TransactionRepository(db)
                val newId = txRepository.saveManualTransaction(freelancer.categoryId, "income", 100000, 5678, "proyek")
                assertEquals("manual", db.transactionDao().getById(newId)?.source)
                txRepository.updateTransaction(1, freelancer.categoryId, "income", 5000000, 1234, "gaji lama")
                assertEquals(oldIncome?.copy(categoryId = freelancer.categoryId), db.transactionDao().getById(1))
                // Kategori custom tanpa kelompok juga masih dapat dipakai.
                txRepository.saveManualTransaction(5, "income", 1000, 5678, "lama")
                db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun freshInstallSeedsSeventeenIncomeSubcategoriesAndKeepsExpenseDefaults() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, FinanceDatabase::class.java
        ).build()
        try {
            val repo = CategoryRepository(db)
            repo.initializeDefaultCategories()
            val rows = withTimeout(5000) { repo.observeAll().first() }
            assertEquals(31, rows.size)
            val incomes = rows.filter { it.transactionType == "income" }
            assertEquals(mapOf("EMPLOYMENT" to 5, "BUSINESS" to 4, "INVESTMENT" to 3, "OTHER" to 5),
                incomes.groupingBy { it.incomeGroup }.eachCount())
            assertTrue(incomes.all { it.financialGroup == null && it.recommendedPercentage == null })
            val expenses = rows.filter { it.transactionType == "expense" }
            assertEquals(mapOf("NEEDS" to 6, "WANTS" to 5, "SAVINGS" to 3),
                expenses.groupingBy { it.financialGroup }.eachCount())
            assertTrue(expenses.all { it.incomeGroup == null })
        } finally { db.close() }
    }
}
