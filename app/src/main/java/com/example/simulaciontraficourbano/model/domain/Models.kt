package com.example.simulaciontraficourbano.model.domain

enum class VehicleType(val speedFactor: Double, val size: Float) {
    CAR(1.0, 0.70f),
    BUS(0.7, 1.05f),
    MOTO(1.4, 0.55f),
    AMBULANCE(1.2, 0.75f)
}

enum class VehicleStatus { MOVING, STOPPED, ARRIVED }

enum class LightColor { RED, YELLOW, GREEN }

enum class Axis { NS, EW } // Norte-Sur / Este-Oeste

// Eventos (Paso 6)
enum class EventType { ACCIDENT, ROADWORKS, CONGESTION, EMERGENCY }

data class SimulationEvent(
    val id: Long,
    val type: EventType,
    val pos: GridPos?,           // puede ser null (ej: emergencia global)
    val startMs: Long,
    val durationMs: Long,
    val message: String
) {
    val endMs: Long get() = startMs + durationMs
}

data class GridPos(val x: Int, val y: Int) {
    fun key(size: Int): Int = x * size + y
}

data class VehicleState(
    val id: Int,
    val type: VehicleType,
    val pos: GridPos,
    val dest: GridPos,
    val status: VehicleStatus,
    val colorArgb: Long, // 0xAARRGGBB
    val movedCells: Int,
    val waitMs: Long
)

data class TrafficLightView(
    val id: Int,
    val pos: GridPos,
    val ns: LightColor,
    val ew: LightColor,
    val remainingMs: Long
)

data class SimulationConfig(
    val gridSize: Int = 10,
    val vehicleCount: Int = 30,
    val ambulanceCount: Int = 2,
    val baseStepMs: Long = 140L,
    val simSpeed: Double = 1.0,

    val lightGreenMs: Long = 8000L,
    val lightYellowMs: Long = 1500L,
    val lightAllRedMs: Long = 600L,

    val lightsEnabled: Boolean = true,
    val collisionsEnabled: Boolean = true,

    // Paso 6: eventos automáticos
    val autoEventsEnabled: Boolean = true,
    val eventEveryMs: Long = 12000L
)

data class SimulationStats(
    val activeVehicles: Int,
    val moving: Int,
    val stopped: Int,
    val arrived: Int,
    val avgSpeedCellsPerSec: Double,
    val totalWaitMs: Long,
    val collisionsAvoided: Long,
    val simTimeMs: Long,
    val activeEvents: Int
)

data class SimulationSnapshot(
    val gridSize: Int,
    val vehicles: List<VehicleState>,
    val lights: List<TrafficLightView>,
    val events: List<SimulationEvent>,
    val stats: SimulationStats
) {
    companion object {
        fun empty(): SimulationSnapshot = SimulationSnapshot(
            gridSize = 10,
            vehicles = emptyList(),
            lights = emptyList(),
            events = emptyList(),
            stats = SimulationStats(
                activeVehicles = 0, moving = 0, stopped = 0, arrived = 0,
                avgSpeedCellsPerSec = 0.0,
                totalWaitMs = 0L,
                collisionsAvoided = 0L,
                simTimeMs = 0L,
                activeEvents = 0
            )
        )
    }
}
