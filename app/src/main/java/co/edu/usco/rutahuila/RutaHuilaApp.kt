package co.edu.usco.rutahuila

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import co.edu.usco.rutahuila.data.PlaceRepository
import co.edu.usco.rutahuila.data.weather.WeatherRepository

/**
 * Clase Application: se ejecuta una sola vez al abrir la app.
 * Inicializa el repositorio compartido y aplica el modo oscuro guardado.
 */
class RutaHuilaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlaceRepository.init(this)
        WeatherRepository.init(this)
        AppCompatDelegate.setDefaultNightMode(
            if (PlaceRepository.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
