package com.fadhil.financereceipt.ui.transaction

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransactionTypeTabsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tabsHaveEqualSizeAndSwitchSelectionWithoutMoving() {
        var income by mutableStateOf(false)
        compose.setContent { MaterialTheme { TransactionTypeTabs(income, { income = it }) } }
        val expense = compose.onNodeWithTag("transaction_type_expense")
        val incoming = compose.onNodeWithTag("transaction_type_income")
        val beforeExpense = expense.fetchSemanticsNode().boundsInRoot
        val beforeIncome = incoming.fetchSemanticsNode().boundsInRoot
        assertEquals(beforeExpense.width, beforeIncome.width, 1f)
        assertEquals(beforeExpense.height, beforeIncome.height, 1f)
        assertEquals(beforeExpense.top, beforeIncome.top, 1f)
        expense.assertIsSelected()
        incoming.performClick().assertIsSelected()
        expense.assertIsNotSelected()
        assertEquals(beforeExpense, expense.fetchSemanticsNode().boundsInRoot)
        assertEquals(beforeIncome, incoming.fetchSemanticsNode().boundsInRoot)
        expense.performClick().assertIsSelected()
    }

    @Test fun bothTabsAreDisabledWhileSaving() {
        compose.setContent { MaterialTheme { TransactionTypeTabs(false, {}, enabled = false) } }
        compose.onNodeWithTag("transaction_type_expense").assertIsNotEnabled()
        compose.onNodeWithTag("transaction_type_income").assertIsNotEnabled()
    }
}
