package co.edu.usco.rutahuila.ui.main

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import co.edu.usco.rutahuila.R
import co.edu.usco.rutahuila.data.PlaceFilter
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.databinding.ActivityMainBinding
import co.edu.usco.rutahuila.ui.explore.ExploreFragment
import co.edu.usco.rutahuila.ui.favorites.FavoritesFragment
import co.edu.usco.rutahuila.ui.map.MapFragment
import co.edu.usco.rutahuila.ui.profile.ProfileFragment
import co.edu.usco.rutahuila.ui.trip.TripFragment
import co.edu.usco.rutahuila.util.IntentKeys
import co.edu.usco.rutahuila.util.LocationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        loadUserLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        // Si la pantalla se recrea (por ejemplo al rotar), Android restaura los fragments solo
        if (savedInstanceState == null) {
            showTab(R.id.nav_explore)
        }
        updateTripBadge() 

        if (PlaceRepository.arePermissionsAsked) {
            loadUserLocation()
        } else {
            explainPermissions()
        }
    }

    // =====================================================================
    // Permisos
    // =====================================================================

    /** Antes de pedir los permisos se le explica al usuario para qué son. */
    private fun explainPermissions() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permissions_title)
            .setMessage(R.string.permissions_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permissions_continue) { _, _ ->
                PlaceRepository.arePermissionsAsked = true
                permissionLauncher.launch(LocationHelper.PERMISSIONS + Manifest.permission.CAMERA)
            }
            .show()
    }

    /** Si el usuario aceptó la ubicación, se obtiene para mostrar las distancias reales. */
    private fun loadUserLocation() {
        if (LocationHelper.hasPermission(this)) {
            LocationHelper.requestLocation(this) { }
        }
    }

    /** Permite que un fragment cambie de pestaña (ej: "Ir a explorar"). */
    fun selectTab(@IdRes itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    /** Número sobre el ícono de "Mi ruta" con la cantidad de paradas. */
    fun updateTripBadge() {
        val count = PlaceRepository.tripIds().size
        if (count > 0) {
            binding.bottomNav.getOrCreateBadge(R.id.nav_trip).number = count
        } else {
            binding.bottomNav.removeBadge(R.id.nav_trip)
        }
    }

    private fun showTab(@IdRes itemId: Int) {
        val tag = "tab_$itemId"
        val transaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.fragments.forEach { transaction.hide(it) }

        val existing = supportFragmentManager.findFragmentByTag(tag)
        if (existing != null) {
            transaction.show(existing)
        } else {
            transaction.add(R.id.fragmentContainer, createFragment(itemId), tag)
        }
        transaction.commit()
    }

    private fun createFragment(@IdRes itemId: Int): Fragment = when (itemId) {
        R.id.nav_map -> MapFragment()
        R.id.nav_favorites -> FavoritesFragment()
        R.id.nav_trip -> TripFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> {
            val filter = intent.getStringExtra(IntentKeys.FILTER)
                ?.let { runCatching { PlaceFilter.valueOf(it) }.getOrNull() }
                ?: PlaceFilter.ALL
            ExploreFragment.newInstance(filter)
        }
    }
}
