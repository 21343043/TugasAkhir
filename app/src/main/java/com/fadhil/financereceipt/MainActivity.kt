package com.fadhil.financereceipt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fadhil.financereceipt.navigation.FinanceNavHost
import com.fadhil.financereceipt.ui.theme.TugasAkhirTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TugasAkhirTheme {
                FinanceNavHost()
            }
        }
    }
}