package com.example.simulaciontraficourbano.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simulaciontraficourbano.data.SimulationRecord

@Composable
fun HistoryScreen(viewModel: SimulationViewModel) {
    val historyList by viewModel.historyList.collectAsState()
    val context = LocalContext.current

    // Cargar historial al entrar
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                // BOTÓN EXPORTAR CSV
                IconButton(onClick = {
                    if (historyList.isNotEmpty()) {
                        shareCsv(context, historyList)
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Exportar CSV", tint = MaterialTheme.colorScheme.primary)
                }

                // BOTÓN BORRAR
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar historial", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No hay sesiones", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyList) { record ->
                    HistoryItemCard(record)
                }
            }
        }
    }
}

// Función auxiliar para crear y compartir el CSV
fun shareCsv(context: android.content.Context, list: List<SimulationRecord>) {
    val header = "Fecha,Duracion(s),Vehiculos,VelocidadMedia,Eventos\n"
    val rows = list.joinToString("\n") { r ->
        "${r.date},${r.durationSec},${r.vehicles},%.2f,${r.accidents}".format(r.avgSpeed)
    }
    val csvContent = header + rows

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csvContent)
        putExtra(Intent.EXTRA_SUBJECT, "Historial de Simulación Tráfico.csv")
        type = "text/plain" // Usamos text/plain para máxima compatibilidad
    }
    val shareIntent = Intent.createChooser(sendIntent, "Exportar registro a...")
    context.startActivity(shareIntent)
}

@Composable
fun HistoryItemCard(record: SimulationRecord) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(record.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Text("${record.durationSec}s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Vehículos", style = MaterialTheme.typography.bodySmall); Text("${record.vehicles}", style = MaterialTheme.typography.bodyLarge) }
                Column { Text("Vel. Media", style = MaterialTheme.typography.bodySmall); Text("%.2f".format(record.avgSpeed), style = MaterialTheme.typography.bodyLarge) }
                Column { Text("Eventos", style = MaterialTheme.typography.bodySmall); Text("${record.accidents}", style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}