package com.fadhil.financereceipt.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fadhil.financereceipt.data.local.dao.FinancialPlanDao
import com.fadhil.financereceipt.data.local.entity.FinancialPlanEntity
import com.fadhil.financereceipt.data.local.dao.CategoryDao
import com.fadhil.financereceipt.data.local.dao.TransactionDao
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import com.fadhil.financereceipt.data.local.entity.ReceiptExtractionEntity
import com.fadhil.financereceipt.data.local.dao.ReceiptExtractionDao

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        FinancialPlanEntity::class,
        ReceiptExtractionEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun financialPlanDao(): FinancialPlanDao

    abstract fun receiptExtractionDao(): ReceiptExtractionDao

    companion object {

        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions` (
                        `transaction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `transaction_type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `transaction_date` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`)
                            REFERENCES `categories`(`category_id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_transactions_category_id`
                    ON `transactions` (`category_id`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_transactions_transaction_date`
                    ON `transactions` (`transaction_date`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_plans` (
                        `plan_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `plan_year` INTEGER NOT NULL,
                        `plan_month` INTEGER NOT NULL,
                        `budget_amount` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`)
                            REFERENCES `categories`(`category_id`)
                            ON UPDATE NO ACTION
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_plans_category_id_plan_year_plan_month`
                    ON `financial_plans` (`category_id`, `plan_year`, `plan_month`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_financial_plans_plan_year_plan_month`
                    ON `financial_plans` (`plan_year`, `plan_month`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `receipt_extractions` (
                        `extraction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transaction_id` INTEGER NOT NULL,
                        `store_name` TEXT NOT NULL,
                        `receipt_date` INTEGER NOT NULL,
                        `total_amount` INTEGER NOT NULL,
                        `raw_ocr_text` TEXT NOT NULL,
                        `normalized_text` TEXT NOT NULL,
                        `image_path` TEXT NOT NULL,
                        `detected_store_name` TEXT,
                        `detected_receipt_date` INTEGER,
                        `detected_total_amount` INTEGER,
                        `parser_version` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`transaction_id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_receipt_extractions_transaction_id`
                    ON `receipt_extractions` (`transaction_id`)
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `financial_group` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `recommended_percentage` INTEGER DEFAULT NULL")

                // Pemetaan satu kali. Nama, ID, emoji, dan relasi kategori lama tetap utuh.
                // Bekukan aturan migrasi ini; perubahan berikutnya memakai migrasi baru.
                db.execSQL("""
                    UPDATE `categories`
                    SET `financial_group` = CASE
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'makanan', 'transportasi', 'tagihan', 'tagihan pokok',
                            'kesehatan', 'pendidikan', 'tempat tinggal'
                        ) THEN 'NEEDS'
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'hiburan', 'belanja', 'shopping', 'traveling', 'hobi'
                        ) THEN 'WANTS'
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'tabungan', 'hutang', 'investasi', 'dana darurat'
                        ) THEN 'SAVINGS'
                        ELSE NULL
                    END
                    WHERE `transaction_type` = 'expense'
                """.trimIndent())
                db.execSQL("""
                    UPDATE `categories`
                    SET `recommended_percentage` = CASE `financial_group`
                        WHEN 'NEEDS' THEN 50
                        WHEN 'WANTS' THEN 30
                        WHEN 'SAVINGS' THEN 20
                        ELSE NULL
                    END
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_receipt.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
