package com.fadhil.financereceipt.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Buat DB v3 sesuai skema asli; Room memvalidasi skema v6 setelah migrasi kategori. */
@RunWith(AndroidJUnit4::class)
class ReceiptMigrationTest {
    @Test fun migrationPreservesCategoryTransactionAndPlan() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "receipt_migration_${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                old.execSQL("""CREATE TABLE categories (
                    category_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category_name TEXT NOT NULL, transaction_type TEXT NOT NULL,
                    emoji TEXT NOT NULL, created_at INTEGER NOT NULL)""")
                old.execSQL("CREATE UNIQUE INDEX index_categories_category_name_transaction_type ON categories(category_name, transaction_type)")
                old.execSQL("""CREATE TABLE transactions (
                    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category_id INTEGER NOT NULL, transaction_type TEXT NOT NULL,
                    amount INTEGER NOT NULL, transaction_date INTEGER NOT NULL,
                    note TEXT NOT NULL, source TEXT NOT NULL, created_at INTEGER NOT NULL,
                    FOREIGN KEY(category_id) REFERENCES categories(category_id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                old.execSQL("CREATE INDEX index_transactions_category_id ON transactions(category_id)")
                old.execSQL("CREATE INDEX index_transactions_transaction_date ON transactions(transaction_date)")
                old.execSQL("""CREATE TABLE financial_plans (
                    plan_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category_id INTEGER NOT NULL, plan_year INTEGER NOT NULL, plan_month INTEGER NOT NULL,
                    budget_amount INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
                    FOREIGN KEY(category_id) REFERENCES categories(category_id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                old.execSQL("CREATE UNIQUE INDEX index_financial_plans_category_id_plan_year_plan_month ON financial_plans(category_id, plan_year, plan_month)")
                old.execSQL("CREATE INDEX index_financial_plans_plan_year_plan_month ON financial_plans(plan_year, plan_month)")
                old.execSQL("INSERT INTO categories VALUES(1, 'Belanja', 'expense', '🛒', 1234)")
                old.execSQL("INSERT INTO transactions VALUES(1, 1, 'expense', 25000, 1234, 'lama', 'manual', 1234)")
                old.execSQL("INSERT INTO financial_plans VALUES(1, 1, 2026, 9, 100000, 1234, 1234)")
                old.version = 3
            }
            val migrated = Room.databaseBuilder(context, FinanceDatabase::class.java, name)
                .addMigrations(FinanceDatabase.MIGRATION_3_4, FinanceDatabase.MIGRATION_4_5,
                    FinanceDatabase.MIGRATION_5_6).build()
            try {
                assertEquals("Belanja", migrated.categoryDao().getById(1)?.categoryName)
                assertEquals("WANTS", migrated.categoryDao().getById(1)?.financialGroup)
                assertEquals(30, migrated.categoryDao().getById(1)?.recommendedPercentage)
                assertEquals(25000L, migrated.transactionDao().getById(1)?.amount)
                assertEquals(100000L, migrated.financialPlanDao().getById(1)?.budgetAmount)
                assertTrue(migrated.receiptExtractionDao().getImagePaths().isEmpty())
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
