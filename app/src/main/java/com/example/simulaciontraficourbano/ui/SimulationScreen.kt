package com.example.simulaciontraficourbano.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.simulaciontraficourbano.sim.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SimulationScreen(viewModel: SimulationViewModel) {
    val snap by viewModel.snapshot.collectAsState()
    val cfg by viewModel.config.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedVehicle by remember { mutableStateOf<VehicleState?>(null) }
    var selectedLight by remember { mutableStateOf<TrafficLightView?>(null) }
    var statsExpanded by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Escuchar eventos
    LaunchedEffect(viewModel.eventsFlow) {
        viewModel.eventsFlow.collectLatest { event ->
            snackbarHostState.showSnackbar(
                message = event.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 4.0f)
        offset += panChange
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de reset zoom
                SmallFloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }
                ) {
                    Icon(Icons.Default.Refresh, "Reset Zoom")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // CANVAS PRINCIPAL (60% de la pantalla)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(transformState)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val cellSize = 64f
                                val x = (tapOffset.x - offset.x) / scale
                                val y = (tapOffset.y - offset.y) / scale

                                // Detectar click en vehículo
                                val clicked = snap.vehicles.find { v ->
                                    val vx = (v.pos.x + 0.5f) * cellSize
                                    val vy = (v.pos.y + 0.5f) * cellSize
                                    val dist = kotlin.math.sqrt((x - vx) * (x - vx) + (y - vy) * (y - vy))
                                    dist < 30f
                                }

                                if (clicked != null) {
                                    selectedVehicle = clicked
                                    selectedLight = null
                                } else {
                                    // Detectar click en semáforo
                                    val clickedLight = snap.lights.find { l ->
                                        val lx = (l.pos.x + 0.5f) * cellSize
                                        val ly = (l.pos.y + 0.5f) * cellSize
                                        val dist = kotlin.math.sqrt((x - lx) * (x - lx) + (y - ly) * (y - ly))
                                        dist < 30f
                                    }
                                    if (clickedLight != null) {
                                        selectedLight = clickedLight
                                        selectedVehicle = null
                                    } else {
                                        selectedVehicle = null
                                        selectedLight = null
                                    }
                                }
                            }
                        }
                ) {
                    drawTrafficCanvas(
                        snapshot = snap,
                        scale = scale,
                        offset = offset,
                        selectedVehicle = selectedVehicle,
                        selectedLight = selectedLight
                    )
                }

                // Panel de estadísticas desplegable - ARRIBA DEL TODO
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Fila del título con zoom y botón de colapsar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Título
                            Text(
                                "Estadísticas en Tiempo Real",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Zoom y botón de colapsar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Indicador de zoom
                                Text(
                                    text = "Zoom: ${(scale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Botón de colapsar/expandir
                                IconButton(
                                    onClick = { statsExpanded = !statsExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (statsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (statsExpanded) "Colapsar" else "Expandir",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Contenido desplegable
                        if (statsExpanded) {
                            Divider(modifier = Modifier.padding(vertical = 6.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatsRow("Vehículos activos", snap.stats.activeVehicles.toString())
                                StatsRow("En movimiento", "${snap.stats.moving} (${((snap.stats.moving.toFloat() / snap.stats.activeVehicles.coerceAtLeast(1)) * 100).toInt()}%)")
                                StatsRow("Detenidos", snap.stats.stopped.toString())
                                StatsRow("Velocidad media", "%.2f".format(snap.stats.avgSpeedCellsPerSec))
                                StatsRow("Tiempo total", "${snap.stats.simTimeMs / 1000}s")
                            }
                        }
                    }
                }
            }

            Divider()

            // CONTROLES Y CONFIGURACIÓN (40% de la pantalla)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(MaterialTheme.colorScheme.background)  // Fondo blanco/tema
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CONTROLES PRINCIPALES
                ControlSection(viewModel)

                // EVENTOS
                EventsSection(viewModel)

                // INSPECTOR (si hay algo seleccionado)
                if (selectedVehicle != null || selectedLight != null) {
                    InspectorSection(selectedVehicle, selectedLight) {
                        selectedVehicle = null
                        selectedLight = null
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ControlSection(viewModel: SimulationViewModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Controles de Simulación",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.start() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Iniciar")
                }

                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reiniciar")
                }

                OutlinedButton(
                    onClick = { viewModel.applyAndRestart() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Configurar")
                }
            }
        }
    }
}

