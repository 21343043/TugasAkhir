package com.fadhil.financereceipt.ui.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpenseCategoryPickerTest {
    @get:Rule val compose = createComposeRule()
    private val rows = listOf(
        CategoryEntity(1, "Makanan", "expense", "", financialGroup = "NEEDS"),
        CategoryEntity(2, "Perawatan Diri", "expense", "", financialGroup = "WANTS"),
        CategoryEntity(3, "Hutang", "expense", "", financialGroup = "SAVINGS"),
        CategoryEntity(4, "Kategori lama", "expense", "")
    )

    @Test fun changingParentClearsChildAndFiltersChoices() {
        var selected by mutableStateOf<Long?>(1L)
        compose.setContent { MaterialTheme {
            ExpenseCategoryPicker(ExpenseCategorySection.fromCategories(rows), selected, { selected = it })
        } }
        compose.onNodeWithTag("expense_group_picker").assertTextContains("Needs (50%)")
        compose.onNodeWithTag("expense_group_picker").performClick()
        compose.onNodeWithTag("expense_group_WANTS").performClick()
        compose.runOnIdle { assertNull(selected) }
        compose.onNodeWithTag("expense_subcategory_1").assertDoesNotExist()
        compose.onNodeWithTag("expense_subcategory_2").performClick()
        compose.runOnIdle { assertEquals(2L, selected) }
    }

    @Test fun oldDebtStaysSelectedWhenEditingAndDisabledWhileSaving() {
        compose.setContent { MaterialTheme {
            ExpenseCategoryPicker(ExpenseCategorySection.fromCategories(rows, 3), 3L, {}, enabled = false)
        } }
        compose.onNodeWithTag("expense_group_picker").assertTextContains("Savings (20%)").assertIsNotEnabled()
        compose.onNodeWithTag("expense_subcategory_picker").assertTextContains("Hutang", substring = true).assertIsNotEnabled()
    }

    @Test fun lateLoadingRequiresValidGroupForOldUnclassifiedSelection() {
        var data by mutableStateOf(emptyList<CategoryEntity>())
        compose.setContent { MaterialTheme {
            ExpenseCategoryPicker(ExpenseCategorySection.fromCategories(data), 4L, {})
        } }
        compose.runOnIdle { data = rows }
        compose.onNodeWithTag("expense_group_picker").assertTextContains("Pilih kelompok pengeluaran")
        compose.onNodeWithTag("expense_subcategory_picker").assertIsNotEnabled()
        compose.onNodeWithText("Belum dikelompokkan").assertDoesNotExist()
    }
}
