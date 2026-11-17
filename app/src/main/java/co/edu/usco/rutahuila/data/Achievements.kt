package co.edu.usco.rutahuila.data

import androidx.annotation.StringRes
import co.edu.usco.rutahuila.R

/** Un logro que el usuario desbloquea al usar la app (gamificación). */
data class Achievement(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val unlocked: Boolean
)

object Achievements {

    private const val ID_TATACOA = 1
    private const val ID_OBSERVATORY = 2
    private const val ID_SAN_AGUSTIN = 3
    private const val ID_IDOLOS = 4

    /** Calcula qué logros están desbloqueados según los datos actuales. */
    fun evaluate(): List<Achievement> {
        val repo = PlaceRepository
        val visitedAll = repo.visitedCount() == repo.totalPlaces()

        return listOf(
            Achievement(
                "⭐", R.string.ach_first_fav_title, R.string.ach_first_fav_desc,
                repo.favoritesCount() >= 1
            ),
            Achievement(
                "🧭", R.string.ach_explorer_title, R.string.ach_explorer_desc,
                repo.visitedCount() >= 3
            ),
            Achievement(
                "📝", R.string.ach_critic_title, R.string.ach_critic_desc,
                repo.ratedCount() >= 5
            ),
            Achievement(
                "🗺️", R.string.ach_planner_title, R.string.ach_planner_desc,
                repo.tripIds().size >= 4
            ),
            Achievement(
                "📷", R.string.ach_photo_title, R.string.ach_photo_desc,
                repo.photosCount() >= 3
            ),
            Achievement(
                "🗿", R.string.ach_archaeologist_title, R.string.ach_archaeologist_desc,
                repo.isVisited(ID_SAN_AGUSTIN) && repo.isVisited(ID_IDOLOS)
            ),
            Achievement(
                "🌌", R.string.ach_stars_title, R.string.ach_stars_desc,
                repo.isVisited(ID_TATACOA) && repo.isVisited(ID_OBSERVATORY)
            ),
            Achievement(
                "🏆", R.string.ach_huilense_title, R.string.ach_huilense_desc,
                visitedAll
            )
        )
    }
}
