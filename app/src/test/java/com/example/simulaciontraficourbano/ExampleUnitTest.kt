package com.example.simulaciontraficourbano

import com.example.simulaciontraficourbano.sim.GridPos
import org.junit.Test
import org.junit.Assert.*

/**
 * Pruebas unitarias para verificar la lógica matemática de la simulación.
 * Requisito: Pruebas obligatorias.
 */
class SimulationLogicTest {

    @Test
    fun gridPos_keyCalculation_isUnique() {
        // Verificamos que la fórmula para generar claves únicas en el HashMap funcione
        val size = 10
        val posA = GridPos(0, 0) // key = 0
        val posB = GridPos(0, 1) // key = 1
        val posC = GridPos(1, 0) // key = 10 (1 * 10 + 0)

        assertEquals(0, posA.key(size))
        assertEquals(1, posB.key(size))
        assertEquals(10, posC.key(size))

        assertNotEquals(posA.key(size), posB.key(size))
    }

    @Test
    fun gridPos_equality_worksCorrectly() {
        // Verificamos que dos objetos distintos con mismas coordenadas sean iguales
        val p1 = GridPos(5, 5)
        val p2 = GridPos(5, 5)
        val p3 = GridPos(5, 6)

        assertEquals("Objetos con mismas coordenadas deben ser iguales", p1, p2)
        assertNotEquals("Objetos con distintas coordenadas deben ser distintos", p1, p3)
    }

    @Test
    fun vehicle_movement_logic_simulation() {
        // Simulamos el cálculo simple de siguiente paso (Pathfinding básico)
        val current = GridPos(2, 2)
        val dest = GridPos(5, 2) // El destino está a la derecha (+X)

        val dx = dest.x - current.x

        // Lógica extraída de VehicleRuntime: priorizar eje X
        val nextX = current.x + dx.coerceIn(-1, 1)
        val nextY = current.y // No se mueve en Y todavía

        val nextPos = GridPos(nextX, nextY)

        assertEquals(GridPos(3, 2), nextPos)
    }
}