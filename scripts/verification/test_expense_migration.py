"""Execute actual MIGRATION_6_7 SQL with SQLite, foreign keys enabled.
Run from project root: python3 scripts/verification/test_expense_migration.py
Room/Compose tests still require Android. Seed helper models repository behavior.
"""
import json
import sqlite3
import unittest
from test_income_migration import ROOT, migrate, seed


def database6():
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys=ON")
    schema = json.loads((ROOT / "app/schemas/com.fadhil.financereceipt.data.local.database.FinanceDatabase/6.json").read_text())
    for entity in schema["database"]["entities"]:
        db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for index in entity["indices"]:
            db.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
    return db


class ExpenseMigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = database6()
        self.addCleanup(self.db.close)

    def category(self, id, name, group="NEEDS", typ="expense"):
        self.db.execute("""INSERT INTO categories
            (category_id,category_name,transaction_type,emoji,created_at,financial_group,recommended_percentage,income_group)
            VALUES(?,?,?,'X',1234,?,?,NULL)""",
            (id, name, typ, group, {"NEEDS": 50, "WANTS": 30, "SAVINGS": 20}.get(group)))

    def upgrade(self):
        self.db.commit()
        self.db.execute("BEGIN")
        try:
            migrate(self.db, 6, 7)
            self.db.commit()
        except Exception:
            self.db.rollback()
            raise
        self.assertEqual([], self.db.execute("PRAGMA foreign_key_check").fetchall())
        self.assertEqual([], self.db.execute("SELECT name FROM sqlite_temp_master WHERE name LIKE '_bill_%'").fetchall())

    def test_no_bills_is_safe(self):
        self.category(1, "Makanan")
        before = self.db.execute("SELECT * FROM categories").fetchall()
        self.upgrade()
        self.assertEqual(before, self.db.execute("SELECT * FROM categories").fetchall())

    def test_only_tagihan_pokok_keeps_its_id(self):
        self.category(21, "Tagihan Pokok")
        self.upgrade()
        self.assertEqual([(21, "Tagihan", "NEEDS", 50)], self.db.execute(
            "SELECT category_id,category_name,financial_group,recommended_percentage FROM categories").fetchall())

    def test_only_tagihan_is_unchanged_and_repeat_is_safe(self):
        self.category(21, "Tagihan")
        before = self.db.execute("SELECT * FROM categories").fetchall()
        self.upgrade()
        self.upgrade()
        self.assertEqual(before, self.db.execute("SELECT * FROM categories").fetchall())

    def test_merge_preserves_receipts_transactions_group_totals_and_monthly_budgets(self):
        db = self.db
        self.category(10, "Tagihan Pokok")
        self.category(20, "Tagihan")
        self.category(30, "Hutang", "SAVINGS")
        db.execute("INSERT INTO transactions VALUES(1,10,'expense',100000,1234,'lama','scan_struk',4567)")
        db.execute("INSERT INTO transactions VALUES(2,20,'expense',200000,4321,'manual','manual',7654)")
        db.execute("INSERT INTO transactions VALUES(3,30,'expense',50000,4321,'hutang','manual',7654)")
        db.execute("""INSERT INTO receipt_extractions VALUES(1,1,'Toko Lama',1234,100000,'OCR asli',
            'normal','receipts/a.jpg','Toko Lama',1234,100000,'v1',4567)""")
        db.execute("INSERT INTO financial_plans VALUES(1,10,2026,9,300000,100,200)")
        db.execute("INSERT INTO financial_plans VALUES(2,20,2026,9,400000,90,300)")
        db.execute("INSERT INTO financial_plans VALUES(3,10,2026,10,500000,100,200)")
        db.execute("INSERT INTO financial_plans VALUES(4,20,2027,9,600000,100,200)")
        receipt = db.execute("SELECT * FROM receipt_extractions").fetchall()
        tx = db.execute("SELECT * FROM transactions ORDER BY transaction_id").fetchall()
        group_sql = "SELECT financial_group,SUM(amount) FROM transactions JOIN categories USING(category_id) GROUP BY financial_group"
        totals = db.execute(group_sql).fetchall()
        budgets = db.execute("SELECT plan_year,plan_month,SUM(budget_amount) FROM financial_plans GROUP BY plan_year,plan_month").fetchall()
        self.upgrade()
        self.assertEqual([(20, "Tagihan"), (30, "Hutang")], db.execute("SELECT category_id,category_name FROM categories ORDER BY category_id").fetchall())
        self.assertEqual(receipt, db.execute("SELECT * FROM receipt_extractions").fetchall())
        after = db.execute("SELECT * FROM transactions ORDER BY transaction_id").fetchall()
        self.assertEqual([(r[0], 20 if r[1] in (10,20) else r[1], *r[2:]) for r in tx], after)
        self.assertEqual(totals, db.execute(group_sql).fetchall())
        self.assertEqual(budgets, db.execute("SELECT plan_year,plan_month,SUM(budget_amount) FROM financial_plans GROUP BY plan_year,plan_month").fetchall())
        self.assertEqual((1,20,2026,9,700000,90,300), db.execute("SELECT * FROM financial_plans WHERE plan_id=1").fetchone())
        self.assertEqual(3, db.execute("SELECT COUNT(*) FROM financial_plans").fetchone()[0])

    def test_case_variants_merge_but_income_and_other_categories_stay(self):
        self.category(1, " TAGIHAN POKOK ")
        self.category(2, "tagihan")
        self.category(3, "Tagihan")
        self.category(4, "Tagihan", None, "income")
        self.category(5, "Tagihan Internet", None)
        self.upgrade()
        self.assertEqual([(3,"Tagihan"),(4,"Tagihan"),(5,"Tagihan Internet")], self.db.execute(
            "SELECT category_id,category_name FROM categories ORDER BY category_id").fetchall())

    def test_overflow_rolls_back_without_data_loss(self):
        self.category(1, "Tagihan")
        self.category(2, "Tagihan Pokok")
        self.db.execute("INSERT INTO financial_plans VALUES(1,1,2026,9,?,1,1)", (2**63-1,))
        self.db.execute("INSERT INTO financial_plans VALUES(2,2,2026,9,1,1,1)")
        before = {t:self.db.execute(f"SELECT * FROM {t}").fetchall() for t in ("categories","financial_plans")}
        with self.assertRaisesRegex(sqlite3.OperationalError, "overflow"):
            self.upgrade()
        for table, rows in before.items():
            self.assertEqual(rows, self.db.execute(f"SELECT * FROM {table}").fetchall())

    def test_new_defaults_and_seed_idempotence(self):
        self.upgrade()
        seed(self.db)
        before = self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall()
        seed(self.db)
        self.assertEqual(before, self.db.execute("SELECT * FROM categories ORDER BY category_id").fetchall())
        groups = dict(self.db.execute("SELECT financial_group,COUNT(*) FROM categories WHERE transaction_type='expense' GROUP BY financial_group"))
        self.assertEqual({"NEEDS":6,"WANTS":5,"SAVINGS":3}, groups)
        self.assertEqual(31, len(before))
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM categories WHERE category_name IN ('Hutang','Tagihan Pokok')").fetchone()[0])

    def test_seed_keeps_legacy_ids_and_capitalization(self):
        self.category(1, "Tempat tinggal")
        self.category(2, "Dana darurat", "SAVINGS")
        self.category(3, "Hutang", "SAVINGS")
        self.category(4, "Tagihan Pokok")
        self.upgrade()
        seed(self.db)
        self.assertEqual([(1,'Tempat tinggal'),(2,'Dana darurat'),(3,'Hutang'),(4,'Tagihan')], self.db.execute(
            "SELECT category_id,category_name FROM categories WHERE category_id<=4 ORDER BY category_id").fetchall())
        for name in ['tempat tinggal','dana darurat','tagihan']:
            self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM categories WHERE LOWER(category_name)=?",(name,)).fetchone()[0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
