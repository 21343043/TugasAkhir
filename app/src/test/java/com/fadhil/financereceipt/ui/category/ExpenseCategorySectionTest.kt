package com.fadhil.financereceipt.ui.category

import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import org.junit.Assert.*
import org.junit.Test

class ExpenseCategorySectionTest {
    private fun category(id: Long, name: String, group: String?) = CategoryEntity(
        categoryId = id, categoryName = name, transactionType = "expense", emoji = "", financialGroup = group
    )

    @Test fun groupsFollowStoredMetadataAndExcludeUnclassifiedChoices() {
        val rows = listOf(
            category(1, "Makanan", "NEEDS"), category(2, "Perawatan Diri", "WANTS"),
            category(3, "Tabungan", "SAVINGS"), category(4, "Kategori khusus", null),
            CategoryEntity(categoryId = 5, categoryName = "Gaji", transactionType = "income", emoji = "", incomeGroup = "EMPLOYMENT")
        )
        val sections = ExpenseCategorySection.fromCategories(rows)
        assertEquals(listOf("NEEDS", "WANTS", "SAVINGS"), sections.map { it.key })
        assertEquals(listOf("Needs (50%)", "Wants (30%)", "Savings (20%)"), sections.map { it.label })
        assertEquals(listOf(1L, 2L, 3L), sections.flatMap { it.categories }.map { it.categoryId })
        assertTrue(ExpenseCategorySection.fromCategories(emptyList()).isEmpty())
    }

    @Test fun oldDebtRemainsEditableButIsNotOfferedForNewManualTransactions() {
        val debt = category(8, "Hutang", "SAVINGS")
        assertTrue(ExpenseCategorySection.fromCategories(listOf(debt)).isEmpty())
        assertEquals(debt, ExpenseCategorySection.fromCategories(listOf(debt), 8).single().categories.single())
    }

    @Test fun displayNamesDoNotMutateStoredEntitiesOrReclassifyThem() {
        val home = category(3, "Tempat tinggal", "NEEDS")
        val reserve = category(4, "Dana darurat", "SAVINGS")
        assertEquals("Tempat Tinggal", ExpenseCategorySection.displayName(home))
        assertEquals("Dana Darurat", ExpenseCategorySection.displayName(reserve))
        assertEquals("Tempat tinggal", home.categoryName)
        val custom = category(5, "Makanan", "WANTS")
        assertEquals("WANTS", ExpenseCategorySection.fromCategories(listOf(custom)).single().key)
    }
}
