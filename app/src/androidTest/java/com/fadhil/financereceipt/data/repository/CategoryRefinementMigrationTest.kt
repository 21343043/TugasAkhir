package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryRefinementMigrationTest {
    @Test fun version7UpgradeValidatesRoomSchemaAndMergesCategories() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "category_refinement_${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                // Tabel versi 7 sama dengan export versi 6; versi 7 hanya mengubah data.
                old.execSQL("""CREATE TABLE IF NOT EXISTS `categories` (`category_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_name` TEXT NOT NULL, `transaction_type` TEXT NOT NULL, `emoji` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `financial_group` TEXT DEFAULT NULL, `recommended_percentage` INTEGER DEFAULT NULL, `income_group` TEXT DEFAULT NULL)""")
                old.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_category_name_transaction_type` ON `categories` (`category_name`, `transaction_type`)""")
                old.execSQL("""CREATE TABLE IF NOT EXISTS `transactions` (`transaction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `transaction_type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `transaction_date` INTEGER NOT NULL, `note` TEXT NOT NULL, `source` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )""")
                old.execSQL("""CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)""")
                old.execSQL("""CREATE INDEX IF NOT EXISTS `index_transactions_transaction_date` ON `transactions` (`transaction_date`)""")
                old.execSQL("""CREATE TABLE IF NOT EXISTS `financial_plans` (`plan_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `plan_year` INTEGER NOT NULL, `plan_month` INTEGER NOT NULL, `budget_amount` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )""")
                old.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_plans_category_id_plan_year_plan_month` ON `financial_plans` (`category_id`, `plan_year`, `plan_month`)""")
                old.execSQL("""CREATE INDEX IF NOT EXISTS `index_financial_plans_plan_year_plan_month` ON `financial_plans` (`plan_year`, `plan_month`)""")
                old.execSQL("""CREATE TABLE IF NOT EXISTS `receipt_extractions` (`extraction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `store_name` TEXT NOT NULL, `receipt_date` INTEGER NOT NULL, `total_amount` INTEGER NOT NULL, `raw_ocr_text` TEXT NOT NULL, `normalized_text` TEXT NOT NULL, `image_path` TEXT NOT NULL, `detected_store_name` TEXT, `detected_receipt_date` INTEGER, `detected_total_amount` INTEGER, `parser_version` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`transaction_id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
                old.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_receipt_extractions_transaction_id` ON `receipt_extractions` (`transaction_id`)""")
                old.execSQL("INSERT INTO categories VALUES(1,'Shopping','expense','X',1234,'WANTS',30,NULL)")
                old.execSQL("INSERT INTO categories VALUES(2,'Belanja','expense','Y',1234,'WANTS',30,NULL)")
                old.execSQL("INSERT INTO categories VALUES(3,'Lifestyle','expense','X',1234,'WANTS',30,NULL)")
                old.execSQL("INSERT INTO categories VALUES(4,'Belum dikelompokkan','expense','X',1234,NULL,NULL,NULL)")
                old.execSQL("INSERT INTO transactions VALUES(1,1,'expense',25000,1234,'struk','scan_struk',5678)")
                old.execSQL("INSERT INTO financial_plans VALUES(1,1,2026,9,100000,100,200)")
                old.execSQL("INSERT INTO financial_plans VALUES(2,2,2026,9,200000,90,300)")
                old.execSQL("INSERT INTO receipt_extractions VALUES(1,1,'Toko',1234,25000,'OCR','normalized','a.jpg','Toko',1234,25000,'v1',5678)")
                old.version = 7
            }
            val db = Room.databaseBuilder(context, FinanceDatabase::class.java, name)
                .addMigrations(FinanceDatabase.MIGRATION_7_8).build()
            try {
                val categories = db.categoryDao()
                assertNull(categories.getById(1))
                assertEquals("Belanja", categories.getById(2)?.categoryName)
                assertEquals("Perawatan Diri", categories.getById(3)?.categoryName)
                assertNull(categories.getById(4))
                assertEquals("WANTS", categories.getById(3)?.financialGroup)
                assertEquals(30, categories.getById(3)?.recommendedPercentage)
                val old = db.transactionDao().getById(1)!!
                assertEquals(2L, old.categoryId)
                assertEquals(25000L, old.amount)
                assertEquals(1234L, old.transactionDate)
                assertEquals(5678L, old.createdAt)
                assertEquals("scan_struk", old.source)
                assertEquals("struk", old.note)
                val receipt = db.receiptExtractionDao().getByTransactionId(1)!!
                assertEquals("Toko", receipt.storeName)
                assertEquals("OCR", receipt.rawOcrText)
                assertEquals(25000L, receipt.totalAmount)
                assertEquals("a.jpg", receipt.imagePath)
                val plan = db.financialPlanDao().getByCategoryAndMonth(2,2026,9)!!
                assertEquals(300000L, plan.budgetAmount)
                assertEquals(90L, plan.createdAt)
                assertEquals(300L, plan.updatedAt)
                val repo = CategoryRepository(db)
                repo.initializeDefaultCategories()
                val once = categories.getAll()
                repo.initializeDefaultCategories()
                assertEquals(once, categories.getAll())
                val choices = categories.observeByType("expense").first()
                assertEquals(14, choices.size)
                assertTrue(choices.all { it.hasValidExpenseGroup() })
                assertEquals(setOf("Belanja","Hiburan","Hobi","Traveling","Perawatan Diri"),
                    choices.filter { it.financialGroup == "WANTS" }.map { it.categoryName }.toSet())
                val transactions = TransactionRepository(db)
                val id = transactions.saveManualTransaction(3,"expense",10000,1234,"baru")
                assertEquals(3L, db.transactionDao().getById(id)?.categoryId)
                transactions.updateTransaction(1,2,"expense",25000,1234,"struk")
                assertEquals(old, db.transactionDao().getById(1))
                assertEquals(receipt, db.receiptExtractionDao().getByTransactionId(1))
                db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
