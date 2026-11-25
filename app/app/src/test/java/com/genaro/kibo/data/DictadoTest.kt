package com.genaro.kibo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DictadoTest {

    @Test
    fun varios_alimentos() {
        val r = Dictado.interpretar("Compré dos litros de leche, una docena de huevos y 3 jitomates")
        assertEquals(3, r.size)
        assertEquals(2.0, r[0].cantidad, 0.001); assertEquals("L", r[0].unidad)
        assertEquals("leche", r[0].consulta)
        assertEquals(12.0, r[1].cantidad, 0.001); assertEquals("pza", r[1].unidad)
        assertEquals("huevos", r[1].consulta)
        assertEquals(3.0, r[2].cantidad, 0.001); assertEquals("jitomates", r[2].consulta)
    }

    @Test
    fun fracciones() {
        val r = Dictado.interpretar("medio kilo de queso panela y un cuarto de kilo de jamón")
        assertEquals(2, r.size)
        assertEquals(0.5, r[0].cantidad, 0.001); assertEquals("kg", r[0].unidad)
        assertEquals("queso panela", r[0].consulta)
        assertEquals(0.25, r[1].cantidad, 0.001); assertEquals("kg", r[1].unidad)
        assertEquals("jamon", r[1].consulta)
    }

    @Test
    fun decimal_con_coma() {
        val r = Dictado.interpretar("1,5 kilos de papa")
        assertEquals(1.5, r[0].cantidad, 0.001); assertEquals("papa", r[0].consulta)
    }

    @Test
    fun sin_cantidad() {
        val r = Dictado.interpretar("tomate")
        assertEquals(1.0, r[0].cantidad, 0.001); assertEquals("pza", r[0].unidad)
    }
}