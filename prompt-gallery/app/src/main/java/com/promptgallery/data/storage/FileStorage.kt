package com.promptgallery.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.promptgallery.core.util.DefaultDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Result of persisting an image's binary into app storage. */
data class StoredFile(
    val filePath: String,
    val thumbnailPath: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val mimeType: String,
)

/**
 * Owns the on-disk layout of the library inside app-private storage:
 *
 *   filesDir/images/<id>.<ext>       full-resolution copies
 *   filesDir/thumbnails/<id>.jpg     downscaled previews for the grid
 *   cacheDir/exports/                transient export artifacts
 *
 * Because everything lives under the app sandbox, no storage permissions are
 * required and the data is private to the user.
 */
@Singleton
class FileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val imagesDir: File by lazy { File(context.filesDir, "images").apply { mkdirs() } }
    private val thumbsDir: File by lazy { File(context.filesDir, "thumbnails").apply { mkdirs() } }
    val exportsDir: File by lazy { File(context.cacheDir, "exports").apply { mkdirs() } }

    fun imageFile(id: String, extension: String): File = File(imagesDir, "$id.$extension")
    fun thumbnailFile(id: String): File = File(thumbsDir, "$id.jpg")

    /**
     * Copies the content at [uri] into the images directory, decodes its
     * dimensions, and writes a thumbnail. The image bytes are read only once.
     */
    suspend fun storeFromUri(uri: Uri, id: String): StoredFile = withContext(dispatcher) {
        val resolver = context.contentResolver
        val (displayName, reportedSize) = queryNameAndSize(uri)
        val mimeType = resolver.getType(uri) ?: guessMimeType(displayName)
        val extension = extensionFor(mimeType, displayName)

        val target = imageFile(id, extension)
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
        } ?: error("Unable to open input stream for $uri")

        val (width, height) = decodeDimensions(target)
        val thumb = writeThumbnail(target, id)

        StoredFile(
            filePath = target.absolutePath,
            thumbnailPath = thumb.absolutePath,
            fileName = displayName,
            width = width,
            height = height,
            fileSize = if (reportedSize > 0) reportedSize else target.length(),
            mimeType = mimeType,
        )
    }

    /** Generates a thumbnail capped at [THUMB_MAX_EDGE] px on the long edge. */
    fun writeThumbnail(source: File, id: String): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        val longEdge = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(longEdge, THUMB_MAX_EDGE)
        }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options)
            ?: return source // Fallback: use the original if decoding fails.

        val scaled = scaleToMaxEdge(decoded, THUMB_MAX_EDGE)
        val thumb = thumbnailFile(id)
        FileOutputStream(thumb).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, out)
        }
        if (scaled != decoded) decoded.recycle()
        scaled.recycle()
        return thumb
    }

    fun decodeDimensions(file: File): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            return bounds.outWidth to bounds.outHeight
        }
        // Some formats expose size only via EXIF.
        return runCatching {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) to
                exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        }.getOrDefault(0 to 0)
    }

    suspend fun deleteFiles(paths: List<String>) = withContext(dispatcher) {
        paths.forEach { runCatching { File(it).delete() } }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long> {
        var name = "image_${System.currentTimeMillis()}"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0 && !cursor.isNull(nameIdx)) name = cursor.getString(nameIdx)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longEdge
        val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun calculateInSampleSize(longEdge: Int, target: Int): Int {
        var sample = 1
        var edge = longEdge
        while (edge / 2 >= target) {
            edge /= 2
            sample *= 2
        }
        return sample
    }

    private fun extensionFor(mimeType: String, name: String): String = when {
        name.substringAfterLast('.', "").isNotBlank() -> name.substringAfterLast('.').lowercase()
        mimeType.contains("png") -> "png"
        mimeType.contains("webp") -> "webp"
        mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
        else -> "img"
    }

    private fun guessMimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "image/*"
    }

    companion object {
        const val THUMB_MAX_EDGE = 512
        const val THUMB_QUALITY = 82
    }
}
