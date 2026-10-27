"""SQLite checks using the migration SQL and seed values from production files.
Run: python3 scripts/verification/test_income_migration.py
Does not replace Room/Compose tests on Android.
"""
from pathlib import Path
import json
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src/main/java/com/fadhil/financereceipt"
DATABASE = (SRC / "data/local/database/FinanceDatabase.kt").read_text()
REPOSITORY = (SRC / "data/repository/CategoryRepository.kt").read_text()


def migrate(db, start, end):
    body = DATABASE.split(f"val MIGRATION_{start}_{end} =", 1)[1].split("\n        }", 1)[0]
    sql = re.findall(r'db\.execSQL\(\s*(?:"""(.*?)"""\.trimIndent\(\)|"([^"\n]*)")\s*\)', body, re.S)
    if not sql:
        raise AssertionError("Migration SQL was not found")
    for multi, single in sql:
        db.execute(multi or single)


def seed(db):
    incomes = re.findall(r'income\("([^"]+)", "([^"]+)", IncomeGroup\.(\w+)\)', REPOSITORY)
    expenses = re.findall(r'expense\("([^"]+)", "([^"]+)", CategoryEntity\.(\w+)\)', REPOSITORY)
    for name, emoji, group in incomes:
        db.execute("""INSERT OR IGNORE INTO categories
            (category_name, transaction_type, emoji, created_at, income_group)
            VALUES (?, 'income', ?, 9999, ?)""", (name, emoji, group))
    existing = {r[0].strip().lower() for r in db.execute(
        "SELECT category_name FROM categories WHERE transaction_type='expense'")}
    for name, emoji, group in expenses:
        if name.strip().lower() in existing:
            continue
        db.execute("""INSERT OR IGNORE INTO categories
            (category_name, transaction_type, emoji, created_at, financial_group, recommended_percentage)
            VALUES (?, 'expense', ?, 9999, ?, ?)""", (name, emoji, group, {"NEEDS": 50, "WANTS": 30, "SAVINGS": 20}[group]))


def schema4():
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys=ON")
    schema = json.loads((ROOT / "app/schemas/com.fadhil.financereceipt.data.local.database.FinanceDatabase/4.json").read_text())
    for entity in schema["database"]["entities"]:
        db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for index in entity["indices"]:
            db.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
    return db


class IncomeMigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = schema4()
        self.addCleanup(self.db.close)

    def test_all_source_groups_and_legacy_fallback(self):
        expected = {
            "Gaji": "EMPLOYMENT", "Salary": "EMPLOYMENT", " BONUS ": "EMPLOYMENT",
            "Tunjangan": "EMPLOYMENT", "Honor": "EMPLOYMENT", "Komisi": "EMPLOYMENT",
            "Business Profit": "BUSINESS", "Freelance": "BUSINESS", "Sales": "BUSINESS", "Project Income": "BUSINESS",
            "Dividend": "INVESTMENT", "Rental Income": "INVESTMENT", "Investment Return": "INVESTMENT",
            "Hadiah": "OTHER", "Cashback": "OTHER", "Refund": "OTHER", "Uang Saku": "OTHER", "Lainnya": "OTHER",
            "Pemasukan khusus": None, "Investasi": None,
        }
        for name in expected:
            self.db.execute("INSERT INTO categories(category_name,transaction_type,emoji,created_at) VALUES(?,'income','X',1234)", (name,))
        migrate(self.db, 4, 5)
        before = self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall()
        migrate(self.db, 5, 6)
        after = self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall()
        self.assertEqual(before, [row[:-1] for row in after])
        self.assertEqual(expected, dict(self.db.execute("SELECT category_name,income_group FROM categories")))
        self.assertTrue(all(row[5:7] == (None, None) for row in after))

    def test_history_receipt_plan_and_expense_metadata_unchanged(self):
        db = self.db
        for name, typ in [("Gaji", "income"), ("Makanan", "expense"), ("Shopping", "expense"), ("Tabungan", "expense")]:
            db.execute("INSERT INTO categories(category_name,transaction_type,emoji,created_at) VALUES(?,?,'X',1234)", (name, typ))
        db.execute("INSERT INTO transactions VALUES(1,1,'income',5000000,1234,'gaji lama','manual',1234)")
        db.execute("INSERT INTO transactions VALUES(2,2,'expense',25000,1234,'struk lama','scan_struk',1234)")
        db.execute("INSERT INTO financial_plans VALUES(1,2,2026,9,1000000,1234,1234)")
        db.execute("INSERT INTO receipt_extractions VALUES(1,2,'Toko Lama',1234,25000,'OCR asli','normal','receipts/a.jpg','Toko Lama',1234,25000,'lama',1234)")
        migrate(db, 4, 5)
        snapshots = {t: db.execute(f"SELECT * FROM {t}").fetchall() for t in ("transactions", "financial_plans", "receipt_extractions")}
        expenses = db.execute("SELECT * FROM categories WHERE transaction_type='expense'").fetchall()
        migrate(db, 5, 6)
        seed(db)
        for table, values in snapshots.items():
            self.assertEqual(values, db.execute(f"SELECT * FROM {table}").fetchall())
        for row in expenses:
            self.assertEqual(row + (None,), db.execute("SELECT * FROM categories WHERE category_id=?", (row[0],)).fetchone())
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())
        self.assertEqual([(1, 'Gaji'), (2, 'Makanan')], db.execute("SELECT t.transaction_id,c.category_name FROM transactions t JOIN categories c USING(category_id) ORDER BY t.transaction_id").fetchall())

    def test_fresh_defaults_and_idempotent_seeding(self):
        migrate(self.db, 4, 5)
        migrate(self.db, 5, 6)
        seed(self.db)
        before = self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall()
        seed(self.db)
        self.assertEqual(before, self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall())
        self.assertEqual(31, len(before))
        self.assertEqual({"EMPLOYMENT": 5, "BUSINESS": 4, "INVESTMENT": 3, "OTHER": 5}, dict(self.db.execute("SELECT income_group,COUNT(*) FROM categories WHERE transaction_type='income' GROUP BY income_group")))
        self.assertEqual({"NEEDS": 6, "WANTS": 5, "SAVINGS": 3}, dict(self.db.execute("SELECT financial_group,COUNT(*) FROM categories WHERE transaction_type='expense' GROUP BY financial_group")))
        # Seeding tidak menimpa pilihan klasifikasi pengguna di versi 6.
        self.db.execute("UPDATE categories SET income_group='OTHER' WHERE category_name='Honor'")
        seed(self.db)
        self.assertEqual("OTHER", self.db.execute("SELECT income_group FROM categories WHERE category_name='Honor'").fetchone()[0])

    def test_legacy_rows_are_not_renamed_replaced_or_deleted(self):
        self.db.execute("INSERT INTO categories VALUES(99,'Gaji','income','custom',4567)")
        self.db.execute("INSERT INTO categories VALUES(100,'Pemasukan khusus','income','custom',4567)")
        migrate(self.db, 4, 5)
        migrate(self.db, 5, 6)
        original = self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall()
        seed(self.db)
        self.assertEqual(original, self.db.execute("SELECT * FROM categories WHERE category_id IN (99,100) ORDER BY category_id").fetchall())
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM categories WHERE category_name='Gaji' AND transaction_type='income'").fetchone()[0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
