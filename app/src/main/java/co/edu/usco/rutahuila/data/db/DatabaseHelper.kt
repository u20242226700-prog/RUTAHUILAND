package co.edu.usco.rutahuila.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import co.edu.usco.rutahuila.data.PlacesCatalog


class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    /** Se ejecuta UNA sola vez: la primera vez que se abre la app. */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_PLACES)
        db.execSQL(SQL_CREATE_USER_PLACE)
        insertInitialPlaces(db)
    }

    /**
     * MIGRACIÓN: se ejecuta cuando el usuario ya tenía una versión anterior
     * de la base de datos. Se agregan las columnas nuevas SIN borrar sus datos.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_PLACES ADD COLUMN $COL_IS_CUSTOM INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_USER_PLACE ADD COLUMN $COL_PHOTO_PATH TEXT")
        }
    }


    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun insertInitialPlaces(db: SQLiteDatabase) {
        PlacesCatalog.places.forEach { place ->
            val placeValues = ContentValues().apply {
                put(COL_ID, place.id)
                put(COL_NAME, place.name)
                put(COL_MUNICIPALITY, place.municipality)
                put(COL_CATEGORY, place.category.name)
                put(COL_DISTANCE_KM, place.distanceKm)
                put(COL_EMOJI, place.emoji)
                put(COL_LATITUDE, place.latitude)
                put(COL_LONGITUDE, place.longitude)
                put(COL_DESCRIPTION, place.description)
                put(COL_TIP, place.tip)
                put(COL_IS_CUSTOM, 0)
            }
            db.insert(TABLE_PLACES, null, placeValues)

            // Cada lugar arranca con su fila de usuario "en blanco"
            val userValues = ContentValues().apply { put(COL_PLACE_ID, place.id) }
            db.insert(TABLE_USER_PLACE, null, userValues)
        }
    }

    companion object {
        const val DATABASE_NAME = "rutahuila.db"
        const val DATABASE_VERSION = 2

        // Tabla places
        const val TABLE_PLACES = "places"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_MUNICIPALITY = "municipality"
        const val COL_CATEGORY = "category"
        const val COL_DISTANCE_KM = "distance_km"
        const val COL_EMOJI = "emoji"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_DESCRIPTION = "description"
        const val COL_TIP = "tip"
        const val COL_IS_CUSTOM = "is_custom"

        // Tabla user_place
        const val TABLE_USER_PLACE = "user_place"
        const val COL_PLACE_ID = "place_id"
        const val COL_IS_FAVORITE = "is_favorite"
        const val COL_IS_VISITED = "is_visited"
        const val COL_RATING = "rating"
        const val COL_NOTE = "note"
        const val COL_TRIP_POSITION = "trip_position"
        const val COL_PHOTO_PATH = "photo_path"

        private const val SQL_CREATE_PLACES = """
            CREATE TABLE $TABLE_PLACES (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_NAME TEXT NOT NULL,
                $COL_MUNICIPALITY TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_DISTANCE_KM INTEGER NOT NULL,
                $COL_EMOJI TEXT NOT NULL,
                $COL_LATITUDE REAL NOT NULL,
                $COL_LONGITUDE REAL NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_TIP TEXT NOT NULL,
                $COL_IS_CUSTOM INTEGER NOT NULL DEFAULT 0
            )
        """

        private const val SQL_CREATE_USER_PLACE = """
            CREATE TABLE $TABLE_USER_PLACE (
                $COL_PLACE_ID INTEGER PRIMARY KEY,
                $COL_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COL_IS_VISITED INTEGER NOT NULL DEFAULT 0,
                $COL_RATING REAL NOT NULL DEFAULT 0,
                $COL_NOTE TEXT NOT NULL DEFAULT '',
                $COL_TRIP_POSITION INTEGER,
                $COL_PHOTO_PATH TEXT,
                FOREIGN KEY ($COL_PLACE_ID) REFERENCES $TABLE_PLACES ($COL_ID)
            )
        """
    }
}
