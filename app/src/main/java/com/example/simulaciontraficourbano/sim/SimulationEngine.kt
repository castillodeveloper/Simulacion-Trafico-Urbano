package com.example.simulaciontraficourbano.sim

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class SimulationEngine(
    private val uiScope: CoroutineScope,
    private val logicScope: CoroutineScope
) {
    private val idGen = AtomicInteger(1)
    private val eventIdGen = AtomicLong(1L)

    private var config: SimulationConfig = SimulationConfig()
    private var gridSize: Int = config.gridSize

    private val vehicles = ConcurrentHashMap<Int, VehicleRuntime>()
    private val lights = ConcurrentHashMap<Int, TrafficLightRuntime>()

    // Ocupación: cellKey -> vehicleId
    private val occupancy = ConcurrentHashMap<Int, Int>()
    private var cellLocks: Array<Mutex> = Array(config.gridSize * config.gridSize) { Mutex() }

    private val collisionsAvoided = AtomicLong(0L)

    // Paso 6: celdas bloqueadas por accidente/obras: cellKey -> expiryMs
    private val blockedCells = ConcurrentHashMap<Int, Long>()

    // Paso 6: eventos activos: eventId -> event
    private val activeEvents = ConcurrentHashMap<Long, SimulationEvent>()

    // Paso 6: notificaciones hacia UI
    private val _eventsFlow = MutableSharedFlow<SimulationEvent>(extraBufferCapacity = 64)
    val eventsFlow: SharedFlow<SimulationEvent> = _eventsFlow.asSharedFlow()

    @Volatile private var running: Boolean = false
    @Volatile private var speedMultiplier: Double = 1.0

    private var emitterJob: Job? = null
    private var autoEventsJob: Job? = null
    private var startTimeMs: Long = 0L

    private val _snapshot = MutableStateFlow(SimulationSnapshot.empty())
    val snapshot: StateFlow<SimulationSnapshot> = _snapshot.asStateFlow()

    fun start(newConfig: SimulationConfig) {
        reset()

        config = newConfig
        gridSize = config.gridSize
        cellLocks = Array(gridSize * gridSize) { Mutex() }
        speedMultiplier = config.simSpeed
        startTimeMs = System.currentTimeMillis()

        running = true
        createLights()
        createVehicles()

        if (config.autoEventsEnabled) startAutoEvents()

        // Emisión a UI ~60 FPS
        emitterJob = uiScope.launch(Dispatchers.Main) {
            while (isActive) {
                _snapshot.value = buildSnapshot(System.currentTimeMillis())
                delay(16L)
            }
        }
    }

    fun pause() { running = false }
    fun resume() { running = true }
    fun setSpeed(mult: Double) { speedMultiplier = mult.coerceIn(0.5, 5.0) }

    fun stepOnce() {
        running = false
        logicScope.launch { vehicles.values.forEach { it.attemptMoveOnce() } }
    }

    fun reset() {
        running = false
        emitterJob?.cancel()
        emitterJob = null

        autoEventsJob?.cancel()
        autoEventsJob = null

        vehicles.values.forEach { it.stop() }
        vehicles.clear()

        lights.values.forEach { it.stop() }
        lights.clear()

        occupancy.clear()
        blockedCells.clear()
        activeEvents.clear()
        collisionsAvoided.set(0L)

        _snapshot.value = SimulationSnapshot.empty()
    }

    fun stop() = reset()

    // ------------------------
    // Paso 6: eventos manuales
    // ------------------------
    fun triggerAccident() {
        logicScope.launch {
            val v = vehicles.values.randomOrNull()?.state
            val pos = v?.pos ?: randomCell(Random(System.currentTimeMillis()))
            createBlockingEvent(
                type = EventType.ACCIDENT,
                pos = pos,
                durationMs = 6_000L,
                message = "🚑 Accidente en (${pos.x}, ${pos.y}) - calle bloqueada 6s"
            )
        }
    }

    fun triggerRoadworks() {
        logicScope.launch {
            val pos = randomFreeOrAnyCell(Random(System.currentTimeMillis()))
            createBlockingEvent(
                type = EventType.ROADWORKS,
                pos = pos,
                durationMs = 15_000L,
                message = "🚧 Obras en (${pos.x}, ${pos.y}) - calle cerrada 15s"
            )
        }
    }

    fun triggerCongestion() {
        logicScope.launch {
            val rng = Random(System.currentTimeMillis())
            val add = 8
            repeat(add) { addVehicle(VehicleType.entries.random(rng), rng) }
            val e = createNonBlockingEvent(
                type = EventType.CONGESTION,
                pos = null,
                durationMs = 8_000L,
                message = "🚗 Congestión: +$add vehículos en la ciudad"
            )
            _eventsFlow.tryEmit(e)
        }
    }

    fun triggerEmergency() {
        logicScope.launch {
            val rng = Random(System.currentTimeMillis())
            addVehicle(VehicleType.AMBULANCE, rng)
            val e = createNonBlockingEvent(
                type = EventType.EMERGENCY,
                pos = null,
                durationMs = 5_000L,
                message = "🚨 Emergencia: ambulancia añadida con prioridad"
            )
            _eventsFlow.tryEmit(e)
        }
    }

    // ------------------------
    // Auto eventos
    // ------------------------
    private fun startAutoEvents() {
        autoEventsJob?.cancel()
        autoEventsJob = logicScope.launch(Dispatchers.Default) {
            val rng = Random(System.currentTimeMillis())
            while (isActive) {
                delay(config.eventEveryMs.coerceIn(4000L, 60_000L))
                if (!running) continue

                when (rng.nextInt(4)) {
                    0 -> triggerAccident()
                    1 -> triggerRoadworks()
                    2 -> triggerCongestion()
                    else -> triggerEmergency()
                }
            }
        }
    }

    // ------------------------
    // Semáforos / vehículos
    // ------------------------
    private fun createLights() {
        if (!config.lightsEnabled) return

        val positions = listOf(
            GridPos(2, 2), GridPos(2, 5), GridPos(2, 7),
            GridPos(5, 2), GridPos(5, 5),
            GridPos(7, 2), GridPos(7, 5), GridPos(7, 7)
        ).filter { it.x in 0 until gridSize && it.y in 0 until gridSize }

        positions.forEach { pos ->
            val id = idGen.getAndIncrement()
            val light = TrafficLightRuntime(
                id = id,
                pos = pos,
                greenMs = config.lightGreenMs,
                yellowMs = config.lightYellowMs,
                allRedMs = config.lightAllRedMs,
                scope = logicScope
            )
            lights[id] = light
            light.start()
        }
    }

    private fun createVehicles() {
        val rng = Random(System.currentTimeMillis())
        val count = config.vehicleCount.coerceIn(5, 100)
        val ambCount = config.ambulanceCount.coerceIn(0, count)

        repeat(ambCount) { addVehicle(VehicleType.AMBULANCE, rng) }
        repeat(count - ambCount) { addVehicle(VehicleType.entries.random(rng), rng) }
    }

    private fun addVehicle(type: VehicleType, rng: Random) {
        val id = idGen.getAndIncrement()
        val start = randomFreeCell(rng)
        val dest = randomCell(rng)
        val color = randomColorArgb(rng, type)

        val rt = VehicleRuntime(
            id = id,
            type = type,
            initialPos = start,
            initialDest = dest,
            colorArgb = color,
            engine = this,
            scope = logicScope
        )
        vehicles[id] = rt
        occupancy[start.key(gridSize)] = id
        rt.start()
    }

    private fun randomCell(rng: Random): GridPos =
        GridPos(rng.nextInt(gridSize), rng.nextInt(gridSize))

    private fun randomFreeOrAnyCell(rng: Random): GridPos {
        var tries = 0
        while (tries < 300) {
            val p = randomCell(rng)
            val key = p.key(gridSize)
            if (!blockedCells.containsKey(key)) return p
            tries++
        }
        return GridPos(0, 0)
    }

    private fun randomFreeCell(rng: Random): GridPos {
        var tries = 0
        while (tries < 800) {
            val p = randomCell(rng)
            val key = p.key(gridSize)
            val blocked = blockedCells[key]
            if (blocked != null && System.currentTimeMillis() < blocked) {
                tries++
                continue
            }
            if (!occupancy.containsKey(key)) return p
            tries++
        }
        return GridPos(0, 0)
    }

    private fun randomColorArgb(rng: Random, type: VehicleType): Long = when (type) {
        VehicleType.AMBULANCE -> 0xFFFF69B4L  // Rosa (hotpink)
        VehicleType.BUS -> 0xFFFFD700L        // Amarillo dorado
        VehicleType.MOTO -> 0xFFFFFFFFL       // Blanco
        VehicleType.CAR -> 0xFFDC143CL        // Rojo crimson (fijo)
    }

    internal fun getGridSize(): Int = gridSize
    internal fun isRunning(): Boolean = running
    internal fun getSpeedMultiplier(): Double = speedMultiplier
    internal fun getConfig(): SimulationConfig = config

    private fun findLightAt(pos: GridPos): TrafficLightRuntime? =
        lights.values.firstOrNull { it.pos == pos }

    internal fun avoidCollision() {
        collisionsAvoided.incrementAndGet()
    }

    internal suspend fun tryMove(
        vehicleId: Int,
        from: GridPos,
        to: GridPos,
        isAmbulance: Boolean
    ): Boolean {
        val fromKey = from.key(gridSize)
        val toKey = to.key(gridSize)
        if (fromKey == toKey) return true

        // Bloqueos por evento (accidente/obras)
        val now = System.currentTimeMillis()
        val expiry = blockedCells[toKey]
        if (expiry != null) {
            if (now < expiry) return false
            blockedCells.remove(toKey, expiry)
        }

        // Semáforo
        val light = if (config.lightsEnabled) findLightAt(to) else null
        if (light != null) {
            if (isAmbulance) {
                val axis = if (to.x != from.x) Axis.EW else Axis.NS
                light.requestPriority(axis)
            }
            if (!light.isGreenForMove(from, to)) return false
        }

        if (!config.collisionsEnabled) {
            occupancy.remove(fromKey)
            occupancy[toKey] = vehicleId
            return true
        }

        val first = minOf(fromKey, toKey)
        val second = maxOf(fromKey, toKey)

        return cellLocks[first].withLock {
            cellLocks[second].withLock {
                if (occupancy.containsKey(toKey)) {
                    avoidCollision()
                    false
                } else {
                    occupancy.remove(fromKey)
                    occupancy[toKey] = vehicleId
                    true
                }
            }
        }
    }

    // ------------------------
    // Eventos helpers
    // ------------------------
    private fun createBlockingEvent(
        type: EventType,
        pos: GridPos,
        durationMs: Long,
        message: String
    ) {
        val now = System.currentTimeMillis()
        val id = eventIdGen.getAndIncrement()
        val e = SimulationEvent(
            id = id,
            type = type,
            pos = pos,
            startMs = now,
            durationMs = durationMs,
            message = message
        )
        activeEvents[id] = e
        blockedCells[pos.key(gridSize)] = e.endMs
        _eventsFlow.tryEmit(e)
    }

    private fun createNonBlockingEvent(
        type: EventType,
        pos: GridPos?,
        durationMs: Long,
        message: String
    ): SimulationEvent {
        val now = System.currentTimeMillis()
        val id = eventIdGen.getAndIncrement()
        val e = SimulationEvent(
            id = id,
            type = type,
            pos = pos,
            startMs = now,
            durationMs = durationMs,
            message = message
        )
        activeEvents[id] = e
        return e
    }

    private fun cleanupExpired(now: Long) {
        // limpiar eventos expirados
        activeEvents.entries.forEach { (id, ev) ->
            if (now >= ev.endMs) activeEvents.remove(id)
        }
        // limpiar bloqueos expirados
        blockedCells.entries.forEach { (cellKey, expiry) ->
            if (now >= expiry) blockedCells.remove(cellKey)
        }
    }

    private fun buildSnapshot(nowMs: Long): SimulationSnapshot {
        cleanupExpired(nowMs)

        val v = vehicles.values.map { it.state }
        val l = lights.values.map { it.toView() }
        val e = activeEvents.values.toList()

        val moving = v.count { it.status == VehicleStatus.MOVING }
        val stopped = v.count { it.status == VehicleStatus.STOPPED }
        val arrived = v.count { it.status == VehicleStatus.ARRIVED }

        val simTime = (nowMs - startTimeMs).coerceAtLeast(0L)
        val totalMoved = v.sumOf { it.movedCells }
        val totalWait = v.sumOf { it.waitMs }
        val avgSpeed = if (simTime > 0L) (totalMoved.toDouble() / (simTime.toDouble() / 1000.0)) else 0.0

        return SimulationSnapshot(
            gridSize = gridSize,
            vehicles = v,
            lights = l,
            events = e,
            stats = SimulationStats(
                activeVehicles = v.size,
                moving = moving,
                stopped = stopped,
                arrived = arrived,
                avgSpeedCellsPerSec = avgSpeed,
                totalWaitMs = totalWait,
                collisionsAvoided = collisionsAvoided.get(),
                simTimeMs = simTime,
                activeEvents = e.size
            )
        )
    }
}

