package com.fadhil.financereceipt.ui.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CategorySelectionDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longListScrollsWhileCancelRemainsVisible() {
        var selected: String? = null
        var visible by mutableStateOf(true)
        compose.setContent { MaterialTheme {
            if (visible) CategorySelectionDialog(
                options = (1..60).map { CategorySelectionOption("$it", "Kategori $it", "📁", "option_$it") },
                selectedKey = null, onSelect = { selected = it }, onDismiss = { visible = false }
            )
        } }
        compose.onNodeWithText("Pilih kategori").assertIsDisplayed()
        compose.onNodeWithTag("category_selection_dialog_list").performScrollToIndex(59)
        compose.onNodeWithTag("option_60").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("60", selected) }
        compose.onNodeWithTag("category_selection_dialog_cancel").assertIsDisplayed().performClick()
        compose.onNodeWithTag("category_selection_dialog").assertDoesNotExist()
    }

    @Test fun cancelChildKeepsNewGroupButRequiresNewSubcategory() {
        val rows = listOf(
            CategoryEntity(1, "Makanan", "expense", "🍔", financialGroup = "NEEDS"),
            CategoryEntity(2, "Belanja", "expense", "🛍️", financialGroup = "WANTS"))
        var selected by mutableStateOf<Long?>(1L)
        compose.setContent { MaterialTheme {
            ExpenseCategoryPicker(ExpenseCategorySection.fromCategories(rows), selected, { selected = it })
        } }
        compose.onNodeWithTag("expense_group_picker").performClick()
        compose.onNodeWithTag("expense_group_WANTS").performClick()
        compose.onNodeWithTag("expense_subcategory_dialog_cancel").performClick()
        compose.runOnIdle { assertNull(selected) }
        compose.onNodeWithTag("expense_group_picker").assertTextContains("Wants (30%)")
        compose.onNodeWithTag("expense_subcategory_picker").assertTextContains("Pilih subkategori pengeluaran").performClick()
        compose.onNodeWithTag("expense_subcategory_2").performClick()
        compose.runOnIdle { assertEquals(2L, selected) }
    }

    @Test fun dismissGroupDoesNotClearExistingCategory() {
        val rows = listOf(CategoryEntity(1, "Gaji", "income", "💼", incomeGroup = "EMPLOYMENT"))
        var selected by mutableStateOf<Long?>(1L)
        compose.setContent { MaterialTheme {
            IncomeCategoryPicker(IncomeCategorySection.fromCategories(rows), selected, { selected = it })
        } }
        compose.onNodeWithTag("income_group_picker").performClick()
        compose.onNodeWithTag("income_group_dialog_cancel").performClick()
        compose.runOnIdle { assertEquals(1L, selected) }
        compose.onNodeWithTag("income_subcategory_picker").assertTextContains("Gaji", substring = true)
    }

    @Test fun bothKindsUseSameDialogBounds() {
        var income by mutableStateOf(false)
        compose.setContent { MaterialTheme {
            if (income) IncomeCategoryPicker(IncomeCategorySection.fromCategories(listOf(
                CategoryEntity(1, "Gaji", "income", "💼", incomeGroup = "EMPLOYMENT"))), null, {})
            else ExpenseCategoryPicker(ExpenseCategorySection.fromCategories(listOf(
                CategoryEntity(2, "Makanan", "expense", "🍔", financialGroup = "NEEDS"))), null, {})
        } }
        compose.onNodeWithTag("expense_group_picker").performClick()
        val expenseBounds = compose.onNodeWithTag("expense_group_dialog_list").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("expense_group_dialog_cancel").performClick()
        compose.runOnIdle { income = true }
        compose.onNodeWithTag("income_group_picker").performClick()
        val incomeBounds = compose.onNodeWithTag("income_group_dialog_list").fetchSemanticsNode().boundsInRoot
        assertEquals(expenseBounds.width, incomeBounds.width, 1f)
        assertEquals(expenseBounds.height, incomeBounds.height, 1f)
    }
}
