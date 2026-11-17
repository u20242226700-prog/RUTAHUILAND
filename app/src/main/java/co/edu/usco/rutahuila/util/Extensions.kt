package co.edu.usco.rutahuila.util

import java.text.Normalizer
import java.util.Locale

private val DIACRITICS = "\\p{Mn}+".toRegex()

/** Quita tildes y pasa a minúsculas: "San Agustín" -> "san agustin". */
fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .trim()
