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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fadhil.financereceipt.ui.history.HistoryScreen
import com.fadhil.financereceipt.ui.home.HomeScreen
import com.fadhil.financereceipt.ui.plan.PlanScreen

@Composable
fun FinanceNavHost() {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                bottomNavigationItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(
                                    navController.graph
                                        .findStartDestination().id
                                ) {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Text(text = item.emoji)
                        },
                        label = {
                            Text(text = item.label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(AppDestination.HOME.route) {
                HomeScreen()
            }

            composable(AppDestination.HISTORY.route) {
                HistoryScreen()
            }

            composable(AppDestination.PLAN.route) {
                PlanScreen()
            }
        }
    }
}