package com.sonicpay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sonicpay.app.data.CrashReporter
import com.sonicpay.app.data.SessionPrefs
import com.sonicpay.app.ui.components.AnimatedBackdrop
import com.sonicpay.app.ui.screens.CustomerScreen
import com.sonicpay.app.ui.screens.HistoryScreen
import com.sonicpay.app.ui.screens.MerchantScreen
import com.sonicpay.app.ui.screens.SettingsScreen
import com.sonicpay.app.ui.screens.WelcomeScreen
import com.sonicpay.app.ui.theme.SonicPayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)
        enableEdgeToEdge()
        SessionPrefs.init(this)
        setContent {
            SonicPayTheme {
                SonicPayApp(startRoute = startRouteFor(SessionPrefs.savedRole))
            }
        }
    }
}

private fun startRouteFor(role: SessionPrefs.Role?): String = when (role) {
    SessionPrefs.Role.Merchant -> "merchant"
    SessionPrefs.Role.Customer -> "customer"
    null -> "welcome"
}

@Composable
fun SonicPayApp(startRoute: String) {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackdrop(modifier = Modifier.fillMaxSize())

        NavHost(
            navController = navController,
            startDestination = startRoute,
            enterTransition = {
                fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 24 }
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = {
                fadeOut(tween(220)) + slideOutVertically(tween(280)) { it / 24 }
            }
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onRoleChosen = { role ->
                        SessionPrefs.chooseRole(role)
                        navController.navigate(
                            if (role is SessionPrefs.Role.Merchant) "merchant" else "customer"
                        ) { popUpTo("welcome") { inclusive = true } }
                    }
                )
            }
            composable("merchant") {
                MerchantScreen(onSettings = { navController.navigate("settings") })
            }
            composable("customer") {
                CustomerScreen(
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenHistory = { navController.navigate("history") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onRoleSwitched = {
                        val target = if (SessionPrefs.savedRole is SessionPrefs.Role.Merchant) {
                            "merchant"
                        } else {
                            "customer"
                        }
                        navController.navigate(target) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable("history") {
                HistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