@Composable
private fun EventsSection(viewModel: SimulationViewModel) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Eventos de Simulación",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.accident() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🚑 Accidente", style = MaterialTheme.typography.bodySmall)
                }

                FilledTonalButton(
                    onClick = { viewModel.roadworks() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🚧 Obras", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.congestion() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🚗 Congestión", style = MaterialTheme.typography.bodySmall)
                }

                FilledTonalButton(
                    onClick = { viewModel.emergency() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🚨 Emergencia", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun InspectorSection(
    vehicle: VehicleState?,
    light: TrafficLightView?,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                Text(
                    if (vehicle != null) "🚗 Inspector de Vehículo" else "🚦 Inspector de Semáforo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }

            Divider()

            if (vehicle != null) {
                InspectorRow("ID", "#${vehicle.id}")
                InspectorRow("Tipo", vehicle.type.toString())
                InspectorRow("Estado", vehicle.status.toString())
                InspectorRow("Posición", "(${vehicle.pos.x}, ${vehicle.pos.y})")
                InspectorRow("Destino", "(${vehicle.dest.x}, ${vehicle.dest.y})")
                InspectorRow("Celdas recorridas", vehicle.movedCells.toString())
                InspectorRow("Tiempo esperando", "${vehicle.waitMs}ms")
            } else if (light != null) {
                InspectorRow("ID", "#${light.id}")
                InspectorRow("Posición", "(${light.pos.x}, ${light.pos.y})")
                InspectorRow("NS", light.ns.toString())
                InspectorRow("EW", light.ew.toString())
                InspectorRow("Cambio en", "${light.remainingMs / 1000.0}s")
            }
        }
    }
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// Función de dibujo del canvas (tu código actual mejorado)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrafficCanvas(
    snapshot: SimulationSnapshot,
    scale: Float,
    offset: Offset,
    selectedVehicle: VehicleState?,
    selectedLight: TrafficLightView?
) {
    val cell = 64f
    val size = snapshot.gridSize

    withTransform({
        translate(offset.x, offset.y)
        scale(scale, scale)
    }) {
        val mapW = size * cell
        val mapH = size * cell

        // Césped de fondo
        val grass = Color(0xFF79C34A)
        val grassDark = Color(0xFF62A83A)
        drawRect(color = grass, topLeft = Offset(0f, 0f), size = Size(mapW, mapH))

        // Manchas de césped
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((x + y) % 3 == 0) {
                    drawRoundRect(
                        color = grassDark,
                        topLeft = Offset(x * cell + cell * 0.06f, y * cell + cell * 0.06f),
                        size = Size(cell * 0.88f, cell * 0.88f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                }
            }
        }

        // Calles (asfalto)
        val asphalt = Color(0xFF2F2F33)
        val asphaltEdge = Color(0xFF202024)
        val lane = Color(0xFFF2D34B)
        val curb = Color(0xFF3A3A3F)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val left = x * cell
                val top = y * cell

                // Asfalto
                drawRoundRect(
                    color = asphaltEdge,
                    topLeft = Offset(left + cell * 0.12f, top + cell * 0.12f),
                    size = Size(cell * 0.76f, cell * 0.76f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = asphalt,
                    topLeft = Offset(left + cell * 0.14f, top + cell * 0.14f),
                    size = Size(cell * 0.72f, cell * 0.72f),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Bordillo
                drawRoundRect(
                    color = curb,
                    topLeft = Offset(left + cell * 0.10f, top + cell * 0.10f),
                    size = Size(cell * 0.80f, cell * 0.80f),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 2f)
                )

                // Líneas discontinuas
                val cx = left + cell / 2f
                val cy = top + cell / 2f
                val a = cell * 0.26f

                // Vertical
                drawLine(
                    color = lane,
                    start = Offset(cx, cy - a),
                    end = Offset(cx, cy + a),
                    strokeWidth = 3f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(cell * 0.06f, cell * 0.05f)
                    )
                )
                // Horizontal
                drawLine(
                    color = lane,
                    start = Offset(cx - a, cy),
                    end = Offset(cx + a, cy),
                    strokeWidth = 3f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(cell * 0.06f, cell * 0.05f)
                    )
                )
            }
        }

        // Eventos (zonas bloqueadas)
        snapshot.events.forEach { ev ->
            val p = ev.pos ?: return@forEach
            val left = p.x * cell
            val top = p.y * cell
            val overlay = when (ev.type) {
                EventType.ACCIDENT -> Color(0x55D50000)
                EventType.ROADWORKS -> Color(0x55FF6D00)
                EventType.CONGESTION -> Color(0x554B7BE5)
                EventType.EMERGENCY -> Color.Transparent
            }
            if (overlay != Color.Transparent) {
                drawRoundRect(
                    color = overlay,
                    topLeft = Offset(left + cell * 0.14f, top + cell * 0.14f),
                    size = Size(cell * 0.72f, cell * 0.72f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }
        }

        // Semáforos
        val zebra = Color(0xFFEDEDED)
        snapshot.lights.forEach { l ->
            val center = Offset((l.pos.x + 0.5f) * cell, (l.pos.y + 0.5f) * cell)

            // Paso de cebra horizontal
            val stripes = 6
            val stripeW = cell * 0.10f
            val stripeH = cell * 0.32f
            val gap = cell * 0.03f
            val startOffset = -(stripes / 2f) * (stripeW + gap)

            for (i in 0 until stripes) {
                val o = startOffset + i * (stripeW + gap)
                drawRoundRect(
                    color = zebra,
                    topLeft = Offset(center.x + o, center.y - stripeH / 2f),
                    size = Size(stripeW, stripeH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = zebra,
                    topLeft = Offset(center.x - stripeH / 2f, center.y + o),
                    size = Size(stripeH, stripeW),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            // Semáforo
            val boxW = cell * 0.18f
            val boxH = cell * 0.34f
            val poleH = cell * 0.18f

            // Poste
            drawRoundRect(
                color = Color(0xFF1A1A1D),
                topLeft = Offset(center.x - boxW * 0.12f, center.y + boxH / 2f),
                size = Size(boxW * 0.24f, poleH),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Caja
            drawRoundRect(
                color = Color(0xFF1A1A1D),
                topLeft = Offset(center.x - boxW / 2f, center.y - boxH / 2f),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Luz activa
            val nsColor = when (l.ns) {
                LightColor.GREEN -> Color(0xFF00C853)
                LightColor.YELLOW -> Color(0xFFFFD600)
                LightColor.RED -> Color(0xFFD50000)
            }
            val ewColor = when (l.ew) {
                LightColor.GREEN -> Color(0xFF00C853)
                LightColor.YELLOW -> Color(0xFFFFD600)
                LightColor.RED -> Color(0xFFD50000)
            }

            val activeColor = if (l.ns != LightColor.RED) nsColor else ewColor

            drawCircle(
                color = activeColor,
                radius = boxW * 0.22f,
                center = center
            )

            // Indicador de dirección
            val d = boxW * 0.35f
            if (l.ns != LightColor.RED) {
                drawLine(Color.Black, Offset(center.x, center.y - d), Offset(center.x, center.y + d), 3f)
            } else {
                drawLine(Color.Black, Offset(center.x - d, center.y), Offset(center.x + d, center.y), 3f)
            }

            // Highlight si seleccionado
            if (selectedLight?.id == l.id) {
                drawCircle(
                    color = Color.Cyan,
                    radius = 25f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
        }

        // Vehículos
        snapshot.vehicles.forEach { v ->
            val center = Offset((v.pos.x + 0.5f) * cell, (v.pos.y + 0.5f) * cell)

            val dx = v.dest.x - v.pos.x
            val dy = v.dest.y - v.pos.y
            val angle = when {
                kotlin.math.abs(dx) >= kotlin.math.abs(dy) && dx > 0 -> 0f
                kotlin.math.abs(dx) >= kotlin.math.abs(dy) && dx < 0 -> 180f
                dy > 0 -> 90f
                dy < 0 -> 270f
                else -> 0f
            }

            val baseW = cell * 0.52f * v.type.size
            val baseH = cell * 0.26f * v.type.size
            val body = Color(v.colorArgb)

            withTransform({
                rotate(angle, pivot = center)
            }) {
                // Sombra
                drawRoundRect(
                    color = Color(0x66000000),
                    topLeft = Offset(center.x - baseW / 2f + 2f, center.y - baseH / 2f + 3f),
                    size = Size(baseW, baseH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Carrocería
                drawRoundRect(
                    color = body,
                    topLeft = Offset(center.x - baseW / 2f, center.y - baseH / 2f),
                    size = Size(baseW, baseH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Parabrisas
                drawRoundRect(
                    color = Color(0x66FFFFFF),
                    topLeft = Offset(center.x - baseW * 0.12f, center.y - baseH * 0.22f),
                    size = Size(baseW * 0.22f, baseH * 0.44f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Faros
                drawCircle(Color(0xFFEDEDED), radius = baseH * 0.10f, center = Offset(center.x + baseW * 0.38f, center.y - baseH * 0.18f))
                drawCircle(Color(0xFFEDEDED), radius = baseH * 0.10f, center = Offset(center.x + baseW * 0.38f, center.y + baseH * 0.18f))
            }

            // Highlight si seleccionado
            if (selectedVehicle?.id == v.id) {
                drawCircle(
                    color = Color.Cyan,
                    radius = 25f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}