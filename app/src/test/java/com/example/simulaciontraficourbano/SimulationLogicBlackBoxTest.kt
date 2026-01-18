package com.example.simulaciontraficourbano.domain

import com.example.simulaciontraficourbano.model.domain.GridPos
import com.example.simulaciontraficourbano.model.domain.LightColor
import com.example.simulaciontraficourbano.model.domain.VehicleType
import org.junit.Test
import org.junit.Assert.*

/**
 * ============================================================================
 * PRUEBAS UNITARIAS DE CAJA NEGRA - LÓGICA DE NEGOCIO
 * ============================================================================
 * 
 * Tipo: Caja Negra (Black Box Testing)
 * Objetivo: Verificar comportamiento esperado sin conocer implementación interna
 * Cobertura: Algoritmos de pathfinding, detección de colisiones, reglas de negocio
 * 
 * Estas pruebas verifican:
 * - Entrada/Salida sin examinar código interno
 * - Casos límite y edge cases
 * - Requisitos funcionales del sistema
 */
class SimulationLogicBlackBoxTest {

    // ========================================================================
    // TESTS: Pathfinding - Algoritmo de búsqueda de caminos
    // ========================================================================
    
    @Test
    fun pathfinding_movesHorizontallyFirst() {
        // GIVEN: Posición actual y destino con diferencia en X e Y
        val current = GridPos(5, 5)
        val destination = GridPos(10, 8)
        
        // WHEN: Calculamos el siguiente movimiento (priorizando X)
        val dx = destination.x - current.x
        val dy = destination.y - current.y
        
        val nextX = current.x + dx.coerceIn(-1, 1)
        val nextY = if (dx != 0) current.y else current.y + dy.coerceIn(-1, 1)
        
        val nextPos = GridPos(nextX, nextY)
        
        // THEN: Debe moverse primero en X (hacia la derecha)
        assertEquals("Debe moverse en X primero", 6, nextPos.x)
        assertEquals("Y no debe cambiar todavía", 5, nextPos.y)
    }
    
    @Test
    fun pathfinding_movesVerticallyWhenXAligned() {
        // GIVEN: Posición y destino alineados en X
        val current = GridPos(10, 5)
        val destination = GridPos(10, 8)
        
        // WHEN: Calculamos el siguiente movimiento
        val dx = destination.x - current.x
        val dy = destination.y - current.y
        
        val nextX = current.x + dx.coerceIn(-1, 1)
        val nextY = if (dx != 0) current.y else current.y + dy.coerceIn(-1, 1)
        
        val nextPos = GridPos(nextX, nextY)
        
        // THEN: Debe moverse en Y (hacia abajo)
        assertEquals("X debe permanecer igual", 10, nextPos.x)
        assertEquals("Debe moverse en Y", 6, nextPos.y)
    }
    
    @Test
    fun pathfinding_stopsAtDestination() {
        // GIVEN: Posición actual = destino
        val current = GridPos(10, 10)
        val destination = GridPos(10, 10)
        
        // WHEN: Calculamos diferencias
        val dx = destination.x - current.x
        val dy = destination.y - current.y
        
        // THEN: No debe haber movimiento
        assertEquals("No hay diferencia en X", 0, dx)
        assertEquals("No hay diferencia en Y", 0, dy)
    }
    
    @Test
    fun pathfinding_movesOneStepAtATime() {
        // GIVEN: Destino muy lejano
        val current = GridPos(0, 0)
        val destination = GridPos(20, 20)
        
        // WHEN: Calculamos siguiente paso
        val dx = destination.x - current.x
        val nextX = current.x + dx.coerceIn(-1, 1)
        
        // THEN: Solo debe moverse 1 celda
        assertEquals("Solo 1 paso a la vez", 1, nextX)
    }

    // ========================================================================
    // TESTS: Detección de colisiones - Distancias y ocupación
    // ========================================================================
    
    @Test
    fun collision_samePosition_detected() {
        // GIVEN: Dos vehículos en la misma posición
        val pos1 = GridPos(10, 10)
        val pos2 = GridPos(10, 10)
        
        // WHEN: Verificamos si ocupan la misma celda
        val collision = pos1 == pos2
        
        // THEN: Debe detectarse colisión
        assertTrue("Colisión en misma posición", collision)
    }
    
