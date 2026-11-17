package co.edu.usco.rutahuila.ui.profile

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.Achievements
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.DialogEditNameBinding
import co.edu.usco.rutahuila.databinding.FragmentProfileBinding
import co.edu.usco.rutahuila.ui.welcome.WelcomeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * PESTAÑA PERFIL: estadísticas, nivel de explorador, logros y ajustes
 * (modo oscuro, editar nombre, cerrar sesión y borrar datos).
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val achievementAdapter = AchievementAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAchievements.layoutManager = GridLayoutManager(requireContext(), ACHIEVEMENT_COLUMNS)
        binding.rvAchievements.adapter = achievementAdapter
        binding.rvAchievements.isNestedScrollingEnabled = false

        setupSettings()
    }

    private fun setupSettings() {
        binding.switchDarkMode.isChecked = PlaceRepository.isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == PlaceRepository.isDarkMode) return@setOnCheckedChangeListener
            PlaceRepository.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.btnEditName.setOnClickListener { showEditNameDialog() }
        binding.btnPermissions.setOnClickListener { openAppSettings() }
        binding.btnLogout.setOnClickListener { goToWelcome() }
        binding.btnResetData.setOnClickListener { confirmReset() }
    }

    private fun refresh() {
        val b = _binding ?: return
        val repo = PlaceRepository

        b.tvAvatar.text = repo.avatar
        b.tvUserName.text = repo.userName

        // Nivel según los lugares visitados
        val visited = repo.visitedCount()
        val total = repo.totalPlaces()
        b.tvLevel.setText(levelFor(visited, total))
        b.progressLevel.max = total
        b.progressLevel.setProgressCompat(visited, true)
        b.tvLevelProgress.text = getString(R.string.profile_level_progress, visited, total)

        // Estadísticas
        b.tvStatVisited.text = visited.toString()
        b.tvStatFavorites.text = repo.favoritesCount().toString()
        b.tvStatTrip.text = repo.tripIds().size.toString()
        val average = repo.averageRating()
        b.tvStatRating.text = if (average > 0f) getString(R.string.profile_rating_format, average) else "—"

        // Logros
        val achievements = Achievements.evaluate()
        achievementAdapter.submitList(achievements)
        b.tvAchievementsTitle.text = getString(
            R.string.profile_achievements_title,
            achievements.count { it.unlocked },
            achievements.size
        )
    }

    private fun levelFor(visited: Int, total: Int): Int = when {
        visited >= total -> R.string.level_4
        visited >= LEVEL_3_MIN -> R.string.level_3
        visited >= LEVEL_2_MIN -> R.string.level_2
        else -> R.string.level_1
    }

    private fun showEditNameDialog() {
        val dialogBinding = DialogEditNameBinding.inflate(layoutInflater)
        dialogBinding.etName.setText(PlaceRepository.userName)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_edit_name)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null) // Se configura abajo para validar
            .show()

        // Se reemplaza el clic para que el diálogo NO se cierre si el nombre es inválido
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim().orEmpty()
            if (name.length < MIN_NAME_LENGTH) {
                dialogBinding.tilName.error = getString(R.string.welcome_name_error)
            } else {
                PlaceRepository.userName = name
                refresh()
                dialog.dismiss()
            }
        }
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_reset_title)
            .setMessage(R.string.profile_reset_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                PlaceRepository.clearAll()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                goToWelcome()
            }
            .show()
    }

    /** Abre los ajustes del sistema, donde el usuario puede cambiar los permisos. */
    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    /** Vuelve a la bienvenida y cierra todas las pantallas anteriores. */
    private fun goToWelcome() {
        val intent = Intent(requireContext(), WelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ACHIEVEMENT_COLUMNS = 2
        const val MIN_NAME_LENGTH = 2
        const val LEVEL_2_MIN = 3
        const val LEVEL_3_MIN = 7
    }
}
