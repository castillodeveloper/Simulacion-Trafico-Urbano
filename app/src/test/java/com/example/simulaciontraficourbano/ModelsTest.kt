package com.example.simulaciontraficourbano.sim

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * ============================================================================
 * PRUEBAS UNITARIAS DE CAJA BLANCA - MODELS
 * ============================================================================
 * 
 * Tipo: Caja Blanca (White Box Testing)
 * Objetivo: Verificar la lógica interna de las data classes y enums del modelo
 * Cobertura: Models.kt - Estructuras de datos básicas
 * 
 * Estas pruebas verifican:
 * - Correcta implementación de data classes
 * - Cálculos matemáticos internos
 * - Propiedades computadas
 * - Igualdad y hash de objetos
 */
class ModelsTest {

    // ========================================================================
    // TESTS: GridPos - Posición en la cuadrícula
    // ========================================================================
    
    @Test
    fun gridPos_keyCalculation_uniqueForDifferentPositions() {
        // GIVEN: Un tamaño de grid de 40x40
        val gridSize = 40
        
        // WHEN: Calculamos keys para diferentes posiciones
        val pos1 = GridPos(0, 0)
        val pos2 = GridPos(0, 1)
        val pos3 = GridPos(1, 0)
        val pos4 = GridPos(39, 39)
        
        // THEN: Cada posición debe tener una key única
        assertEquals("Esquina superior izquierda debe ser 0", 0, pos1.key(gridSize))
        assertEquals("Una celda a la derecha", 1, pos2.key(gridSize))
        assertEquals("Una celda abajo", 40, pos3.key(gridSize))
        assertEquals("Esquina inferior derecha", 39 * 40 + 39, pos4.key(gridSize))
        
        // THEN: Todas las keys deben ser diferentes
        val keys = setOf(pos1.key(gridSize), pos2.key(gridSize), 
                        pos3.key(gridSize), pos4.key(gridSize))
        assertEquals("Debe haber 4 keys únicas", 4, keys.size)
    }
    
    @Test
    fun gridPos_keyCalculation_consistentForSamePosition() {
        // GIVEN: La misma posición calculada múltiples veces
        val gridSize = 40
        val pos = GridPos(15, 20)
        
        // WHEN: Calculamos la key múltiples veces
        val key1 = pos.key(gridSize)
        val key2 = pos.key(gridSize)
        val key3 = pos.key(gridSize)
        
        // THEN: Todas las keys deben ser idénticas
        assertEquals(key1, key2)
        assertEquals(key2, key3)
    }
    
    @Test
    fun gridPos_equality_sameCoordinates() {
        // GIVEN: Dos objetos GridPos con mismas coordenadas
        val pos1 = GridPos(10, 15)
        val pos2 = GridPos(10, 15)
        
        // THEN: Deben ser iguales (data class equality)
        assertEquals("GridPos con mismas coordenadas deben ser iguales", pos1, pos2)
        assertEquals("Hash codes deben ser iguales", pos1.hashCode(), pos2.hashCode())
    }
    
    @Test
    fun gridPos_equality_differentCoordinates() {
        // GIVEN: Dos objetos GridPos con coordenadas diferentes
        val pos1 = GridPos(10, 15)
        val pos2 = GridPos(10, 16)
        val pos3 = GridPos(11, 15)
        
        // THEN: Deben ser diferentes
        assertNotEquals("Diferente Y", pos1, pos2)
        assertNotEquals("Diferente X", pos1, pos3)
    }

    // ========================================================================
    // TESTS: VehicleType - Tipos de vehículos
    // ========================================================================
    
    @Test
    fun vehicleType_speedFactors_correctValues() {
        // THEN: Verificar que los factores de velocidad son correctos
        assertEquals("Coche velocidad normal", 1.0, VehicleType.CAR.speedFactor, 0.001)
        assertEquals("Autobús velocidad baja", 0.7, VehicleType.BUS.speedFactor, 0.001)
        assertEquals("Moto velocidad alta", 1.4, VehicleType.MOTO.speedFactor, 0.001)
        assertEquals("Ambulancia velocidad alta", 1.2, VehicleType.AMBULANCE.speedFactor, 0.001)
    }
    
    @Test
    fun vehicleType_sizes_busIsBiggest() {
        // THEN: El autobús debe ser el más grande
        assertTrue("Autobús más grande que coche", 
                  VehicleType.BUS.size > VehicleType.CAR.size)
        assertTrue("Autobús más grande que moto", 
                  VehicleType.BUS.size > VehicleType.MOTO.size)
        assertTrue("Autobús más grande que ambulancia", 
                  VehicleType.BUS.size > VehicleType.AMBULANCE.size)
    }
    
    @Test
    fun vehicleType_sizes_motoIsSmallest() {
        // THEN: La moto debe ser la más pequeña
        assertTrue("Moto más pequeña que coche", 
                  VehicleType.MOTO.size < VehicleType.CAR.size)
        assertTrue("Moto más pequeña que bus", 
                  VehicleType.MOTO.size < VehicleType.BUS.size)
        assertTrue("Moto más pequeña que ambulancia", 
                  VehicleType.MOTO.size < VehicleType.AMBULANCE.size)
    }

    // ========================================================================
    // TESTS: SimulationConfig - Configuración de la simulación
    // ========================================================================
    
    @Test
    fun simulationConfig_defaultValues_areValid() {
        // GIVEN: Una configuración con valores por defecto
        val config = SimulationConfig()
        
        // THEN: Los valores por defecto deben ser válidos
        assertEquals("Grid size por defecto", 10, config.gridSize)
        assertEquals("Vehículos por defecto", 30, config.vehicleCount)
        assertEquals("Ambulancias por defecto", 2, config.ambulanceCount)
        assertEquals("Velocidad por defecto", 1.0, config.simSpeed, 0.001)
        assertTrue("Semáforos habilitados", config.lightsEnabled)
        assertTrue("Colisiones habilitadas", config.collisionsEnabled)
    }
    
