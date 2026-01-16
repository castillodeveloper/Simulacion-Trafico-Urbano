package com.example.simulaciontraficourbano.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.simulaciontraficourbano.sim.SimulationSnapshot
import com.example.simulaciontraficourbano.sim.SimulationStats
import kotlin.math.roundToInt

@Composable
fun StatsScreen(viewModel: SimulationViewModel) {
    val snap by viewModel.snapshot.collectAsState()
    val stats = snap.stats

    var speedHistory by remember { mutableStateOf(listOf<Float>()) }

    LaunchedEffect(stats.avgSpeedCellsPerSec) {
        speedHistory = (speedHistory + stats.avgSpeedCellsPerSec.toFloat()).takeLast(50)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Estadísticas de Simulación",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Análisis en tiempo real del comportamiento del tráfico",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Vehículos",
                value = stats.activeVehicles.toString(),
                icon = Icons.Default.DirectionsCar,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "En Movimiento",
                value = stats.moving.toString(),
                icon = Icons.Default.ArrowUpward,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Detenidos",
                value = stats.stopped.toString(),
                icon = Icons.Default.Warning,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Colisiones Evitadas",
                value = stats.collisionsAvoided.toString(),
                icon = Icons.Default.Shield,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }

        SpeedChartCard(
            speedHistory = speedHistory,
            currentSpeed = stats.avgSpeedCellsPerSec.toFloat()
        )

        VehicleStatusChart(
            moving = stats.moving,
            stopped = stats.stopped,
            arrived = stats.arrived,
            total = stats.activeVehicles
        )

        AdditionalMetricsCard(stats)

        SystemInfoCard(stats, snap)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpeedChartCard(
    speedHistory: List<Float>,
    currentSpeed: Float
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null)
                    Text(
                        "Velocidad Media en el Tiempo",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = "${"%.2f".format(currentSpeed)} c/s",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                "Evolución de la velocidad promedio (celdas por segundo)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (speedHistory.isNotEmpty()) {
                LineChart(
                    data = speedHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Recopilando datos...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = (maxValue - minValue).coerceAtLeast(0.1f)

        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1).coerceAtLeast(1)

        val avg = data.average().toFloat()
        val avgY = height - ((avg - minValue) / range * height * 0.9f) - height * 0.05f
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = Offset(0f, avgY),
            end = Offset(width, avgY),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(10f, 10f)
            )
        )

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - ((value - minValue) / range * height * 0.9f) - height * 0.05f

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )

        data.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - ((value - minValue) / range * height * 0.9f) - height * 0.05f

            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(x, y)
            )
            drawCircle(
                color = color,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun VehicleStatusChart(
    moving: Int,
    stopped: Int,
    arrived: Int,
    total: Int
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null)
                Text(
                    "Estado de Vehículos",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(
                "Distribución por estado actual",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            StatusBar(
                label = "En Movimiento",
                value = moving,
                total = total,
                color = Color(0xFF4CAF50),
                icon = Icons.Default.ArrowUpward
            )

            StatusBar(
                label = "Detenidos",
                value = stopped,
                total = total,
                color = Color(0xFFFF9800),
                icon = Icons.Default.Warning
            )

            StatusBar(
                label = "Llegados al Destino",
                value = arrived,
                total = total,
                color = Color(0xFF2196F3),
                icon = Icons.Default.CheckCircle
            )
        }
    }
}

@Composable
private fun StatusBar(
    label: String,
    value: Int,
    total: Int,
    color: Color,
    icon: ImageVector
) {
    val percentage = if (total > 0) (value.toFloat() / total) else 0f
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 600),
        label = "percentage"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "$value (${(percentage * 100).roundToInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = color,
                    size = Size(width = size.width * animatedPercentage, height = size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}

// FUNCIONES AUXILIARES QUE FALTABAN

@Composable
private fun AdditionalMetricsCard(stats: SimulationStats) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Métricas Adicionales", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Tiempo Espera Total", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.totalWaitMs / 1000}s", style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Eventos Activos", style = MaterialTheme.typography.bodyMedium)
                Text(stats.activeEvents.toString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SystemInfoCard(stats: SimulationStats, snap: SimulationSnapshot) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Info del Sistema", style = MaterialTheme.typography.titleMedium)
            Text("Grid Size: ${snap.gridSize}x${snap.gridSize}", style = MaterialTheme.typography.bodySmall)
            Text("FPS Simulación: ~60", style = MaterialTheme.typography.bodySmall)
        }
    }
}