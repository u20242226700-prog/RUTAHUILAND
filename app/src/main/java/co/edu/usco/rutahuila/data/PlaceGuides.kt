package co.edu.usco.rutahuila.data

import androidx.annotation.StringRes
import co.edu.usco.rutahuila.R

/** Qué tan exigente es la visita. */
enum class Difficulty(@StringRes val labelRes: Int) {
    EASY(R.string.difficulty_easy),
    MEDIUM(R.string.difficulty_medium),
    HARD(R.string.difficulty_hard)
}

/** Recomendaciones para planear la visita a un lugar. */
data class PlaceGuide(
    val duration: String,
    val difficulty: Difficulty,
    val bestTime: String,
    val whatToBring: String
)


object PlaceGuides {

    private val guides: Map<Int, PlaceGuide> = mapOf(
        1 to PlaceGuide(
            "Medio día a 1 día", Difficulty.EASY,
            "Temprano en la mañana o al atardecer",
            "Agua, gorra, protector solar y zapatos cerrados"
        ),
        2 to PlaceGuide(
            "2 horas, de noche", Difficulty.EASY,
            "Noches despejadas y sin luna llena",
            "Chaqueta ligera, repelente y linterna"
        ),
        3 to PlaceGuide(
            "3 a 4 horas", Difficulty.EASY,
            "En la mañana y con clima seco",
            "Zapatos cómodos, impermeable y agua"
        ),
        4 to PlaceGuide(
            "2 horas", Difficulty.EASY,
            "En la mañana",
            "Zapatos cómodos, gorra y agua"
        ),
        5 to PlaceGuide(
            "1 hora", Difficulty.EASY,
            "Días sin lluvia",
            "Zapatos con buen agarre"
        ),
        6 to PlaceGuide(
            "1 a 2 horas", Difficulty.MEDIUM,
            "Días despejados, para ver la cascada completa",
            "Chaqueta impermeable y zapatos con buen agarre"
        ),
        7 to PlaceGuide(
            "2 a 3 horas", Difficulty.HARD,
            "Cuando el operador confirme que el río está apto",
            "Ropa que se pueda mojar, muda de ropa y protector solar"
        ),
        8 to PlaceGuide(
            "Medio día", Difficulty.EASY,
            "Días soleados",
            "Traje de baño, protector solar y gorra"
        ),
        9 to PlaceGuide(
            "2 a 3 horas", Difficulty.EASY,
            "En la tarde o en la noche, cuando baja el calor",
            "Traje de baño, toalla y sandalias"
        ),
        10 to PlaceGuide(
            "1 a 2 días", Difficulty.HARD,
            "Temporada seca",
            "Botas, linterna, impermeable y abrigo"
        ),
        11 to PlaceGuide(
            "1 a 2 horas", Difficulty.EASY,
            "Al atardecer",
            "Ropa fresca y agua"
        ),
        12 to PlaceGuide(
            "30 minutos", Difficulty.EASY,
            "Al final de la tarde",
            "Cámara y ropa fresca"
        ),
        13 to PlaceGuide(
            "2 a 3 horas", Difficulty.MEDIUM,
            "Días sin lluvia",
            "Zapatos con buen agarre, traje de baño y agua"
        )
    )

    /** Devuelve la ficha del lugar, o null si no tiene. */
    fun forPlace(placeId: Int): PlaceGuide? = guides[placeId]
}
