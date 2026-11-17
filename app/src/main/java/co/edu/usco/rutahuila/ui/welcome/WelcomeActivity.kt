package co.edu.usco.rutahuila.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PlaceFilter
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.ActivityWelcomeBinding
import co.edu.usco.rutahuila.ui.main.MainActivity
import co.edu.usco.rutahuila.ui.onboarding.OnboardingActivity
import co.edu.usco.rutahuila.util.IntentKeys
import com.google.android.material.chip.Chip


class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Debe ir ANTES de super.onCreate
        super.onCreate(savedInstanceState)

        // La primera vez que se abre la app se muestra el tutorial
        if (!PlaceRepository.isOnboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restoreSavedUser()

        binding.etName.doAfterTextChanged { binding.tilName.error = null }

        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                startExploring()
                true
            } else {
                false
            }
        }

        binding.btnStart.setOnClickListener { startExploring() }
    }

    private fun restoreSavedUser() {
        val savedName = PlaceRepository.userName
        if (savedName.isNotBlank()) {
            binding.etName.setText(savedName)
            binding.tvCardTitle.text = getString(R.string.welcome_back_title, savedName)
            binding.btnStart.setText(R.string.welcome_button_continue)
        }

        val savedAvatar = PlaceRepository.avatar
        binding.chipGroupAvatar.children
            .filterIsInstance<Chip>()
            .firstOrNull { it.text.toString() == savedAvatar }
            ?.isChecked = true
    }

    private fun startExploring() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()

        // Validación del formulario
        if (name.length < MIN_NAME_LENGTH) {
            binding.tilName.error = getString(R.string.welcome_name_error)
            binding.etName.requestFocus()
            return
        }

        PlaceRepository.userName = name
        PlaceRepository.avatar = selectedAvatar()

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(IntentKeys.USER_NAME, name)
            putExtra(IntentKeys.FILTER, selectedFilter().name)
        }
        startActivity(intent)
        finish() // Al presionar "atrás" en la app principal, se sale (no vuelve al login)
    }

    private fun selectedAvatar(): String {
        val chip = binding.chipGroupAvatar.findViewById<Chip>(binding.chipGroupAvatar.checkedChipId)
        return chip?.text?.toString() ?: PlaceRepository.DEFAULT_AVATAR
    }

    private fun selectedFilter(): PlaceFilter = when (binding.chipGroupInterest.checkedChipId) {
        R.id.chipAdventure -> PlaceFilter.ADVENTURE
        R.id.chipCulture -> PlaceFilter.CULTURE
        R.id.chipNature -> PlaceFilter.NATURE
        else -> PlaceFilter.ALL
    }

    private companion object {
        const val MIN_NAME_LENGTH = 2
    }
}
