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
    version = 8,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `income_group` TEXT DEFAULT NULL")
                // Aturan satu kali dibekukan di sini, tidak mengikuti enum/default masa depan.
                // Tidak mengganti nama/ID, menimpa transaksi, atau menebak nama yang ambigu.
                db.execSQL("""
                    UPDATE `categories`
                    SET `income_group` = CASE
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'gaji', 'salary', 'bonus', 'tunjangan', 'honor', 'komisi'
                        ) THEN 'EMPLOYMENT'
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'business profit', 'freelance', 'sales', 'project income'
                        ) THEN 'BUSINESS'
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'dividend', 'rental income', 'investment return'
                        ) THEN 'INVESTMENT'
                        WHEN LOWER(TRIM(`category_name`)) IN (
                            'hadiah', 'cashback', 'refund', 'uang saku', 'lainnya'
                        ) THEN 'OTHER'
                        ELSE NULL
                    END
                    WHERE `transaction_type` = 'income'
                """.trimIndent())
            }
        }

        // Migrasi data dalam transaksi upgrade Room. Prioritaskan ID Tagihan yang
        // sudah ada; bila hanya Tagihan Pokok tersedia, pertahankan ID tersebut.
        // Anggaran periode sama dijumlahkan dan memakai ID rencana terkecil.
        // SUM integer menolak overflow agar upgrade rollback, bukan membulatkan nilai.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TEMP TABLE _bill_ids AS
                    SELECT category_id FROM categories
                    WHERE transaction_type = 'expense'
                      AND LOWER(TRIM(category_name)) IN ('tagihan', 'tagihan pokok')
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _bill_target AS
                    SELECT category_id FROM categories
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                    ORDER BY CASE WHEN category_name = 'Tagihan' THEN 0
                                  WHEN LOWER(TRIM(category_name)) = 'tagihan' THEN 1 ELSE 2 END, category_id
                    LIMIT 1
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _bill_plans AS
                    SELECT MIN(plan_id) AS keep_id, plan_year, plan_month,
                           SUM(budget_amount) AS budget_amount,
                           MIN(created_at) AS created_at, MAX(updated_at) AS updated_at
                    FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                    GROUP BY plan_year, plan_month
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                      AND plan_id NOT IN (SELECT keep_id FROM _bill_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE financial_plans
                    SET category_id = (SELECT category_id FROM _bill_target),
                        budget_amount = (SELECT budget_amount FROM _bill_plans WHERE keep_id = financial_plans.plan_id),
                        created_at = (SELECT created_at FROM _bill_plans WHERE keep_id = financial_plans.plan_id),
                        updated_at = (SELECT updated_at FROM _bill_plans WHERE keep_id = financial_plans.plan_id)
                    WHERE plan_id IN (SELECT keep_id FROM _bill_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE transactions SET category_id = (SELECT category_id FROM _bill_target)
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM categories
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                      AND category_id NOT IN (SELECT category_id FROM _bill_target)
                """.trimIndent())
                db.execSQL("""
                    UPDATE categories
                    SET category_name = 'Tagihan', financial_group = 'NEEDS',
                        recommended_percentage = 50, income_group = NULL
                    WHERE category_id IN (SELECT category_id FROM _bill_target)
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _bill_plans
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _bill_target
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _bill_ids
                """.trimIndent())
            }
        }

        // Aturan migrasi dibekukan. Transaksi/struk tetap memakai ID transaksi lama.
        // Kategori tujuan diutamakan; anggaran periode sama dijumlahkan.
        // Nama custom yang tidak dikenal tidak diklasifikasikan secara otomatis.
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bersihkan juga Tagihan Pokok yang mungkin masih tersisa pada data versi 7.
                db.execSQL("""
                    CREATE TEMP TABLE _bill_ids AS
                    SELECT category_id FROM categories
                    WHERE transaction_type = 'expense'
                      AND LOWER(TRIM(category_name)) IN ('tagihan', 'tagihan pokok')
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _bill_target AS
                    SELECT category_id FROM categories
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                    ORDER BY CASE WHEN category_name = 'Tagihan' THEN 0
                                  WHEN LOWER(TRIM(category_name)) = 'tagihan' THEN 1 ELSE 2 END, category_id
                    LIMIT 1
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _bill_plans AS
                    SELECT MIN(plan_id) AS keep_id, plan_year, plan_month,
                           SUM(budget_amount) AS budget_amount,
                           MIN(created_at) AS created_at, MAX(updated_at) AS updated_at
                    FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                    GROUP BY plan_year, plan_month
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                      AND plan_id NOT IN (SELECT keep_id FROM _bill_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE financial_plans
                    SET category_id = (SELECT category_id FROM _bill_target),
                        budget_amount = (SELECT budget_amount FROM _bill_plans WHERE keep_id = financial_plans.plan_id),
                        created_at = (SELECT created_at FROM _bill_plans WHERE keep_id = financial_plans.plan_id),
                        updated_at = (SELECT updated_at FROM _bill_plans WHERE keep_id = financial_plans.plan_id)
                    WHERE plan_id IN (SELECT keep_id FROM _bill_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE transactions SET category_id = (SELECT category_id FROM _bill_target)
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM categories
                    WHERE category_id IN (SELECT category_id FROM _bill_ids)
                      AND category_id NOT IN (SELECT category_id FROM _bill_target)
                """.trimIndent())
                db.execSQL("""
                    UPDATE categories
                    SET category_name = 'Tagihan', financial_group = 'NEEDS',
                        recommended_percentage = 50, income_group = NULL
                    WHERE category_id IN (SELECT category_id FROM _bill_target)
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _bill_plans
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _bill_target
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _bill_ids
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _shopping_ids AS
                    SELECT category_id FROM categories
                    WHERE transaction_type = 'expense'
                      AND LOWER(TRIM(category_name)) IN ('shopping', 'belanja')
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _shopping_target AS
                    SELECT category_id FROM categories
                    WHERE category_id IN (SELECT category_id FROM _shopping_ids)
                    ORDER BY CASE WHEN category_name = 'Belanja' THEN 0
                                  WHEN LOWER(TRIM(category_name)) = 'belanja' THEN 1 ELSE 2 END, category_id
                    LIMIT 1
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _shopping_plans AS
                    SELECT MIN(plan_id) AS keep_id, plan_year, plan_month,
                           SUM(budget_amount) AS budget_amount,
                           MIN(created_at) AS created_at, MAX(updated_at) AS updated_at
                    FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _shopping_ids)
                    GROUP BY plan_year, plan_month
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _shopping_ids)
                      AND plan_id NOT IN (SELECT keep_id FROM _shopping_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE financial_plans
                    SET category_id = (SELECT category_id FROM _shopping_target),
                        budget_amount = (SELECT budget_amount FROM _shopping_plans WHERE keep_id = financial_plans.plan_id),
                        created_at = (SELECT created_at FROM _shopping_plans WHERE keep_id = financial_plans.plan_id),
                        updated_at = (SELECT updated_at FROM _shopping_plans WHERE keep_id = financial_plans.plan_id)
                    WHERE plan_id IN (SELECT keep_id FROM _shopping_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE transactions SET category_id = (SELECT category_id FROM _shopping_target)
                    WHERE category_id IN (SELECT category_id FROM _shopping_ids)
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM categories
                    WHERE category_id IN (SELECT category_id FROM _shopping_ids)
                      AND category_id NOT IN (SELECT category_id FROM _shopping_target)
                """.trimIndent())
                db.execSQL("""
                    UPDATE categories
                    SET category_name = 'Belanja', emoji = '🛍️', financial_group = 'WANTS',
                        recommended_percentage = 30, income_group = NULL
                    WHERE category_id IN (SELECT category_id FROM _shopping_target)
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _shopping_plans
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _shopping_target
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _shopping_ids
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _lifestyle_ids AS
                    SELECT category_id FROM categories
                    WHERE transaction_type = 'expense'
                      AND LOWER(TRIM(category_name)) IN ('lifestyle', 'perawatan diri')
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _lifestyle_target AS
                    SELECT category_id FROM categories
                    WHERE category_id IN (SELECT category_id FROM _lifestyle_ids)
                    ORDER BY CASE WHEN category_name = 'Perawatan Diri' THEN 0
                                  WHEN LOWER(TRIM(category_name)) = 'perawatan diri' THEN 1 ELSE 2 END, category_id
                    LIMIT 1
                """.trimIndent())
                db.execSQL("""
                    CREATE TEMP TABLE _lifestyle_plans AS
                    SELECT MIN(plan_id) AS keep_id, plan_year, plan_month,
                           SUM(budget_amount) AS budget_amount,
                           MIN(created_at) AS created_at, MAX(updated_at) AS updated_at
                    FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _lifestyle_ids)
                    GROUP BY plan_year, plan_month
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM financial_plans
                    WHERE category_id IN (SELECT category_id FROM _lifestyle_ids)
                      AND plan_id NOT IN (SELECT keep_id FROM _lifestyle_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE financial_plans
                    SET category_id = (SELECT category_id FROM _lifestyle_target),
                        budget_amount = (SELECT budget_amount FROM _lifestyle_plans WHERE keep_id = financial_plans.plan_id),
                        created_at = (SELECT created_at FROM _lifestyle_plans WHERE keep_id = financial_plans.plan_id),
                        updated_at = (SELECT updated_at FROM _lifestyle_plans WHERE keep_id = financial_plans.plan_id)
                    WHERE plan_id IN (SELECT keep_id FROM _lifestyle_plans)
                """.trimIndent())
                db.execSQL("""
                    UPDATE transactions SET category_id = (SELECT category_id FROM _lifestyle_target)
                    WHERE category_id IN (SELECT category_id FROM _lifestyle_ids)
                """.trimIndent())
                db.execSQL("""
                    DELETE FROM categories
                    WHERE category_id IN (SELECT category_id FROM _lifestyle_ids)
                      AND category_id NOT IN (SELECT category_id FROM _lifestyle_target)
                """.trimIndent())
                db.execSQL("""
                    UPDATE categories
                    SET category_name = 'Perawatan Diri', emoji = '🧴', financial_group = 'WANTS',
                        recommended_percentage = 30, income_group = NULL
                    WHERE category_id IN (SELECT category_id FROM _lifestyle_target)
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _lifestyle_plans
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _lifestyle_target
                """.trimIndent())
                db.execSQL("""
                    DROP TABLE _lifestyle_ids
                """.trimIndent())
                db.execSQL("""
                    UPDATE categories
                    SET financial_group = CASE
                        WHEN LOWER(TRIM(category_name)) IN ('makanan', 'transportasi', 'tempat tinggal', 'tagihan', 'kesehatan', 'pendidikan') THEN 'NEEDS'
                        WHEN LOWER(TRIM(category_name)) IN ('belanja', 'hiburan', 'hobi', 'traveling', 'perawatan diri') THEN 'WANTS'
                        WHEN LOWER(TRIM(category_name)) IN ('tabungan', 'dana darurat', 'investasi', 'hutang') THEN 'SAVINGS'
                        ELSE financial_group END
                    WHERE transaction_type = 'expense' AND financial_group IS NULL
                """.trimIndent())
                db.execSQL("""
                    UPDATE categories
                    SET recommended_percentage = CASE financial_group
                        WHEN 'NEEDS' THEN 50 WHEN 'WANTS' THEN 30 WHEN 'SAVINGS' THEN 20 END
                    WHERE transaction_type = 'expense' AND financial_group IN ('NEEDS', 'WANTS', 'SAVINGS')
                """.trimIndent())
                // Biasanya ini hanya label UI, bukan baris database. Bersihkan bila pernah dibuat.
                db.execSQL("""
                    DELETE FROM categories
                    WHERE LOWER(TRIM(category_name)) = 'belum dikelompokkan'
                      AND NOT EXISTS (SELECT 1 FROM transactions t WHERE t.category_id = categories.category_id)
                      AND NOT EXISTS (SELECT 1 FROM financial_plans p WHERE p.category_id = categories.category_id)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
