"""Exercise actual MIGRATION_7_8 SQL against SQLite with foreign keys enabled.
Uses exported v6 schema: v7 changed only data, not tables/columns.
Seed uses the existing Python simulation, not the Android Room runtime.
"""
import unittest
from test_expense_migration import database6
from test_income_migration import migrate, seed


class CategoryRefinementTest(unittest.TestCase):
    def setUp(self):
        self.db = database6()
        self.addCleanup(self.db.close)

    def category(self, id, name, group='WANTS', typ='expense'):
        if typ == 'income':
            group = None
        self.db.execute("INSERT INTO categories VALUES(?,?,?,'X',1234,?,?,?)",
            (id, name, typ, group, {'NEEDS':50,'WANTS':30,'SAVINGS':20}.get(group),
             'OTHER' if typ == 'income' else None))

    def transaction(self, id, category):
        self.db.execute("INSERT INTO transactions VALUES(?,?,'expense',25000,1234,'lama','scan_struk',5678)", (id, category))

    def upgrade(self):
        self.db.commit()
        self.db.execute('BEGIN')
        try:
            migrate(self.db, 7, 8)
            self.db.commit()
        except Exception:
            self.db.rollback()
            raise
        self.assertEqual([], self.db.execute('PRAGMA foreign_key_check').fetchall())
        self.assertEqual([], self.db.execute('SELECT name FROM sqlite_temp_master').fetchall())

    def rows(self, table):
        return self.db.execute(f'SELECT * FROM {table} ORDER BY 1').fetchall()

    def test_merge_both_pairs_preserves_history_receipts_totals_and_plans(self):
        for id,name in [(1,'Shopping'),(2,'Belanja'),(3,'Lifestyle'),(4,'Perawatan Diri')]:
            self.category(id,name)
            self.transaction(id,id)
            self.db.execute("INSERT INTO financial_plans VALUES(?,?,2026,9,?,100,200)",(id,id,id*100000))
        self.db.execute("INSERT INTO financial_plans VALUES(5,1,2026,10,800000,100,300)")
        self.db.execute("INSERT INTO receipt_extractions VALUES(1,1,'Toko',1234,25000,'OCR','normalized','a.jpg','Toko',1234,25000,'v1',5678)")
        transactions=self.rows('transactions'); receipts=self.rows('receipt_extractions')
        before=self.db.execute('SELECT c.financial_group,SUM(t.amount) FROM transactions t JOIN categories c USING(category_id) GROUP BY 1').fetchall()
        self.upgrade()
        self.assertEqual([(2,'Belanja','WANTS',30),(4,'Perawatan Diri','WANTS',30)], self.db.execute('SELECT category_id,category_name,financial_group,recommended_percentage FROM categories ORDER BY 1').fetchall())
        self.assertEqual([r[:1]+(({1:2,2:2,3:4,4:4}[r[1]]),)+r[2:] for r in transactions],self.rows('transactions'))
        self.assertEqual(receipts,self.rows('receipt_extractions'))
        self.assertEqual([(2,9,300000),(2,10,800000),(4,9,700000)],self.db.execute('SELECT category_id,plan_month,budget_amount FROM financial_plans ORDER BY 1,2').fetchall())
        self.assertEqual(before,self.db.execute('SELECT c.financial_group,SUM(t.amount) FROM transactions t JOIN categories c USING(category_id) GROUP BY 1').fetchall())

    def test_only_old_names_keep_ids(self):
        self.category(41,'Shopping');self.category(51,'Lifestyle')
        self.upgrade()
        self.assertEqual([(41,'Belanja'),(51,'Perawatan Diri')],self.db.execute('SELECT category_id,category_name FROM categories ORDER BY 1').fetchall())

    def test_case_space_variants_merge_and_income_is_untouched(self):
        for id,name in [(1,'shopping'),(2,' Belanja '),(3,'Belanja'),(4,'PERAWATAN DIRI'),(5,' lifestyle ')]:
            self.category(id,name)
        self.category(6,'Shopping',typ='income');self.category(7,'Belanja Online')
        income=self.db.execute('SELECT * FROM categories WHERE category_id=6').fetchone()
        self.upgrade()
        self.assertEqual([(3,'Belanja'),(4,'Perawatan Diri'),(6,'Shopping'),(7,'Belanja Online')],self.db.execute('SELECT category_id,category_name FROM categories ORDER BY 1').fetchall())
        self.assertEqual(income,self.db.execute('SELECT * FROM categories WHERE category_id=6').fetchone())

    def test_placeholder_deleted_only_when_unreferenced(self):
        self.category(1,'Belum dikelompokkan',None)
        self.category(2,' BELUM DIKELOMPOKKAN ',None)
        self.transaction(1,2)
        self.category(3,'belum dikelompokkan',None)
        self.db.execute('INSERT INTO financial_plans VALUES(1,3,2026,9,100,1,1)')
        self.upgrade()
        self.assertIsNone(self.db.execute('SELECT * FROM categories WHERE category_id=1').fetchone())
        self.assertEqual([2,3],[r[0] for r in self.rows('categories')])
        self.assertEqual(1,len(self.rows('transactions')))
        self.assertEqual(1,len(self.rows('financial_plans')))

    def test_bill_alias_left_in_version7_is_merged(self):
        self.category(1,'Tagihan Pokok','NEEDS');self.category(2,'Tagihan','NEEDS');self.transaction(1,1)
        self.upgrade()
        self.assertEqual([(2,'Tagihan')],self.db.execute('SELECT category_id,category_name FROM categories').fetchall())
        self.assertEqual(2,self.rows('transactions')[0][1])

    def test_empty_and_fresh_seed_idempotence(self):
        self.upgrade();seed(self.db)
        before=self.rows('categories');seed(self.db)
        self.assertEqual(before,self.rows('categories'))
        groups={group:[r[1] for r in before if r[5]==group] for group in ['NEEDS','WANTS','SAVINGS']}
        self.assertEqual({'Makanan','Transportasi','Tempat Tinggal','Tagihan','Kesehatan','Pendidikan'},set(groups['NEEDS']))
        self.assertEqual({'Belanja','Hiburan','Hobi','Traveling','Perawatan Diri'},set(groups['WANTS']))
        self.assertEqual({'Tabungan','Dana Darurat','Investasi'},set(groups['SAVINGS']))
        self.assertEqual(31,len(before))

    def test_known_null_metadata_backfilled_unknown_not_guessed(self):
        for id,name in [(1,'Makanan'),(2,'Tabungan'),(3,'Hiburan'),(4,'Kategori pribadi')]:
            self.category(id,name,None)
        self.upgrade()
        self.assertEqual([(1,'NEEDS',50),(2,'SAVINGS',20),(3,'WANTS',30),(4,None,None)],self.db.execute('SELECT category_id,financial_group,recommended_percentage FROM categories ORDER BY 1').fetchall())

    def test_overflow_in_second_merge_rolls_back_first_merge_too(self):
        for id,name in [(1,'Shopping'),(2,'Lifestyle'),(3,'Perawatan Diri')]:self.category(id,name)
        self.db.execute('INSERT INTO financial_plans VALUES(1,2,2026,9,9223372036854775807,1,1)')
        self.db.execute('INSERT INTO financial_plans VALUES(2,3,2026,9,1,1,1)')
        before=self.rows('categories');plans=self.rows('financial_plans')
        with self.assertRaisesRegex(Exception,'overflow'): self.upgrade()
        self.assertEqual(before,self.rows('categories'));self.assertEqual(plans,self.rows('financial_plans'))

    def test_full_upgrade_from_v6_and_repeat_is_safe(self):
        self.category(1,'Tagihan Pokok','NEEDS');self.category(2,'Shopping');self.category(3,'Lifestyle')
        migrate(self.db,6,7);self.upgrade();seed(self.db)
        before=self.rows('categories');self.upgrade();seed(self.db)
        self.assertEqual(before,self.rows('categories'))
        self.assertFalse(any(r[1] in ['Shopping','Lifestyle','Tagihan Pokok','Belum dikelompokkan'] for r in before))

    def test_dao_choices_exclude_null_and_literal_placeholder(self):
        import re
        from test_income_migration import SRC
        source=(SRC/'data/local/dao/CategoryDao.kt').read_text()
        query=next(q for q in re.findall(r'"""(.*?)"""',source,re.S) if 'transaction_type = :type' in q)
        self.category(1,'Belanja');self.category(2,'Kategori khusus',None)
        self.category(3,'Belum dikelompokkan','NEEDS');self.category(4,'Salary',typ='income')
        self.assertEqual([1],[r[0] for r in self.db.execute(query,{'type':'expense'})])
        self.assertEqual([4],[r[0] for r in self.db.execute(query,{'type':'income'})])

if __name__=='__main__': unittest.main(verbosity=2)
