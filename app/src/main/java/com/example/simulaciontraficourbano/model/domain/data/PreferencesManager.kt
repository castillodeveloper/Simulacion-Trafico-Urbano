package com.example.simulaciontraficourbano.model.domain.data

import android.content.Context
import com.example.simulaciontraficourbano.model.domain.SimulationConfig
import java.lang.Double

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("sim_trafico_prefs", Context.MODE_PRIVATE)

    fun saveConfig(config: SimulationConfig) {
        prefs.edit().apply {
            putInt("gridSize", config.gridSize)
            putInt("vehicleCount", config.vehicleCount)
            putInt("ambulanceCount", config.ambulanceCount)
            // Guardamos el double como bits (Long) para no perder precisión
            putLong("simSpeed", Double.doubleToRawLongBits(config.simSpeed))
            putBoolean("lightsEnabled", config.lightsEnabled)
            putBoolean("collisionsEnabled", config.collisionsEnabled)
            putInt("greenSeconds", (config.lightGreenMs / 1000).toInt())
            putBoolean("autoEvents", config.autoEventsEnabled)
            putInt("eventFreq", (config.eventEveryMs / 1000).toInt())
            apply()
        }
    }

    fun loadConfig(): SimulationConfig {
        // Valores por defecto si es la primera vez que se abre la app
        val speedBits = prefs.getLong("simSpeed", Double.doubleToRawLongBits(1.0))
        val greenSecs = prefs.getInt("greenSeconds", 8)
        val eventFreq = prefs.getInt("eventFreq", 12)

        return SimulationConfig(
            gridSize = prefs.getInt("gridSize", 10),
            vehicleCount = prefs.getInt("vehicleCount", 30),
            ambulanceCount = prefs.getInt("ambulanceCount", 2),
            simSpeed = Double.longBitsToDouble(speedBits),
            lightsEnabled = prefs.getBoolean("lightsEnabled", true),
            collisionsEnabled = prefs.getBoolean("collisionsEnabled", true),
            lightGreenMs = greenSecs * 1000L,
            autoEventsEnabled = prefs.getBoolean("autoEvents", true),
            eventEveryMs = eventFreq * 1000L
        )
    }
}