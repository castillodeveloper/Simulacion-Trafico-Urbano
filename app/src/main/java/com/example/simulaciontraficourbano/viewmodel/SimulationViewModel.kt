package com.example.simulaciontraficourbano.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simulaciontraficourbano.model.domain.data.HistoryManager
import com.example.simulaciontraficourbano.model.domain.data.SimulationRecord
import com.example.simulaciontraficourbano.model.domain.SimulationConfig
import com.example.simulaciontraficourbano.model.domain.SimulationEngine
import com.example.simulaciontraficourbano.model.domain.SimulationEvent
import com.example.simulaciontraficourbano.model.domain.SimulationSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Presets de escenarios rápidos para la simulación
enum class ScenarioPreset { LIGHT, RUSH_HOUR, EMERGENCY, CHAOS }

class SimulationViewModel(application: Application) : AndroidViewModel(application) {

    // Scope separado para la lógica pesada de la simulación
    private val logicScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val engine = SimulationEngine(
        uiScope = viewModelScope,
        logicScope = logicScope
    )

    // Se encarga de guardar y cargar el historial de simulaciones
    private val historyManager = HistoryManager(application.applicationContext)

    val snapshot: StateFlow<SimulationSnapshot> = engine.snapshot
    val eventsFlow: SharedFlow<SimulationEvent> = engine.eventsFlow

    private val _config = MutableStateFlow(SimulationConfig())
    val config: StateFlow<SimulationConfig> = _config.asStateFlow()

    // Lista que usa la UI para mostrar el historial
    private val _historyList = MutableStateFlow<List<SimulationRecord>>(emptyList())
    val historyList: StateFlow<List<SimulationRecord>> = _historyList.asStateFlow()

    fun start() = engine.start(_config.value)

    fun applyAndRestart() {
        saveCurrentSession()
        engine.start(_config.value)
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun stepOnce() = engine.stepOnce()

    fun reset() {
        saveCurrentSession()
        engine.reset()
    }

    // Guardado de sesiones en el historial
    private fun saveCurrentSession() {
        val currentSnap = snapshot.value
        val stats = currentSnap.stats

        // Solo guardamos sesiones que hayan durado un mínimo
        if (stats.simTimeMs > 6000) {
            viewModelScope.launch(Dispatchers.IO) {
                historyManager.saveSession(
                    durationMs = stats.simTimeMs,
                    vehicles = stats.activeVehicles,
                    avgSpeed = stats.avgSpeedCellsPerSec,
                    events = stats.activeEvents
                )
                // Recargamos el historial para reflejarlo en la UI
                loadHistory()
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _historyList.value = historyManager.loadHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.clearHistory()
            loadHistory()
        }
    }

    fun loadSavedConfig(savedConfig: SimulationConfig) {
        _config.value = savedConfig
    }

    fun setSpeed(mult: Double) {
        val m = mult.coerceIn(0.5, 5.0)
        _config.value = _config.value.copy(simSpeed = m)
        engine.setSpeed(m)
    }

    fun setVehicleCount(n: Int) {
        val count = n.coerceIn(5, 100)
        val amb = _config.value.ambulanceCount.coerceIn(0, count)
        _config.value = _config.value.copy(vehicleCount = count, ambulanceCount = amb)
    }

    fun setAmbulanceCount(n: Int) {
        val amb = n.coerceIn(0, _config.value.vehicleCount)
        _config.value = _config.value.copy(ambulanceCount = amb)
    }

    fun setLightsEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(lightsEnabled = enabled)
    }

    fun setCollisionsEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(collisionsEnabled = enabled)
    }

    fun setGreenSeconds(sec: Int) {
        val s = sec.coerceIn(3, 30)
        _config.value = _config.value.copy(lightGreenMs = s * 1000L)
    }

    fun setAutoEventsEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(autoEventsEnabled = enabled)
    }

    fun setEventEverySeconds(sec: Int) {
        val s = sec.coerceIn(4, 60)
        _config.value = _config.value.copy(eventEveryMs = s * 1000L)
    }

    // Eventos manuales desde la UI
    fun accident() = engine.triggerAccident()
    fun roadworks() = engine.triggerRoadworks()
    fun congestion() = engine.triggerCongestion()
    fun emergency() = engine.triggerEmergency()

    fun applyScenario(preset: ScenarioPreset) {
        val newCfg = when (preset) {
            ScenarioPreset.LIGHT ->
                SimulationConfig(10, 1, 2, 140L, 1.0, 8000L, 1500L, 600L, true, true, true, 12000L)
            ScenarioPreset.RUSH_HOUR ->
                SimulationConfig(50, 2, 2, 140L, 1.0, 8000L, 1500L, 600L, true, true, true, 12000L)
            ScenarioPreset.EMERGENCY ->
                SimulationConfig(35, 10, 2, 140L, 1.2, 6000L, 1200L, 600L, true, true, true, 12000L)
            ScenarioPreset.CHAOS ->
                SimulationConfig(40, 0, 2, 140L, 1.3, 8000L, 1500L, 600L, false, false, true, 12000L)
        }
        _config.value = newCfg
        engine.start(newCfg)
    }

    override fun onCleared() {
        engine.stop()
        logicScope.cancel()
        super.onCleared()
    }
}
