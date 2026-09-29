package com.fadhil.financereceipt.ui.category

import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.IncomeGroup
import org.junit.Assert.*
import org.junit.Test

class IncomeCategorySectionTest {
    @Test fun sectionsKeepEachIncomeExactlyOnceAndExcludeExpenses() {
        val rows = IncomeGroup.values().mapIndexed { index, group ->
            CategoryEntity(categoryId = index + 1L, categoryName = group.label,
                transactionType = "income", emoji = "", incomeGroup = group.name)
        } + listOf(
            CategoryEntity(categoryId = 8, categoryName = "Kategori lama", transactionType = "income", emoji = ""),
            CategoryEntity(categoryId = 9, categoryName = "Makanan", transactionType = "expense", emoji = "",
                financialGroup = CategoryEntity.NEEDS)
        )
        val sections = IncomeCategorySection.fromCategories(rows)
        assertEquals(listOf("EMPLOYMENT", "BUSINESS", "INVESTMENT", "OTHER"), sections.map { it.key })
        assertEquals(setOf(1L, 2L, 3L, 4L), sections.flatMap { it.categories }.map { it.categoryId }.toSet())
        assertEquals(4, sections.sumOf { it.categories.size })
        assertTrue(IncomeCategorySection.fromCategories(emptyList()).isEmpty())
    }

    @Test fun invalidGroupsAndMixedTypesAreRejectedButLegacyNullRemainsValid() {
        rejected { CategoryEntity(categoryName = "X", transactionType = "income", emoji = "", incomeGroup = "INVALID") }
        rejected { CategoryEntity(categoryName = "X", transactionType = "expense", emoji = "", incomeGroup = "EMPLOYMENT") }
        rejected { CategoryEntity(categoryName = "X", transactionType = "income", emoji = "", financialGroup = "NEEDS") }
        rejected { CategoryEntity(categoryName = "X", transactionType = "expense", emoji = "", financialGroup = "WANTS", recommendedPercentage = 50) }
        assertNull(CategoryEntity(categoryName = "Lama", transactionType = "income", emoji = "").incomeGroup)
    }

    private fun rejected(block: () -> Unit) {
        try { block(); fail("Nilai seharusnya ditolak") }
        catch (_: IllegalArgumentException) { /* expected */ }
    }
}