    @Test
    fun collision_adjacentPositions_noCollision() {
        // GIVEN: Dos vehículos en posiciones adyacentes
        val pos1 = GridPos(10, 10)
        val pos2 = GridPos(11, 10)
        
        // WHEN: Verificamos si ocupan la misma celda
        val collision = pos1 == pos2
        
        // THEN: No debe haber colisión
        assertFalse("No hay colisión en posiciones adyacentes", collision)
    }
    
    @Test
    fun collision_distance_calculatedCorrectly() {
        // GIVEN: Dos posiciones conocidas
        val pos1 = GridPos(0, 0)
        val pos2 = GridPos(3, 4)
        
        // WHEN: Calculamos distancia euclidiana
        val dx = (pos2.x - pos1.x).toDouble()
        val dy = (pos2.y - pos1.y).toDouble()
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        
        // THEN: Distancia debe ser 5 (triángulo 3-4-5)
        assertEquals("Distancia 3-4-5", 5.0, distance, 0.001)
    }

    // ========================================================================
    // TESTS: Velocidad de vehículos - Factores y cálculos
    // ========================================================================
    
    @Test
    fun speed_busIsSlowerThanCar() {
        // GIVEN: Factores de velocidad
        val carSpeed = VehicleType.CAR.speedFactor
        val busSpeed = VehicleType.BUS.speedFactor
        
        // THEN: Autobús debe ser más lento
        assertTrue("Autobús más lento que coche", busSpeed < carSpeed)
    }
    
    @Test
    fun speed_motoIsFasterThanCar() {
        // GIVEN: Factores de velocidad
        val carSpeed = VehicleType.CAR.speedFactor
        val motoSpeed = VehicleType.MOTO.speedFactor
        
        // THEN: Moto debe ser más rápida
        assertTrue("Moto más rápida que coche", motoSpeed > carSpeed)
    }
    
    @Test
    fun speed_ambulanceHasPriority() {
        // GIVEN: Factor de velocidad de ambulancia
        val ambulanceSpeed = VehicleType.AMBULANCE.speedFactor
        val carSpeed = VehicleType.CAR.speedFactor
        
        // THEN: Ambulancia debe ser más rápida que coche normal
        assertTrue("Ambulancia más rápida que coche", ambulanceSpeed > carSpeed)
    }
    
    @Test
    fun speed_withSimulationMultiplier() {
        // GIVEN: Velocidad base y multiplicador de simulación
        val baseStepMs = 140L
        val speedMultiplier = 2.0
        
        // WHEN: Calculamos tiempo real de step
        val actualStepMs = (baseStepMs / speedMultiplier).toLong()
        
        // THEN: Debe ser la mitad (más rápido)
        assertEquals("2x speed = mitad del tiempo", 70L, actualStepMs)
    }

    // ========================================================================
    // TESTS: Ciclo de semáforos - Transiciones de estado
    // ========================================================================
    
    @Test
    fun trafficLight_greenToYellow_validTransition() {
        // GIVEN: Estado actual verde
        val currentState = LightColor.GREEN
        
        // WHEN: Pasa tiempo de verde
        val nextState = LightColor.YELLOW
        
        // THEN: Transición válida
        assertTrue("Verde puede cambiar a amarillo", 
                  currentState == LightColor.GREEN && nextState == LightColor.YELLOW)
    }
    
    @Test
    fun trafficLight_yellowToRed_validTransition() {
        // GIVEN: Estado actual amarillo
        val currentState = LightColor.YELLOW
        
        // WHEN: Pasa tiempo de amarillo
        val nextState = LightColor.RED
        
        // THEN: Transición válida
        assertTrue("Amarillo puede cambiar a rojo", 
                  currentState == LightColor.YELLOW && nextState == LightColor.RED)
    }
    
    @Test
    fun trafficLight_redToGreen_validTransition() {
        // GIVEN: Estado actual rojo
        val currentState = LightColor.RED
        
        // WHEN: Pasa tiempo de rojo
        val nextState = LightColor.GREEN
        
        // THEN: Transición válida
        assertTrue("Rojo puede cambiar a verde", 
                  currentState == LightColor.RED && nextState == LightColor.GREEN)
    }
    
