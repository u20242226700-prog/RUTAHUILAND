package co.edu.usco.rutahuila.ui.form

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.Category
import co.edu.usco.rutahuila.data.Municipalities
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.ActivityPlaceFormBinding
import co.edu.usco.rutahuila.util.IntentKeys
import com.google.android.material.chip.Chip

/**
 * FORMULARIO DE LUGAR PROPIO.
 * La misma pantalla sirve para CREAR (sin parámetros) y para EDITAR
 * (cuando recibe el ID de un lugar). Guarda en la base de datos.
 */
class PlaceFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceFormBinding

    /** Lugar que se está editando, o null si se está creando uno nuevo. */
    private var editingPlace: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.actMunicipality.setSimpleItems(Municipalities.names.toTypedArray())

        loadPlaceToEdit()
        setupValidationReset()
        binding.btnSave.setOnClickListener { save() }
    }

    /** Si llegó un ID, se llenan los campos con los datos actuales. */
    private fun loadPlaceToEdit() {
        val id = intent.getIntExtra(IntentKeys.PLACE_ID, NO_ID)
        if (id == NO_ID) return

        val place = PlaceRepository.getById(id)
        if (place == null || !place.isCustom) {
            finish()
            return
        }
        editingPlace = place

        binding.toolbar.setTitle(R.string.form_title_edit)
        binding.etName.setText(place.name)
        binding.actMunicipality.setText(place.municipality, false)
        binding.etDescription.setText(place.description)
        binding.chipGroupCategory.check(chipIdFor(place.category))
        binding.chipGroupEmoji.children
            .filterIsInstance<Chip>()
            .firstOrNull { it.text.toString() == place.emoji }
            ?.isChecked = true
    }

    /** Al corregir un campo, se borra su mensaje de error. */
    private fun setupValidationReset() {
        binding.etName.doAfterTextChanged { binding.tilName.error = null }
        binding.etDescription.doAfterTextChanged { binding.tilDescription.error = null }
        binding.actMunicipality.setOnItemClickListener { _, _, _, _ ->
            binding.tilMunicipality.error = null
        }
    }

    private fun save() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val municipality = Municipalities.find(binding.actMunicipality.text?.toString().orEmpty())

        // Validación: se revisan todos los campos y se marcan los que fallan
        var isValid = true
        if (name.length < MIN_NAME_LENGTH) {
            binding.tilName.error = getString(R.string.form_error_name)
            isValid = false
        }
        if (municipality == null) {
            binding.tilMunicipality.error = getString(R.string.form_error_municipality)
            isValid = false
        }
        if (description.length < MIN_DESCRIPTION_LENGTH) {
            binding.tilDescription.error = getString(R.string.form_error_description)
            isValid = false
        }
        if (!isValid || municipality == null) return

        val category = selectedCategory()
        val emoji = selectedEmoji()
        val editing = editingPlace

        val placeId = if (editing == null) {
            PlaceRepository.addCustomPlace(name, municipality, category, emoji, description)
        } else {
            PlaceRepository.updateCustomPlace(
                editing.id, name, municipality, category, emoji, description
            )
            editing.id
        }

        // Se le avisa a la pantalla anterior que todo salió bien
        setResult(RESULT_OK, Intent().putExtra(IntentKeys.PLACE_ID, placeId))
        finish()
    }

    private fun selectedCategory(): Category = when (binding.chipGroupCategory.checkedChipId) {
        R.id.chipAdventure -> Category.ADVENTURE
        R.id.chipCulture -> Category.CULTURE
        else -> Category.NATURE
    }

    private fun chipIdFor(category: Category): Int = when (category) {
        Category.ADVENTURE -> R.id.chipAdventure
        Category.CULTURE -> R.id.chipCulture
        Category.NATURE -> R.id.chipNature
    }

    private fun selectedEmoji(): String {
        val chip = binding.chipGroupEmoji.findViewById<Chip>(binding.chipGroupEmoji.checkedChipId)
        return chip?.text?.toString() ?: DEFAULT_EMOJI
    }

    companion object {
        private const val NO_ID = -1
        private const val MIN_NAME_LENGTH = 3
        private const val MIN_DESCRIPTION_LENGTH = 10
        private const val DEFAULT_EMOJI = "📍"

        /** Sin placeId abre el formulario vacío; con placeId abre el lugar para editarlo. */
        fun newIntent(context: Context, placeId: Int? = null): Intent {
            val intent = Intent(context, PlaceFormActivity::class.java)
            if (placeId != null) intent.putExtra(IntentKeys.PLACE_ID, placeId)
            return intent
        }
    }
}
