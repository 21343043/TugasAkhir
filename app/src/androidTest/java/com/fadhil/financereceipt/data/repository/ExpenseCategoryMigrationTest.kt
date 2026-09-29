package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseCategoryMigrationTest {
    @Test fun version6UpgradeMergesBillsAndPreservesReceiptAndBudgetTotals() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "expense_migration_${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
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
                old.execSQL("INSERT INTO categories VALUES(10,'Tagihan Pokok','expense','X',1234,'NEEDS',50,NULL)")
                old.execSQL("INSERT INTO categories VALUES(20,'Tagihan','expense','Y',2345,'NEEDS',50,NULL)")
                old.execSQL("INSERT INTO categories VALUES(30,'Hutang','expense','X',1234,'SAVINGS',20,NULL)")
                old.execSQL("INSERT INTO categories VALUES(40,'Tempat tinggal','expense','X',1234,'NEEDS',50,NULL)")
                old.execSQL("INSERT INTO categories VALUES(50,'Dana darurat','expense','X',1234,'SAVINGS',20,NULL)")
                old.execSQL("INSERT INTO transactions VALUES(1,10,'expense',100000,1234,'struk lama','scan_struk',4567)")
                old.execSQL("INSERT INTO transactions VALUES(2,20,'expense',200000,4321,'manual','manual',7654)")
                old.execSQL("INSERT INTO financial_plans VALUES(1,10,2026,9,300000,100,200)")
                old.execSQL("INSERT INTO financial_plans VALUES(2,20,2026,9,400000,90,300)")
                old.execSQL("INSERT INTO financial_plans VALUES(3,10,2026,10,500000,100,200)")
                old.execSQL("""INSERT INTO receipt_extractions VALUES(1,1,'Toko Lama',1234,100000,
                    'OCR asli','normal','receipts/a.jpg','Toko Lama',1234,100000,'v1',4567)""")
                old.version = 6
            }
            val db = Room.databaseBuilder(context, FinanceDatabase::class.java, name)
                .addMigrations(FinanceDatabase.MIGRATION_6_7, FinanceDatabase.MIGRATION_7_8).build()
            try {
                val dao = db.categoryDao()
                assertNull(dao.getById(10))
                assertEquals("Tagihan", dao.getById(20)?.categoryName)
                assertEquals("NEEDS", dao.getById(20)?.financialGroup)
                assertEquals(50, dao.getById(20)?.recommendedPercentage)
                assertEquals("Y", dao.getById(20)?.emoji)
                val first = db.transactionDao().getById(1)!!
                assertEquals(20L, first.categoryId)
                assertEquals(100000L, first.amount)
                assertEquals(1234L, first.transactionDate)
                assertEquals(4567L, first.createdAt)
                assertEquals("struk lama", first.note)
                assertEquals("scan_struk", first.source)
                assertEquals(20L, db.transactionDao().getById(2)?.categoryId)
                val receipt = db.receiptExtractionDao().getByTransactionId(1)!!
                assertEquals("Toko Lama", receipt.storeName)
                assertEquals(1234L, receipt.receiptDate)
                assertEquals(100000L, receipt.totalAmount)
                assertEquals("OCR asli", receipt.rawOcrText)
                assertEquals("receipts/a.jpg", receipt.imagePath)
                val plan = db.financialPlanDao().getByCategoryAndMonth(20, 2026, 9)!!
                assertEquals(1L, plan.planId)
                assertEquals(700000L, plan.budgetAmount)
                assertEquals(90L, plan.createdAt)
                assertEquals(300L, plan.updatedAt)
                assertNull(db.financialPlanDao().getById(2))
                assertEquals(500000L, db.financialPlanDao().getByCategoryAndMonth(20, 2026, 10)?.budgetAmount)
                val repo = CategoryRepository(db)
                repo.initializeDefaultCategories()
                val seeded = dao.getAll()
                repo.initializeDefaultCategories()
                assertEquals(seeded, dao.getAll())
                assertEquals(1, seeded.count { it.transactionType == "expense" && it.categoryName == "Tagihan" })
                assertFalse(seeded.any { it.transactionType == "expense" && it.categoryName == "Tagihan Pokok" })
                assertEquals("Hutang", dao.getById(30)?.categoryName)
                assertEquals("Tempat tinggal", dao.getById(40)?.categoryName)
                assertEquals(1, seeded.count { it.categoryName.equals("Tempat Tinggal", ignoreCase = true) })
                assertEquals(1, seeded.count { it.categoryName.equals("Dana Darurat", ignoreCase = true) })
                assertEquals("WANTS", seeded.single { it.categoryName == "Perawatan Diri" }.financialGroup)
                val transactions = TransactionRepository(db)
                transactions.updateTransaction(1, 20, "expense", 100000, 1234, "struk lama")
                assertEquals(first, db.transactionDao().getById(1))
                assertEquals(receipt, db.receiptExtractionDao().getByTransactionId(1))
                transactions.saveManualTransaction(20, "expense", 25000, 1234, "tagihan baru")
                db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
