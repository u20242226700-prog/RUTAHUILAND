package co.edu.usco.rutahuila.data

import co.edu.usco.rutahuila.util.normalized

/** Un municipio del Huila con su ubicación aproximada. */
data class Municipality(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Int
)

/**
 * Municipios que el usuario puede elegir al crear un lugar propio.
 * De aquí salen las coordenadas (para el mapa y el clima) y la distancia
 * aproximada por carretera desde Neiva. Son valores aproximados.
 */
object Municipalities {

    val all: List<Municipality> = listOf(
        Municipality("Acevedo", 1.8053, -75.8886, 190),
        Municipality("Aipe", 3.2222, -75.2364, 35),
        Municipality("Campoalegre", 2.6867, -75.3258, 28),
        Municipality("Garzón", 2.1964, -75.6278, 92),
        Municipality("Gigante", 2.3867, -75.5456, 80),
        Municipality("Hobo", 2.5800, -75.4494, 52),
        Municipality("La Plata", 2.3856, -75.8914, 122),
        Municipality("Neiva", 2.9273, -75.2819, 0),
        Municipality("Paicol", 2.4497, -75.7739, 120),
        Municipality("Palermo", 2.8892, -75.4353, 18),
        Municipality("Pitalito", 1.8537, -76.0513, 188),
        Municipality("Rivera", 2.7772, -75.2583, 25),
        Municipality("Saladoblanco", 1.9939, -76.0431, 215),
        Municipality("San Agustín", 1.8828, -76.2683, 227),
        Municipality("San José de Isnos", 1.9294, -76.2156, 220),
        Municipality("Tello", 3.0669, -75.1386, 25),
        Municipality("Tesalia", 2.4856, -75.7286, 100),
        Municipality("Timaná", 1.9744, -75.9319, 170),
        Municipality("Villavieja", 3.2189, -75.2189, 38),
        Municipality("Yaguará", 2.6644, -75.5175, 50)
    )

    val names: List<String> get() = all.map { it.name }

    /** Busca un municipio por nombre, sin importar mayúsculas ni tildes. */
    fun find(name: String): Municipality? =
        all.firstOrNull { it.name.normalized() == name.normalized() }
}
