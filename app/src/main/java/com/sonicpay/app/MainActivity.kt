package com.sonicpay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sonicpay.app.ui.components.AnimatedBackdrop
import com.sonicpay.app.ui.screens.CustomerScreen
import com.sonicpay.app.ui.screens.HomeScreen
import com.sonicpay.app.ui.screens.MerchantScreen
import com.sonicpay.app.ui.theme.SonicPayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonicPayTheme {
                SonicPayApp()
            }
        }
    }
}

@Composable
fun SonicPayApp() {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackdrop(modifier = Modifier.fillMaxSize())

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onMerchantSelected = { navController.navigate("merchant") },
                    onCustomerSelected = { navController.navigate("customer") }
                )
            }
            composable("merchant") {
                MerchantScreen(onBack = { navController.popBackStack() })
            }
            composable("customer") {
                CustomerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
