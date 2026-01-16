package com.example.simulaciontraficourbano.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SimulationRecord(
    val date: String,
    val durationSec: Long,
    val vehicles: Int,
    val avgSpeed: Double,
    val accidents: Int
)

class HistoryManager(private val context: Context) {
    private val fileName = "simulation_history.json"

    fun saveSession(durationMs: Long, vehicles: Int, avgSpeed: Double, events: Int) {
        if (durationMs < 1000) return // No guardar sesiones de menos de 1 segundo

        val history = loadHistory().toMutableList()

        // Crear nuevo registro
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val newRecord = SimulationRecord(
            date = dateFormat.format(Date()),
            durationSec = durationMs / 1000,
            vehicles = vehicles,
            avgSpeed = avgSpeed,
            accidents = events
        )

        history.add(0, newRecord) // Añadir al principio

        // Convertir a JSON String nativo
        val jsonArray = JSONArray()
        history.forEach { record ->
            val jsonObj = JSONObject().apply {
                put("date", record.date)
                put("duration", record.durationSec)
                put("vehicles", record.vehicles)
                put("speed", record.avgSpeed)
                put("events", record.accidents)
            }
            jsonArray.put(jsonObj)
        }

        // Escribir en archivo
        try {
            val file = File(context.filesDir, fileName)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadHistory(): List<SimulationRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        val list = mutableListOf<SimulationRecord>()
        try {
            val content = file.readText()
            val jsonArray = JSONArray(content)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SimulationRecord(
                        date = obj.optString("date"),
                        durationSec = obj.optLong("duration"),
                        vehicles = obj.optInt("vehicles"),
                        avgSpeed = obj.optDouble("speed"),
                        accidents = obj.optInt("events")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        return list
    }

    // Opcional: Borrar historial
    fun clearHistory() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) file.delete()
    }
}