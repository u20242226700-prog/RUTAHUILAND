package co.edu.usco.rutahuila

import co.edu.usco.rutahuila.data.Municipalities
import co.edu.usco.rutahuila.data.PlaceGuides
import co.edu.usco.rutahuila.data.PlacesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de los datos iniciales: revisan que el catálogo no tenga errores. */
class CatalogTest {

    // Rectángulo aproximado que encierra al departamento del Huila
    private val latitudes = 1.4..3.9
    private val longitudes = -76.7..-74.4

    @Test
    fun losIdsDeLosLugaresNoSeRepiten() {
        val ids = PlacesCatalog.places.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun todosLosLugaresTienenNombreYDescripcion() {
        PlacesCatalog.places.forEach { place ->
            assertTrue(place.name.isNotBlank())
            assertTrue(place.description.isNotBlank())
        }
    }

    @Test
    fun lasCoordenadasDeLosLugaresEstanDentroDelHuila() {
        PlacesCatalog.places.forEach { place ->
            assertTrue("Latitud de ${place.name}", place.latitude in latitudes)
            assertTrue("Longitud de ${place.name}", place.longitude in longitudes)
        }
    }

    @Test
    fun lasCoordenadasDeLosMunicipiosEstanDentroDelHuila() {
        Municipalities.all.forEach { municipality ->
            assertTrue("Latitud de ${municipality.name}", municipality.latitude in latitudes)
            assertTrue("Longitud de ${municipality.name}", municipality.longitude in longitudes)
        }
    }

    @Test
    fun cadaLugarDelCatalogoTieneSuFichaDeVisita() {
        PlacesCatalog.places.forEach { place ->
            assertNotNull("Falta la ficha de ${place.name}", PlaceGuides.forPlace(place.id))
        }
    }
}
