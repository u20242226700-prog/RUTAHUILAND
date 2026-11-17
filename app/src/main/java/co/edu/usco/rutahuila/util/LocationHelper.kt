package co.edu.usco.rutahuila.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import co.edu.usco.rutahuila.data.Place
import kotlin.math.roundToInt

/**
 * Ubicación del usuario (GPS).
 * Guarda en memoria la última ubicación obtenida y calcula la distancia
 * en línea recta desde ahí hasta cualquier lugar.
 */
object LocationHelper {

    /** Permisos que se le piden al usuario (ubicación precisa o aproximada). */
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private const val METERS_PER_KM = 1000

    /** Última ubicación conocida, o null si todavía no se ha pedido. */
    var lastLocation: Location? = null
        private set

    /** true si el usuario ya aceptó al menos uno de los dos permisos. */
    fun hasPermission(context: Context): Boolean =
        PERMISSIONS.any { isGranted(context, it) }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Pide la ubicación actual UNA vez. Cuando llega (o falla), avisa con
     * onResult(true/false). La respuesta puede tardar unos segundos.
     */
    fun requestLocation(context: Context, onResult: (Boolean) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = chooseProvider(context, manager)
        if (provider == null) {
            onResult(false)
            return
        }

        val noCancel: CancellationSignal? = null
        try {
            LocationManagerCompat.getCurrentLocation(
                manager, provider, noCancel, ContextCompat.getMainExecutor(context)
            ) { location ->
                if (location != null) lastLocation = location
                onResult(location != null)
            }
        } catch (e: SecurityException) {
            onResult(false)
        }
    }

    /** El GPS es más exacto, pero necesita el permiso de ubicación precisa. */
    private fun chooseProvider(context: Context, manager: LocationManager): String? {
        val canUseGps = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        return when {
            !hasPermission(context) -> null
            canUseGps -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    /** Distancia en línea recta (km) del usuario al lugar, o null si no hay ubicación. */
    fun distanceKm(place: Place): Int? {
        val here = lastLocation ?: return null
        val result = FloatArray(1)
        Location.distanceBetween(
            here.latitude, here.longitude, place.latitude, place.longitude, result
        )
        return (result[0] / METERS_PER_KM).roundToInt()
    }
}
