package com.promptgallery.data.storage

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.promptgallery.core.util.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Generation parameters recovered from an image file, all best-effort. */
data class ExtractedMetadata(
    val prompt: String = "",
    val negativePrompt: String = "",
    val aiModel: String = "",
    val seed: Long? = null,
    val sampler: String = "",
    val cfg: Float? = null,
    val steps: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * Extracts embedded generation metadata from imported images.
 *
 * Supports the de-facto Automatic1111 / ComfyUI convention of storing the
 * generation recipe in a PNG `tEXt`/`iTXt` chunk keyed `parameters`, and falls
 * back to the EXIF `UserComment`/`ImageDescription` tags used by some tools.
 */
@Singleton
class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend fun extract(uri: Uri): ExtractedMetadata = withContext(dispatcher) {
        val raw = readPngParameters(uri) ?: readExifParameters(uri)
        if (raw.isNullOrBlank()) ExtractedMetadata() else parseAutomatic1111(raw)
    }

    // ---- PNG text chunks -------------------------------------------------

    private fun readPngParameters(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { readPngTextChunks(it)["parameters"] }
    }.getOrNull()

    /**
     * Minimal PNG parser that reads `tEXt` and `iTXt` chunks into a key/value
     * map without decoding the pixels.
     */
    private fun readPngTextChunks(input: InputStream): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val signature = ByteArray(8)
        if (input.read(signature) != 8) return result
        val isPng = signature[0].toInt() and 0xFF == 0x89 && signature[1].toInt() == 'P'.code
        if (!isPng) return result

        while (true) {
            val length = input.readIntBE() ?: break
            val type = ByteArray(4)
            if (input.read(type) != 4) break
            val typeName = String(type, Charsets.US_ASCII)
            val data = ByteArray(length)
            var read = 0
            while (read < length) {
                val r = input.read(data, read, length - read)
                if (r == -1) break
                read += r
            }
            input.skip(4) // CRC

            when (typeName) {
                "tEXt" -> parseTextChunk(data)?.let { result[it.first] = it.second }
                "iTXt" -> parseITextChunk(data)?.let { result[it.first] = it.second }
                "IDAT", "IEND" -> return result // text chunks precede pixel data
            }
        }
        return result
    }

    private fun parseTextChunk(data: ByteArray): Pair<String, String>? {
        val nullIdx = data.indexOf(0)
        if (nullIdx <= 0) return null
        val key = String(data, 0, nullIdx, Charsets.ISO_8859_1)
        val value = String(data, nullIdx + 1, data.size - nullIdx - 1, Charsets.ISO_8859_1)
        return key to value
    }

    private fun parseITextChunk(data: ByteArray): Pair<String, String>? {
        val nullIdx = data.indexOf(0)
        if (nullIdx <= 0) return null
        val key = String(data, 0, nullIdx, Charsets.US_ASCII)
        // Skip: compressionFlag(1), compressionMethod(1), languageTag\0, translatedKey\0
        var cursor = nullIdx + 3
        cursor = data.indexOf(0, cursor) + 1
        cursor = data.indexOf(0, cursor) + 1
        if (cursor <= 0 || cursor > data.size) return null
        val value = String(data, cursor, data.size - cursor, Charsets.UTF_8)
        return key to value
    }

    // ---- EXIF ------------------------------------------------------------

    private fun readExifParameters(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
                ?: exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
        }
    }.getOrNull()

    // ---- Automatic1111 parameter string parsing --------------------------

    /**
     * Parses the standard layout:
     *   <positive prompt>
     *   Negative prompt: <negative>
     *   Steps: 20, Sampler: Euler a, CFG scale: 7, Seed: 1, Size: 512x768, Model: x
     */
    internal fun parseAutomatic1111(raw: String): ExtractedMetadata {
        val text = raw.replace("\r\n", "\n").trim()
        val negIndex = text.indexOf("Negative prompt:")
        val settingsIndex = findSettingsLineIndex(text)

        val prompt: String
        val negative: String
        val settingsLine: String

        if (negIndex >= 0) {
            prompt = text.substring(0, negIndex).trim()
            val afterNeg = text.substring(negIndex + "Negative prompt:".length)
            val negEnd = if (settingsIndex > negIndex) {
                settingsIndex - (negIndex + "Negative prompt:".length)
            } else {
                afterNeg.length
            }
            negative = afterNeg.substring(0, negEnd.coerceIn(0, afterNeg.length)).trim()
            settingsLine = if (settingsIndex >= 0) text.substring(settingsIndex).trim() else ""
        } else if (settingsIndex >= 0) {
            prompt = text.substring(0, settingsIndex).trim()
            negative = ""
            settingsLine = text.substring(settingsIndex).trim()
        } else {
            prompt = text
            negative = ""
            settingsLine = ""
        }

        val fields = parseKeyValueSettings(settingsLine)
        val size = fields["Size"]?.split("x", "X")?.mapNotNull { it.trim().toIntOrNull() }

        return ExtractedMetadata(
            prompt = prompt,
            negativePrompt = negative,
            aiModel = fields["Model"] ?: fields["model"] ?: "",
            seed = fields["Seed"]?.toLongOrNull(),
            sampler = fields["Sampler"] ?: "",
            cfg = (fields["CFG scale"] ?: fields["CFG"])?.toFloatOrNull(),
            steps = fields["Steps"]?.toIntOrNull(),
            width = size?.getOrNull(0),
            height = size?.getOrNull(1),
        )
    }

    /** Finds the start of the comma-separated settings line, if present. */
    private fun findSettingsLineIndex(text: String): Int {
        val markers = listOf("\nSteps:", "\nSampler:", "\nSeed:", "\nModel:", "\nCFG scale:")
        return markers.mapNotNull { marker ->
            val idx = text.indexOf(marker)
            if (idx >= 0) idx + 1 else null
        }.minOrNull() ?: -1
    }

    /** Splits "Key: value, Key2: value2" honouring values that contain commas. */
    private fun parseKeyValueSettings(line: String): Map<String, String> {
        if (line.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        // Match `Key: value` where value runs until the next `, Key:` boundary.
        val regex = Regex("([A-Za-z][A-Za-z0-9 _/]*?):\\s*([^,]*(?:,(?![A-Za-z][A-Za-z0-9 _/]*?:)[^,]*)*)")
        regex.findAll(line).forEach { match ->
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            if (key.isNotEmpty()) result[key] = value
        }
        return result
    }
}

private fun ByteArray.indexOf(byte: Int, from: Int = 0): Int {
    for (i in from until size) if (this[i].toInt() == byte) return i
    return -1
}

private fun InputStream.readIntBE(): Int? {
    val b = ByteArray(4)
    if (read(b) != 4) return null
    return ((b[0].toInt() and 0xFF) shl 24) or
        ((b[1].toInt() and 0xFF) shl 16) or
        ((b[2].toInt() and 0xFF) shl 8) or
        (b[3].toInt() and 0xFF)
}