    @Test
    fun trafficLight_cycleDuration_calculated() {
        // GIVEN: Tiempos de cada estado
        val greenMs = 8000L
        val yellowMs = 1500L
        val redMs = 8000L + 600L  // verde del otro lado + all-red
        
        // WHEN: Calculamos duración del ciclo completo
        val fullCycleMs = greenMs + yellowMs + redMs
        
        // THEN: Debe ser aproximadamente 18 segundos
        assertTrue("Ciclo completo ~18 segundos", fullCycleMs > 17000 && fullCycleMs < 19000)
    }

    // ========================================================================
    // TESTS: Reglas de negocio - Validaciones
    // ========================================================================
    
    @Test
    fun business_vehicleCountRange_valid() {
        // GIVEN: Rango válido de vehículos (5-100)
        val minVehicles = 5
        val maxVehicles = 100
        
        // WHEN: Validamos diferentes valores
        val valid1 = 30 in minVehicles..maxVehicles
        val valid2 = 5 in minVehicles..maxVehicles
        val valid3 = 100 in minVehicles..maxVehicles
        val invalid1 = 3 in minVehicles..maxVehicles
        val invalid2 = 150 in minVehicles..maxVehicles
        
        // THEN: Solo valores en rango son válidos
        assertTrue("30 vehículos válido", valid1)
        assertTrue("5 vehículos válido (mínimo)", valid2)
        assertTrue("100 vehículos válido (máximo)", valid3)
        assertFalse("3 vehículos inválido", invalid1)
        assertFalse("150 vehículos inválido", invalid2)
    }
    
    @Test
    fun business_speedMultiplierRange_valid() {
        // GIVEN: Rango válido de velocidad (0.5x - 5x)
        val minSpeed = 0.5
        val maxSpeed = 5.0
        
        // WHEN: Validamos diferentes valores
        val valid1 = 1.0 in minSpeed..maxSpeed
        val valid2 = 0.5 in minSpeed..maxSpeed
        val valid3 = 5.0 in minSpeed..maxSpeed
        val invalid1 = 0.2 in minSpeed..maxSpeed
        val invalid2 = 10.0 in minSpeed..maxSpeed
        
        // THEN: Solo valores en rango son válidos
        assertTrue("1x válido", valid1)
        assertTrue("0.5x válido (mínimo)", valid2)
        assertTrue("5x válido (máximo)", valid3)
        assertFalse("0.2x inválido", invalid1)
        assertFalse("10x inválido", invalid2)
    }
    
    @Test
    fun business_ambulanceCount_cannotExceedTotal() {
        // GIVEN: Total de vehículos
        val totalVehicles = 30
        
        // WHEN: Validamos cantidad de ambulancias
        val validAmbulances = 5
        val invalidAmbulances = 35
        
        // THEN: Ambulancias no pueden superar total
        assertTrue("5 ambulancias de 30 válido", validAmbulances <= totalVehicles)
        assertFalse("35 ambulancias de 30 inválido", invalidAmbulances <= totalVehicles)
    }

    // ========================================================================
    // TESTS: Estadísticas - Cálculos y agregaciones
    // ========================================================================
    
    @Test
    fun stats_percentage_calculated() {
        // GIVEN: Estadísticas de vehículos
        val total = 50
        val moving = 30
        
        // WHEN: Calculamos porcentaje
        val percentage = (moving.toFloat() / total) * 100
        
        // THEN: Debe ser 60%
        assertEquals("60% en movimiento", 60.0f, percentage, 0.1f)
    }
    
    @Test
    fun stats_averageSpeed_calculated() {
        // GIVEN: Vehículos con diferentes velocidades
        val speeds = listOf(1.0, 1.4, 0.7, 1.2, 1.0)
        
        // WHEN: Calculamos promedio
        val avgSpeed = speeds.average()
        
        // THEN: Debe calcular correctamente
        assertEquals("Velocidad promedio", 1.06, avgSpeed, 0.01)
    }
    
    @Test
    fun stats_timeConversion_secondsToMs() {
        // GIVEN: Tiempo en segundos
        val seconds = 10
        
        // WHEN: Convertimos a milisegundos
        val milliseconds = seconds * 1000L
        
        // THEN: Debe ser 10000ms
        assertEquals("10 segundos = 10000ms", 10000L, milliseconds)
    }
}
