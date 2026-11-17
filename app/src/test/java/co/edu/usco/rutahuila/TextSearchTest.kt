package co.edu.usco.rutahuila

import co.edu.usco.rutahuila.data.Municipalities
import co.edu.usco.rutahuila.util.normalized
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class TextSearchTest {

    @Test
    fun normalized_quitaLasTildes() {
        assertEquals("san agustin", "San Agustín".normalized())
    }

    @Test
    fun normalized_pasaAMinusculasYQuitaEspaciosDeLosLados() {
        assertEquals("neiva", "  NEIVA ".normalized())
    }

    @Test
    fun buscarSinTilde_encuentraElTextoConTilde() {
        val nombre = "Parque Arqueológico San Agustín".normalized()
        assertTrue(nombre.contains("arqueologico".normalized()))
    }

    @Test
    fun municipio_seEncuentraSinImportarTildesNiMayusculas() {
        val municipio = Municipalities.find("garzon")
        assertNotNull(municipio)
        assertEquals("Garzón", municipio?.name)
    }

    @Test
    fun municipio_queNoExisteDevuelveNull() {
        assertNull(Municipalities.find("Bogotá"))
    }
}
