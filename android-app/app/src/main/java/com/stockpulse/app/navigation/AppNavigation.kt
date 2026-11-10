package com.stockpulse.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stockpulse.app.presentation.auth.LoginScreen
import com.stockpulse.app.presentation.auth.RegisterScreen
import com.stockpulse.app.presentation.auth.SessionViewModel
import com.stockpulse.app.presentation.auth.WelcomeScreen
import com.stockpulse.app.presentation.dashboard.DashboardScreen
import com.stockpulse.app.presentation.alerts.AlertsScreen
import com.stockpulse.app.presentation.devices.DevicesScreen
import com.stockpulse.app.presentation.payments.PaymentsScreen
import com.stockpulse.app.presentation.market.MarketScreen
import com.stockpulse.app.presentation.portfolio.PortfolioScreen
import com.stockpulse.app.presentation.profile.ProfileScreen

/**
 * Secciones de la navegacion inferior (seccion 7 del prompt).
 */
data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

object AppDestinations {
    val DASHBOARD = Destination("dashboard", "Inicio", Icons.Filled.Home)
    val MARKET = Destination("market", "Mercado", Icons.Filled.ShowChart)
    val PORTFOLIO = Destination("portfolio", "Portafolio", Icons.Filled.PieChart)
    val PROFILE = Destination("profile", "Perfil", Icons.Filled.Person)

    val bottomBarItems = listOf(DASHBOARD, MARKET, PORTFOLIO, PROFILE)
}

object Routes {
    const val WELCOME = "welcome"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val ALERTS = "alerts"
    const val PAYMENTS = "payments"
    const val DEVICES = "devices"
}

@Composable
fun StockPulseApp() {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.session.collectAsState()
    val sessionReady by sessionViewModel.sessionReady.collectAsState()

    // Puerta de entrada REAL: sesion persistida -> entra directo a la app
    if (!sessionReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (session != null) Routes.MAIN else Routes.WELCOME
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreateAccount = { navController.navigate(Routes.REGISTER) },
                onLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onAuthed = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onAuthed = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            val sessionViewModel: SessionViewModel = hiltViewModel()
            MainScaffold(
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAlerts = {
                    navController.navigate(Routes.ALERTS)
                },
                onNavigateToPayments = {
                    navController.navigate(Routes.PAYMENTS)
                },
                onNavigateToDevices = {
                    navController.navigate(Routes.DEVICES)
                }
            )
        }
        composable(Routes.ALERTS) {
            AlertsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PAYMENTS) {
            PaymentsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DEVICES) {
            DevicesScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Contenedor de las 4 pestanas con barra inferior. */
@Composable
private fun MainScaffold(
    onLogout: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToDevices: () -> Unit
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestinations.bottomBarItems.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            tabNavController.navigate(destination.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = AppDestinations.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.DASHBOARD.route) { DashboardScreen() }
            composable(AppDestinations.MARKET.route) { MarketScreen() }
            composable(AppDestinations.PORTFOLIO.route) { PortfolioScreen() }
            composable(AppDestinations.PROFILE.route) {
                ProfileScreen(
                    onLogout = onLogout,
                    onOpenAlerts = onNavigateToAlerts,
                    onOpenPayments = onNavigateToPayments,
                    onOpenDevices = onNavigateToDevices
                )
            }
        }
    }
}
