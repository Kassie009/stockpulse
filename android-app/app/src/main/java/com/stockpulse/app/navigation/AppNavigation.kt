package com.stockpulse.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stockpulse.app.presentation.dashboard.DashboardScreen
import com.stockpulse.app.presentation.market.MarketScreen
import com.stockpulse.app.presentation.portfolio.PortfolioScreen
import com.stockpulse.app.presentation.profile.ProfileScreen

/** Secciones de la navegacion inferior (seccion 7: navegacion inferior). */
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

@Composable
fun StockPulseApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestinations.bottomBarItems.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Volver siempre a la misma instancia de cada pestana
                                popUpTo(navController.graph.findStartDestination().id) {
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
            navController = navController,
            startDestination = AppDestinations.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.DASHBOARD.route) { DashboardScreen() }
            composable(AppDestinations.MARKET.route) { MarketScreen() }
            composable(AppDestinations.PORTFOLIO.route) { PortfolioScreen() }
            composable(AppDestinations.PROFILE.route) { ProfileScreen() }
        }
    }
}
