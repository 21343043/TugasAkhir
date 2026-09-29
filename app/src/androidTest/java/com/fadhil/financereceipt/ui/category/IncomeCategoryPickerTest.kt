package com.fadhil.financereceipt.ui.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class IncomeCategoryPickerTest {
    @get:Rule val compose = createComposeRule()
    private val sections = IncomeCategorySection.fromCategories(listOf(
        CategoryEntity(1, "Gaji", "income", "", incomeGroup = "EMPLOYMENT"),
        CategoryEntity(2, "Freelance", "income", "", incomeGroup = "BUSINESS"),
        CategoryEntity(3, "Kategori lama", "income", "")
    ))

    @Test fun changingParentClearsChildAndLimitsChoices() {
        var selected by mutableStateOf<Long?>(1L)
        compose.setContent { MaterialTheme { IncomeCategoryPicker(sections, selected, { selected = it }) } }
        compose.onNodeWithTag("income_group_picker").assertTextContains("Employment Income")
        compose.onNodeWithTag("income_group_picker").performClick()
        compose.onNodeWithTag("income_group_BUSINESS").performClick()
        compose.runOnIdle { assertNull(selected) }
        compose.onNodeWithTag("income_subcategory_1").assertDoesNotExist()
        compose.onNodeWithTag("income_subcategory_2").performClick()
        compose.runOnIdle { assertEquals(2L, selected) }
    }

    @Test fun legacyWithoutGroupIsNotOffered() {
        compose.setContent { MaterialTheme { IncomeCategoryPicker(sections, 3L, {}) } }
        compose.onNodeWithTag("income_group_picker").assertTextContains("Pilih kelompok pemasukan")
        compose.onNodeWithTag("income_subcategory_picker").assertIsNotEnabled()
        compose.onNodeWithTag("income_group_picker").performClick()
        compose.onNodeWithText("Belum dikelompokkan").assertDoesNotExist()
        compose.onNodeWithTag("income_group_dialog_cancel").performClick()
    }

    @Test fun loadingAndSavingDisableSelection() {
        compose.setContent { MaterialTheme { IncomeCategoryPicker(sections, 1L, {}, enabled = false) } }
        compose.onNodeWithTag("income_group_picker").assertIsNotEnabled()
        compose.onNodeWithTag("income_subcategory_picker").assertIsNotEnabled()
    }
}
