package com.fadhil.financereceipt.navigation

data class BottomNavigationItem(
    val destination: AppDestination,
    val label: String,
    val emoji: String
)

val bottomNavigationItems = listOf(
    BottomNavigationItem(
        destination = AppDestination.HOME,
        label = "Beranda",
        emoji = "🏠"
    ),
    BottomNavigationItem(
        destination = AppDestination.HISTORY,
        label = "Riwayat",
        emoji = "📋"
    ),
    BottomNavigationItem(
        destination = AppDestination.PLAN,
        label = "Plan",
        emoji = "🎯"
    )
)