package co.edu.usco.rutahuila.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.ActivityOnboardingBinding
import co.edu.usco.rutahuila.ui.welcome.WelcomeActivity

/**
 * TUTORIAL INICIAL.
 * Tres páginas deslizables que explican la app. Solo se muestra la primera
 * vez: al terminar se guarda una marca en SharedPreferences.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val pages = listOf(
        OnboardingPage("🌄", R.string.onb_1_title, R.string.onb_1_text),
        OnboardingPage("🗺️", R.string.onb_2_title, R.string.onb_2_text),
        OnboardingPage("📷", R.string.onb_3_title, R.string.onb_3_text)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingAdapter(pages)

        // Cada vez que el usuario cambia de página se actualizan los puntos y el botón
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateControls(position)
        })
        updateControls(0)

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current == pages.lastIndex) {
                finishOnboarding()
            } else {
                binding.viewPager.currentItem = current + 1
            }
        }
    }

    private fun updateControls(position: Int) {
        // Puntos: "● ○ ○" indica que estamos en la primera página
        binding.tvDots.text = pages.indices.joinToString(" ") { index ->
            if (index == position) DOT_SELECTED else DOT_EMPTY
        }
        binding.btnNext.setText(
            if (position == pages.lastIndex) R.string.onboarding_start else R.string.onboarding_next
        )
    }

    private fun finishOnboarding() {
        PlaceRepository.isOnboardingDone = true
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    private companion object {
        const val DOT_SELECTED = "●"
        const val DOT_EMPTY = "○"
    }
}