private class VehicleRuntime(
    val id: Int,
    val type: VehicleType,
    initialPos: GridPos,
    initialDest: GridPos,
    val colorArgb: Long,
    private val engine: SimulationEngine,
    private val scope: CoroutineScope
) {
    @Volatile var state: VehicleState = VehicleState(
        id = id,
        type = type,
        pos = initialPos,
        dest = initialDest,
        status = VehicleStatus.MOVING,
        colorArgb = colorArgb,
        movedCells = 0,
        waitMs = 0L
    )
        private set

    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (engine.isRunning()) attemptMoveOnce()

                val base = engine.getConfig().baseStepMs
                val speed = engine.getSpeedMultiplier() * type.speedFactor
                val delayMs = (base / speed).toLong().coerceIn(20L, 600L)
                delay(delayMs)
            }
        }
    }

    fun stop() { job?.cancel() }

    suspend fun attemptMoveOnce() {
        val gridSize = engine.getGridSize()
        val cur = state.pos
        val dest = state.dest

        if (cur == dest) {
            val newDest = GridPos(Random.nextInt(gridSize), Random.nextInt(gridSize))
            state = state.copy(dest = newDest, status = VehicleStatus.MOVING)
            return
        }

        val next = nextStep(cur, dest)
        val moved = engine.tryMove(
            vehicleId = id,
            from = cur,
            to = next,
            isAmbulance = (type == VehicleType.AMBULANCE)
        )

        state = if (moved) {
            state.copy(
                pos = next,
                status = VehicleStatus.MOVING,
                movedCells = state.movedCells + 1
            )
        } else {
            state.copy(
                status = VehicleStatus.STOPPED,
                waitMs = state.waitMs + 60L
            )
        }
    }

    private fun nextStep(cur: GridPos, dest: GridPos): GridPos {
        val dx = dest.x - cur.x
        val dy = dest.y - cur.y
        return when {
            dx != 0 -> GridPos(cur.x + dx.coerceIn(-1, 1), cur.y)
            dy != 0 -> GridPos(cur.x, cur.y + dy.coerceIn(-1, 1))
            else -> cur
        }
    }
}