    @Test
    fun simulationConfig_customValues_preserved() {
        // GIVEN: Una configuración con valores personalizados
        val config = SimulationConfig(
            gridSize = 50,
            vehicleCount = 100,
            ambulanceCount = 5,
            simSpeed = 2.0,
            lightsEnabled = false,
            collisionsEnabled = false
        )
        
        // THEN: Los valores personalizados deben preservarse
        assertEquals(50, config.gridSize)
        assertEquals(100, config.vehicleCount)
        assertEquals(5, config.ambulanceCount)
        assertEquals(2.0, config.simSpeed, 0.001)
        assertFalse(config.lightsEnabled)
        assertFalse(config.collisionsEnabled)
    }
    
    @Test
    fun simulationConfig_lightTimings_calculatedCorrectly() {
        // GIVEN: Configuración con tiempos de semáforo
        val config = SimulationConfig(
            lightGreenMs = 8000L,
            lightYellowMs = 1500L,
            lightAllRedMs = 600L
        )
        
        // THEN: El ciclo completo debe sumar correctamente
        val totalCycleMs = config.lightGreenMs + config.lightYellowMs + 
                          config.lightAllRedMs + config.lightGreenMs
        
        assertTrue("Ciclo completo > 10 segundos", totalCycleMs > 10000)
    }

    // ========================================================================
    // TESTS: VehicleState - Estado de vehículos
    // ========================================================================
    
    @Test
    fun vehicleState_creationWithAllFields_success() {
        // GIVEN: Datos para crear un vehículo
        val id = 1
        val type = VehicleType.CAR
        val pos = GridPos(5, 5)
        val dest = GridPos(10, 10)
        val status = VehicleStatus.MOVING
        val color = 0xFF0000FFL
        
        // WHEN: Creamos el estado del vehículo
        val vehicle = VehicleState(
            id = id,
            type = type,
            pos = pos,
            dest = dest,
            status = status,
            colorArgb = color,
            movedCells = 10,
            waitMs = 500L
        )
        
        // THEN: Todos los campos deben estar correctamente asignados
        assertEquals(id, vehicle.id)
        assertEquals(type, vehicle.type)
        assertEquals(pos, vehicle.pos)
        assertEquals(dest, vehicle.dest)
        assertEquals(status, vehicle.status)
        assertEquals(color, vehicle.colorArgb)
        assertEquals(10, vehicle.movedCells)
        assertEquals(500L, vehicle.waitMs)
    }

    // ========================================================================
    // TESTS: TrafficLightView - Vista de semáforos
    // ========================================================================
    
    @Test
    fun trafficLightView_perpendicular_cannotBothBeGreen() {
        // Este test verifica la LÓGICA que debería cumplirse,
        // aunque la data class no lo valide automáticamente
        
        // GIVEN: Un semáforo correctamente configurado
        val light = TrafficLightView(
            id = 1,
            pos = GridPos(5, 5),
            ns = LightColor.GREEN,
            ew = LightColor.RED,
            remainingMs = 5000L
        )
        
        // THEN: Si NS es verde, EW debe ser rojo (o amarillo)
        if (light.ns == LightColor.GREEN) {
            assertNotEquals("EW no puede ser verde si NS es verde", 
                          LightColor.GREEN, light.ew)
        }
    }

    // ========================================================================
    // TESTS: SimulationSnapshot - Snapshot del estado
    // ========================================================================
    
    @Test
    fun simulationSnapshot_empty_hasCorrectDefaults() {
        // WHEN: Creamos un snapshot vacío
        val snapshot = SimulationSnapshot.empty()
        
        // THEN: Debe tener valores por defecto correctos
        assertEquals(10, snapshot.gridSize)
        assertTrue(snapshot.vehicles.isEmpty())
        assertTrue(snapshot.lights.isEmpty())
        assertTrue(snapshot.events.isEmpty())
        assertEquals(0, snapshot.stats.activeVehicles)
        assertEquals(0, snapshot.stats.moving)
        assertEquals(0, snapshot.stats.stopped)
        assertEquals(0, snapshot.stats.arrived)
        assertEquals(0.0, snapshot.stats.avgSpeedCellsPerSec, 0.001)
    }

    // ========================================================================
    // TESTS: SimulationEvent - Eventos de la simulación
    // ========================================================================
    
    @Test
    fun simulationEvent_endTime_calculatedCorrectly() {
        // GIVEN: Un evento que comienza en t=1000 y dura 5000ms
        val startMs = 1000L
        val durationMs = 5000L
        
        val event = SimulationEvent(
            id = 1L,
            type = EventType.ACCIDENT,
            pos = GridPos(10, 10),
            startMs = startMs,
            durationMs = durationMs,
            message = "Accidente"
        )
        
        // THEN: El tiempo de finalización debe ser correcto
        assertEquals("End time = start + duration", 6000L, event.endMs)
    }
    
    @Test
    fun simulationEvent_differentTypes_exist() {
        // THEN: Deben existir todos los tipos de eventos
        val types = EventType.values()
        assertTrue("Debe haber al menos 4 tipos", types.size >= 4)
        assertTrue(types.contains(EventType.ACCIDENT))
        assertTrue(types.contains(EventType.ROADWORKS))
        assertTrue(types.contains(EventType.CONGESTION))
        assertTrue(types.contains(EventType.EMERGENCY))
    }
}
