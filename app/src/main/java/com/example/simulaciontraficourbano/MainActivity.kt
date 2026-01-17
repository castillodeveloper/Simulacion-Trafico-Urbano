package com.example.simulaciontraficourbano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.simulaciontraficourbano.ui.*
import com.example.simulaciontraficourbano.ui.theme.SimuladorPSPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimuladorPSPTheme {
                TrafficSimulationApp()
            }
        }
    }
}

// Definición de rutas
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Simulation : Screen("simulation", "Simulación", Icons.Default.Place)
    object Config : Screen("config", "Configuración", Icons.Default.Settings)
    object Stats : Screen("stats", "Estadísticas", Icons.Default.Star)
    object History : Screen("history", "Historial", Icons.Default.List)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficSimulationApp() {
    val navController = rememberNavController()
    val viewModel: SimulationViewModel = viewModel()

    // --- CARGAR PREFERENCIAS AL INICIO ---
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    LaunchedEffect(Unit) {
        val savedConfig = prefs.loadConfig()
        viewModel.loadSavedConfig(savedConfig)
    }
    // -------------------------------------

    val screens = listOf(
        Screen.Simulation,
        Screen.Config,
        Screen.Stats,
        Screen.History
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Simulador de Tráfico Urbano",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Simulation.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Simulation.route) {
                SimulationScreen(viewModel = viewModel)
            }

            composable(Screen.Config.route) {
                ConfigScreen(viewModel = viewModel)
            }

            composable(Screen.Stats.route) {
                StatsScreen(viewModel = viewModel)
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = viewModel)
            }
        }
    }
}