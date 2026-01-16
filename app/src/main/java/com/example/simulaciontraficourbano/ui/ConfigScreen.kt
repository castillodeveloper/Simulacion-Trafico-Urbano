package com.example.simulaciontraficourbano.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // <--- IMPORTANTE
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ConfigScreen(viewModel: SimulationViewModel) {
    val cfg by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración de Simulación",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Ajusta los parámetros de la simulación. Presiona 'Aplicar' para reiniciar con la nueva configuración.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ScenarioSection(viewModel)

        VehicleConfigSection(
            vehicleCount = cfg.vehicleCount,
            ambulanceCount = cfg.ambulanceCount,
            onVehicleCountChange = { viewModel.setVehicleCount(it) },
            onAmbulanceCountChange = { viewModel.setAmbulanceCount(it) }
        )

        SimulationConfigSection(
            simSpeed = cfg.simSpeed,
            onSimSpeedChange = { viewModel.setSpeed(it) }
        )

        TrafficLightConfigSection(
            lightsEnabled = cfg.lightsEnabled,
            greenSeconds = (cfg.lightGreenMs / 1000L).toInt(),
            onLightsEnabledChange = { viewModel.setLightsEnabled(it) },
            onGreenSecondsChange = { viewModel.setGreenSeconds(it) }
        )

        AdvancedConfigSection(
            collisionsEnabled = cfg.collisionsEnabled,
            autoEventsEnabled = cfg.autoEventsEnabled,
            eventEverySeconds = (cfg.eventEveryMs / 1000L).toInt(),
            onCollisionsEnabledChange = { viewModel.setCollisionsEnabled(it) },
            onAutoEventsEnabledChange = { viewModel.setAutoEventsEnabled(it) },
            onEventEverySecondsChange = { viewModel.setEventEverySeconds(it) }
        )

        // --- SECCIÓN MODIFICADA PARA GUARDAR DATOS ---
        val context = LocalContext.current
        Button(
            onClick = {
                viewModel.applyAndRestart()
                // GUARDAR PREFERENCIAS AL APLICAR
                PreferencesManager(context).saveConfig(cfg)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Aplicar y Reiniciar Simulación", style = MaterialTheme.typography.titleMedium)
        }
        // ---------------------------------------------

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ScenarioSection(viewModel: SimulationViewModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Escenarios Predefinidos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                "Carga configuraciones predefinidas para diferentes situaciones de tráfico",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioCard(
                    title = "🌤️ Tráfico Ligero",
                    description = "10 vehículos, 1 ambulancia, ritmo normal",
                    onClick = { viewModel.applyScenario(ScenarioPreset.LIGHT) }
                )

                ScenarioCard(
                    title = "🚦 Hora Punta",
                    description = "50 vehículos, 2 ambulancias, mucho tráfico",
                    onClick = { viewModel.applyScenario(ScenarioPreset.RUSH_HOUR) }
                )

                ScenarioCard(
                    title = "🚨 Emergencia",
                    description = "35 vehículos, 10 ambulancias con prioridad",
                    onClick = { viewModel.applyScenario(ScenarioPreset.EMERGENCY) }
                )

                ScenarioCard(
                    title = "⚠️ Caos Total",
                    description = "40 vehículos, sin semáforos, sin colisiones",
                    onClick = { viewModel.applyScenario(ScenarioPreset.CHAOS) }
                )
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VehicleConfigSection(
    vehicleCount: Int,
    ambulanceCount: Int,
    onVehicleCountChange: (Int) -> Unit,
    onAmbulanceCountChange: (Int) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                Text(
                    "Configuración de Vehículos",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            ConfigSlider(
                label = "Número de Vehículos",
                value = vehicleCount.toFloat(),
                valueRange = 5f..100f,
                steps = 94,
                onValueChange = { onVehicleCountChange(it.roundToInt()) },
                valueText = vehicleCount.toString(),
                icon = Icons.Default.DirectionsCar,
                description = "Total de vehículos en la simulación"
            )

            ConfigSlider(
                label = "Ambulancias",
                value = ambulanceCount.toFloat(),
                valueRange = 0f..vehicleCount.toFloat(),
                steps = vehicleCount.coerceAtLeast(1),
                onValueChange = { onAmbulanceCountChange(it.roundToInt()) },
                valueText = ambulanceCount.toString(),
                icon = Icons.Default.LocalHospital,
                description = "Vehículos de emergencia con prioridad en semáforos"
            )
        }
    }
}

@Composable
private fun SimulationConfigSection(
    simSpeed: Double,
    onSimSpeedChange: (Double) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null)
                Text(
                    "Velocidad de Simulación",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            ConfigSlider(
                label = "Multiplicador de Velocidad",
                value = simSpeed.toFloat(),
                valueRange = 0.5f..5f,
                steps = 44,
                onValueChange = { onSimSpeedChange(it.toDouble()) },
                valueText = "${"%.1f".format(simSpeed)}x",
                icon = Icons.Default.Speed,
                description = "Controla qué tan rápido avanza la simulación"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5, 1.0, 2.0, 5.0).forEach { speed ->
                    FilterChip(
                        selected = simSpeed == speed,
                        onClick = { onSimSpeedChange(speed) },
                        label = { Text("${speed}x") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficLightConfigSection(
    lightsEnabled: Boolean,
    greenSeconds: Int,
    onLightsEnabledChange: (Boolean) -> Unit,
    onGreenSecondsChange: (Int) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Traffic, contentDescription = null)
                Text(
                    "Configuración de Semáforos",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            ConfigSwitch(
                label = "Activar Semáforos",
                checked = lightsEnabled,
                onCheckedChange = onLightsEnabledChange,
                icon = Icons.Default.Traffic,
                description = "Habilita el sistema de semáforos en los cruces"
            )

            if (lightsEnabled) {
                HorizontalDivider()

                ConfigSlider(
                    label = "Duración Luz Verde",
                    value = greenSeconds.toFloat(),
                    valueRange = 3f..30f,
                    steps = 26,
                    onValueChange = { onGreenSecondsChange(it.roundToInt()) },
                    valueText = "${greenSeconds}s",
                    icon = Icons.Default.Timer,
                    description = "Tiempo que permanece la luz verde"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 8, 10, 15).forEach { seconds ->
                        FilterChip(
                            selected = greenSeconds == seconds,
                            onClick = { onGreenSecondsChange(seconds) },
                            label = { Text("${seconds}s") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedConfigSection(
    collisionsEnabled: Boolean,
    autoEventsEnabled: Boolean,
    eventEverySeconds: Int,
    onCollisionsEnabledChange: (Boolean) -> Unit,
    onAutoEventsEnabledChange: (Boolean) -> Unit,
    onEventEverySecondsChange: (Int) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text(
                    "Opciones Avanzadas",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            ConfigSwitch(
                label = "Detección de Colisiones",
                checked = collisionsEnabled,
                onCheckedChange = onCollisionsEnabledChange,
                icon = Icons.Default.Warning,
                description = "Los vehículos detectan y evitan colisiones"
            )

            HorizontalDivider()

            ConfigSwitch(
                label = "Eventos Automáticos",
                checked = autoEventsEnabled,
                onCheckedChange = onAutoEventsEnabledChange,
                icon = Icons.Default.Notifications,
                description = "Genera eventos aleatorios durante la simulación"
            )

            if (autoEventsEnabled) {
                ConfigSlider(
                    label = "Frecuencia de Eventos",
                    value = eventEverySeconds.toFloat(),
                    valueRange = 4f..60f,
                    steps = 55,
                    onValueChange = { onEventEverySecondsChange(it.roundToInt()) },
                    valueText = "${eventEverySeconds}s",
                    icon = Icons.Default.Timer,
                    description = "Cada cuánto tiempo ocurre un evento"
                )
            }
        }
    }
}

@Composable
private fun ConfigSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueText: String,
    icon: ImageVector,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfigSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}