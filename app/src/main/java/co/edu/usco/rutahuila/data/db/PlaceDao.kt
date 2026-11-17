package co.edu.usco.rutahuila.data.db

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import co.edu.usco.rutahuila.data.Category
import co.edu.usco.rutahuila.data.Place
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_CATEGORY
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_DESCRIPTION
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_DISTANCE_KM
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_EMOJI
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_ID
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_IS_CUSTOM
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_IS_FAVORITE
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_IS_VISITED
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_LATITUDE
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_LONGITUDE
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_MUNICIPALITY
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_NAME
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_NOTE
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_PHOTO_PATH
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_PLACE_ID
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_RATING
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_TIP
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.COL_TRIP_POSITION
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.TABLE_PLACES
import co.edu.usco.rutahuila.data.db.DatabaseHelper.Companion.TABLE_USER_PLACE

data class UserPlaceState(
    val isFavorite: Boolean,
    val isVisited: Boolean,
    val rating: Float,
    val note: String,
    val isInTrip: Boolean,
    val photoPath: String?
)


class PlaceDao(private val helper: DatabaseHelper) {

    // =====================================================================
    // LEER (SELECT) la tabla places
    // =====================================================================

    /** SELECT * FROM places ORDER BY id */
    fun getAllPlaces(): List<Place> =
        helper.readableDatabase
            .rawQuery("SELECT * FROM $TABLE_PLACES ORDER BY $COL_ID", null)
            .toPlaceList()

    /** SELECT * FROM places WHERE id = ? */
    fun getPlace(id: Int): Place? =
        helper.readableDatabase
            .rawQuery("SELECT * FROM $TABLE_PLACES WHERE $COL_ID = ?", arrayOf(id.toString()))
            .toPlaceList()
            .firstOrNull()

    /** JOIN de las dos tablas: solo los lugares marcados como favoritos. */
    fun getFavoritePlaces(): List<Place> =
        helper.readableDatabase.rawQuery(
            """
            SELECT p.* FROM $TABLE_PLACES p
            INNER JOIN $TABLE_USER_PLACE u ON u.$COL_PLACE_ID = p.$COL_ID
            WHERE u.$COL_IS_FAVORITE = 1
            ORDER BY p.$COL_NAME
            """,
            null
        ).toPlaceList()

    /** JOIN de las dos tablas: los lugares de la ruta, en el orden del usuario. */
    fun getTripPlaces(): List<Place> =
        helper.readableDatabase.rawQuery(
            """
            SELECT p.* FROM $TABLE_PLACES p
            INNER JOIN $TABLE_USER_PLACE u ON u.$COL_PLACE_ID = p.$COL_ID
            WHERE u.$COL_TRIP_POSITION IS NOT NULL
            ORDER BY u.$COL_TRIP_POSITION
            """,
            null
        ).toPlaceList()

    // =====================================================================
    // CREAR, ACTUALIZAR y BORRAR lugares propios (tabla places)
    // =====================================================================

