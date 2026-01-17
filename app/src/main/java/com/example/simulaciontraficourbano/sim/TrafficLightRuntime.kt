package com.example.simulaciontraficourbano.sim

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class TrafficLightRuntime(
    val id: Int,
    val pos: GridPos,
    private val greenMs: Long,
    private val yellowMs: Long,
    private val allRedMs: Long,
    private val scope: CoroutineScope
) {
    @Volatile private var ns: LightColor = LightColor.GREEN
    @Volatile private var ew: LightColor = LightColor.RED

    // Para UI: tiempo restante de la fase actual
    @Volatile private var phaseEndMs: Long = 0L

    // Prioridad (ambulancia)
    private val forceUntilMs = AtomicLong(0L)
    @Volatile private var forcedAxis: Axis? = null

    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch(Dispatchers.Default) {
            var axis = Axis.NS

            while (isActive) {
                val now = System.currentTimeMillis()
                val forced = forcedAxis
                val forceActive = (forced != null) && (now < forceUntilMs.get())
                val effectiveAxis = if (forceActive) forced!! else axis

                setGreen(effectiveAxis, greenMs)
                delay(greenMs)

                setYellow(effectiveAxis, yellowMs)
                delay(yellowMs)

                if (allRedMs > 0) {
                    setAllRed(allRedMs)
                    delay(allRedMs)
                }

                if (!forceActive) {
                    axis = if (axis == Axis.NS) Axis.EW else Axis.NS
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun requestPriority(axis: Axis, durationMs: Long = 2500L) {
        forcedAxis = axis
        forceUntilMs.set(System.currentTimeMillis() + durationMs)
    }

    fun isGreenForMove(from: GridPos, to: GridPos): Boolean {
        val dx = to.x - from.x
        val axis = if (dx != 0) Axis.EW else Axis.NS
        return if (axis == Axis.NS) ns == LightColor.GREEN else ew == LightColor.GREEN
    }

    fun toView(): TrafficLightView {
        val now = System.currentTimeMillis()
        val remaining = (phaseEndMs - now).coerceAtLeast(0L)
        return TrafficLightView(
            id = id,
            pos = pos,
            ns = ns,
            ew = ew,
            remainingMs = remaining
        )
    }

    private fun setGreen(axis: Axis, ms: Long) {
        phaseEndMs = System.currentTimeMillis() + ms
        if (axis == Axis.NS) {
            ns = LightColor.GREEN
            ew = LightColor.RED
        } else {
            ns = LightColor.RED
            ew = LightColor.GREEN
        }
    }

    private fun setYellow(axis: Axis, ms: Long) {
        phaseEndMs = System.currentTimeMillis() + ms
        if (axis == Axis.NS) {
            ns = LightColor.YELLOW
            ew = LightColor.RED
        } else {
            ns = LightColor.RED
            ew = LightColor.YELLOW
        }
    }

    private fun setAllRed(ms: Long) {
        phaseEndMs = System.currentTimeMillis() + ms
        ns = LightColor.RED
        ew = LightColor.RED
    }
}
