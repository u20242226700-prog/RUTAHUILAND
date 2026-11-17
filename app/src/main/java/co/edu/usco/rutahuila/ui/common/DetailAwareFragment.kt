package co.edu.usco.rutahuila.ui.common

import android.app.Activity
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.ui.detail.DetailActivity
import co.edu.usco.rutahuila.ui.main.MainActivity
import com.google.android.material.snackbar.Snackbar

/**
 * Clase base para las pestañas que abren el detalle de un lugar.
 *
 * - Abre DetailActivity y RECIBE el resultado cuando el usuario vuelve.
 * - Refresca la pestaña cada vez que se vuelve visible.
 *
 * Así no se repite el mismo código en Explorar, Favoritos y Mi ruta (herencia).
 */
abstract class DetailAwareFragment : Fragment() {

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        refresh()
        (activity as? MainActivity)?.updateTripBadge()
        if (result.resultCode == Activity.RESULT_OK) {
            PlaceFormatter.detailResultMessage(requireContext(), result.data)
                ?.let { showMessage(it) }
        }
    }

    /** Cada pestaña define cómo se actualiza su contenido. */
    protected abstract fun refresh()

    /** Vista sobre la que se ancla el Snackbar (por ejemplo, un botón flotante). */
    protected open fun snackbarAnchor(): View? = null

    protected fun openDetail(place: Place) {
        detailLauncher.launch(DetailActivity.newIntent(requireContext(), place.id))
    }

    protected fun showMessage(message: String) {
        val root = view ?: return
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(snackbarAnchor())
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) refresh()
    }
}