    /**
     * INSERT INTO places ... (el id lo genera SQLite automáticamente).
     * También crea su fila en user_place. Devuelve el id del lugar nuevo.
     */
    fun insertPlace(place: Place): Int {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val newId = db.insertOrThrow(TABLE_PLACES, null, place.toContentValues())
            val userValues = ContentValues().apply { put(COL_PLACE_ID, newId) }
            db.insertOrThrow(TABLE_USER_PLACE, null, userValues)
            db.setTransactionSuccessful()
            return newId.toInt()
        } finally {
            db.endTransaction()
        }
    }

    /** UPDATE places SET ... WHERE id = ? AND is_custom = 1 */
    fun updatePlace(place: Place) {
        helper.writableDatabase.update(
            TABLE_PLACES,
            place.toContentValues(),
            "$COL_ID = ? AND $COL_IS_CUSTOM = 1",
            arrayOf(place.id.toString())
        )
    }

    /**
     * DELETE de un lugar propio. Primero se borra su fila de user_place
     * (por la llave foránea) y luego el lugar. Todo en una transacción.
     */
    fun deletePlace(placeId: Int) {
        val db = helper.writableDatabase
        val args = arrayOf(placeId.toString())
        db.beginTransaction()
        try {
            db.delete(
                TABLE_USER_PLACE,
                "$COL_PLACE_ID = ? AND $COL_PLACE_ID IN " +
                    "(SELECT $COL_ID FROM $TABLE_PLACES WHERE $COL_IS_CUSTOM = 1)",
                args
            )
            db.delete(TABLE_PLACES, "$COL_ID = ? AND $COL_IS_CUSTOM = 1", args)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Columnas de un lugar (sin el id, que lo maneja SQLite). */
    private fun Place.toContentValues() = ContentValues().apply {
        put(COL_NAME, name)
        put(COL_MUNICIPALITY, municipality)
        put(COL_CATEGORY, category.name)
        put(COL_DISTANCE_KM, distanceKm)
        put(COL_EMOJI, emoji)
        put(COL_LATITUDE, latitude)
        put(COL_LONGITUDE, longitude)
        put(COL_DESCRIPTION, description)
        put(COL_TIP, tip)
        put(COL_IS_CUSTOM, if (isCustom) 1 else 0)
    }

    // =====================================================================
    // LEER (SELECT) la tabla user_place
    // =====================================================================

    /** SELECT * FROM user_place WHERE place_id = ? */
    fun getUserState(placeId: Int): UserPlaceState {
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_USER_PLACE WHERE $COL_PLACE_ID = ?",
            arrayOf(placeId.toString())
        )
        return cursor.use {
            if (!it.moveToFirst()) {
                EMPTY_STATE
            } else {
                val photoIndex = it.getColumnIndexOrThrow(COL_PHOTO_PATH)
                UserPlaceState(
                    isFavorite = it.getInt(it.getColumnIndexOrThrow(COL_IS_FAVORITE)) == 1,
                    isVisited = it.getInt(it.getColumnIndexOrThrow(COL_IS_VISITED)) == 1,
                    rating = it.getFloat(it.getColumnIndexOrThrow(COL_RATING)),
                    note = it.getString(it.getColumnIndexOrThrow(COL_NOTE)).orEmpty(),
                    isInTrip = !it.isNull(it.getColumnIndexOrThrow(COL_TRIP_POSITION)),
                    photoPath = if (it.isNull(photoIndex)) null else it.getString(photoIndex)
                )
            }
        }
    }

    /** IDs de la ruta en orden. */
    fun getTripIds(): List<Int> {
        val cursor = helper.readableDatabase.rawQuery(
            """
            SELECT $COL_PLACE_ID FROM $TABLE_USER_PLACE
            WHERE $COL_TRIP_POSITION IS NOT NULL
            ORDER BY $COL_TRIP_POSITION
            """,
            null
        )
        val ids = mutableListOf<Int>()
        cursor.use {
            while (it.moveToNext()) ids.add(it.getInt(0))
        }
        return ids
    }

    fun countFavorites(): Int = count("$COL_IS_FAVORITE = 1")

    fun countVisited(): Int = count("$COL_IS_VISITED = 1")

    fun countRated(): Int = count("$COL_RATING > 0")

    fun countPhotos(): Int = count("$COL_PHOTO_PATH IS NOT NULL")


    fun averageRating(): Float {
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT AVG($COL_RATING) FROM $TABLE_USER_PLACE WHERE $COL_RATING > 0",
            null
        )
        return cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getFloat(0) else 0f
        }
    }

    private fun count(where: String): Int =
        DatabaseUtils.queryNumEntries(helper.readableDatabase, TABLE_USER_PLACE, where).toInt()

    // =====================================================================
    // ACTUALIZAR (UPDATE) la tabla user_place
    // =====================================================================

    fun setFavorite(placeId: Int, favorite: Boolean) =
        updateUserPlace(placeId) { put(COL_IS_FAVORITE, if (favorite) 1 else 0) }

    fun setVisited(placeId: Int, visited: Boolean) =
        updateUserPlace(placeId) { put(COL_IS_VISITED, if (visited) 1 else 0) }

    fun setRating(placeId: Int, rating: Float) =
        updateUserPlace(placeId) { put(COL_RATING, rating) }

    fun setNote(placeId: Int, note: String) =
        updateUserPlace(placeId) { put(COL_NOTE, note) }

    /** Guarda la ruta del archivo de la foto (o NULL para quitarla). */
    fun setPhotoPath(placeId: Int, path: String?) =
        updateUserPlace(placeId) {
            if (path == null) putNull(COL_PHOTO_PATH) else put(COL_PHOTO_PATH, path)
        }

    private fun updateUserPlace(placeId: Int, fill: ContentValues.() -> Unit) {
        val values = ContentValues().apply(fill)
        helper.writableDatabase.update(
            TABLE_USER_PLACE, values, "$COL_PLACE_ID = ?", arrayOf(placeId.toString())
        )
    }

    /**
     * Guarda el orden completo de la ruta.
     * Usa una TRANSACCIÓN: o se guardan todos los cambios, o ninguno.
     */
    fun saveTripOrder(orderedIds: List<Int>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            // 1. Se sacan todos los lugares de la ruta
            db.execSQL("UPDATE $TABLE_USER_PLACE SET $COL_TRIP_POSITION = NULL")

            // 2. Se asigna la posición nueva a cada parada
            orderedIds.forEachIndexed { position, placeId ->
                val values = ContentValues().apply { put(COL_TRIP_POSITION, position) }
                db.update(
                    TABLE_USER_PLACE, values, "$COL_PLACE_ID = ?", arrayOf(placeId.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Deja la base de datos como recién instalada: borra los lugares propios
     * y limpia las filas del usuario. No borra los lugares del catálogo.
     */
    fun resetUserData() {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                """
                DELETE FROM $TABLE_USER_PLACE WHERE $COL_PLACE_ID IN
                    (SELECT $COL_ID FROM $TABLE_PLACES WHERE $COL_IS_CUSTOM = 1)
                """
            )
            db.execSQL("DELETE FROM $TABLE_PLACES WHERE $COL_IS_CUSTOM = 1")
            db.execSQL(
                """
                UPDATE $TABLE_USER_PLACE SET
                    $COL_IS_FAVORITE = 0,
                    $COL_IS_VISITED = 0,
                    $COL_RATING = 0,
                    $COL_NOTE = '',
                    $COL_TRIP_POSITION = NULL,
                    $COL_PHOTO_PATH = NULL
                """
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // =====================================================================
    // Conversión de filas (Cursor) a objetos Kotlin
    // =====================================================================

    /** Recorre el cursor, convierte cada fila en un Place y lo cierra. */
    private fun Cursor.toPlaceList(): List<Place> {
        val places = mutableListOf<Place>()
        use {
            while (it.moveToNext()) {
                places.add(
                    Place(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_NAME)),
                        municipality = it.getString(it.getColumnIndexOrThrow(COL_MUNICIPALITY)),
                        category = Category.valueOf(it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))),
                        distanceKm = it.getInt(it.getColumnIndexOrThrow(COL_DISTANCE_KM)),
                        emoji = it.getString(it.getColumnIndexOrThrow(COL_EMOJI)),
                        latitude = it.getDouble(it.getColumnIndexOrThrow(COL_LATITUDE)),
                        longitude = it.getDouble(it.getColumnIndexOrThrow(COL_LONGITUDE)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_DESCRIPTION)),
                        tip = it.getString(it.getColumnIndexOrThrow(COL_TIP)),
                        isCustom = it.getInt(it.getColumnIndexOrThrow(COL_IS_CUSTOM)) == 1
                    )
                )
            }
        }
        return places
    }

    private companion object {
        val EMPTY_STATE = UserPlaceState(
            isFavorite = false, isVisited = false, rating = 0f, note = "",
            isInTrip = false, photoPath = null
        )
    }
}
