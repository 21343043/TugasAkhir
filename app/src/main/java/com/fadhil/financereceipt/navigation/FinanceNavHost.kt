package com.fadhil.financereceipt.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fadhil.financereceipt.ui.history.HistoryScreen
import com.fadhil.financereceipt.ui.home.HomeScreen
import com.fadhil.financereceipt.ui.plan.PlanScreen
import com.fadhil.financereceipt.ui.transaction.AddTransactionScreen

private const val OPEN_CURRENT_MONTH = "open_current_month"

private fun NavHostController.openMainTab(destination: AppDestination) {
    if (currentDestination?.route == destination.route) return
    if (destination == AppDestination.HOME) {
        // Home selalu tetap menjadi akar. Jangan restore tumpukan tab lain melalui Home.
        popBackStack(AppDestination.HOME.route, inclusive = false, saveState = true)
        return
    }
    navigate(destination.route) {
        popUpTo(AppDestination.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun FinanceNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != AppDestination.ADD_TRANSACTION.route) {
                NavigationBar {
                    bottomNavigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.destination.route,
                            onClick = { navController.openMainTab(item.destination) },
                            icon = { Text(item.emoji) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.HOME.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(AppDestination.HOME.route) {
                HomeScreen(
                    onAddTransaction = {
                        navController.navigate(AppDestination.ADD_TRANSACTION.route) { launchSingleTop = true }
                    },
                    onOpenPlan = {
                        navController.openMainTab(AppDestination.PLAN)
                        navController.getBackStackEntry(AppDestination.PLAN.route)
                            .savedStateHandle[OPEN_CURRENT_MONTH] = true
                    }
                )
            }
            composable(AppDestination.HISTORY.route) { HistoryScreen() }
            composable(AppDestination.ADD_TRANSACTION.route) {
                AddTransactionScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.PLAN.route) { entry ->
                val openCurrentMonth by entry.savedStateHandle
                    .getStateFlow(OPEN_CURRENT_MONTH, false)
                    .collectAsStateWithLifecycle()
                PlanScreen(
                    openCurrentMonth = openCurrentMonth,
                    onCurrentMonthOpened = { entry.savedStateHandle[OPEN_CURRENT_MONTH] = false }
                )
            }
        }
    }
}
