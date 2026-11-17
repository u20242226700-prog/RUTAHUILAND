package co.edu.usco.rutahuila.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


object PhotoStorage {

    private const val PHOTOS_DIR = "photos"
    private const val TEMP_DIR = "camera"
    private const val CAMERA_FILE = "captura.jpg"
    private const val GALLERY_FILE = "galeria.tmp"
    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val MAX_SIZE = 1280
    private const val JPEG_QUALITY = 85


    fun cameraUri(context: Context): Uri = FileProvider.getUriForFile(
        context, context.packageName + AUTHORITY_SUFFIX, cameraFile(context)
    )

    suspend fun saveFromCamera(context: Context, placeId: Int): String? =
        withContext(Dispatchers.IO) {
            process(context, cameraFile(context), placeId)
        }

    suspend fun saveFromGallery(context: Context, uri: Uri, placeId: Int): String? =
        withContext(Dispatchers.IO) {
            val temp = File(tempDir(context), GALLERY_FILE)
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                input.use { source -> temp.outputStream().use { source.copyTo(it) } }
                process(context, temp, placeId)
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }

    fun load(path: String): Bitmap? = BitmapFactory.decodeFile(path)

    fun delete(path: String) {
        File(path).delete()
    }

    fun deleteAll(context: Context) {
        File(context.filesDir, PHOTOS_DIR).deleteRecursively()
    }

    // =====================================================================
    // Funciones internas
    // =====================================================================

    private fun tempDir(context: Context): File =
        File(context.cacheDir, TEMP_DIR).apply { mkdirs() }

    private fun cameraFile(context: Context): File = File(tempDir(context), CAMERA_FILE)

    private fun process(context: Context, source: File, placeId: Int): String? {
        val bitmap = decodeScaled(source) ?: return null
        val dir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
        val destination = File(dir, "lugar_${placeId}_${System.currentTimeMillis()}.jpg")
        return try {
            destination.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
            }
            source.delete()
            destination.absolutePath
        } catch (e: IOException) {
            null
        }
    }

    private fun decodeScaled(file: File): Bitmap? {
        // 1. Se leen solo las medidas, sin cargar la imagen en memoria
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (largestSide <= 0) return null

        // 2. Se carga a la mitad, un cuarto, etc. según haga falta
        var sample = 1
        while (largestSide / (sample * 2) >= MAX_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null

        // 3. Ajuste final de tamaño y rotación
        val scale = minOf(1f, MAX_SIZE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotationOf(file))
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }

    private fun rotationOf(file: File): Float = try {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: IOException) {
        0f
    }
}
