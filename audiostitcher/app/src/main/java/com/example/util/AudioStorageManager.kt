package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import java.security.MessageDigest

object AudioStorageManager {

    private const val TAG = "AudioStorageManager"

    fun getAudioClipsDir(context: Context): File {
        val dir = File(context.filesDir, "audio_clips")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getMergedAudiosDir(context: Context): File {
        val dir = File(context.filesDir, "merged_audios")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies incoming content URI to internal storage file and extracts metadata.
     */
    fun saveAudioFromUri(context: Context, uri: Uri, preferredName: String? = null): AudioMetaData {
        val clipsDir = getAudioClipsDir(context)
        val contentResolver = context.contentResolver

        // Determine mime type and extension
        val mimeType = contentResolver.getType(uri) ?: "audio/ogg"
        val extension = when {
            mimeType.contains("opus") -> "opus"
            mimeType.contains("ogg") -> "ogg"
            mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
            mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
            mimeType.contains("wav") -> "wav"
            else -> "opus"
        }

        val timeStamp = SimpleDateFormat("HHmmss_SSS", Locale.getDefault()).format(Date())
        val fileName = preferredName ?: "audio_$timeStamp.$extension"
        val targetFile = File(clipsDir, "clip_${UUID.randomUUID().toString().take(8)}_$fileName")

        var fileSizeBytes = 0L
        val digest = MessageDigest.getInstance("SHA-256")

        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    fileSizeBytes += bytesRead
                }
            }
        }

        val fileHash = digest.digest().joinToString("") { "%02x".format(it) }
        val durationMs = extractDurationMs(targetFile)

        return AudioMetaData(
            file = targetFile,
            originalName = fileName,
            mimeType = mimeType,
            durationMs = durationMs,
            fileSizeBytes = fileSizeBytes,
            fileHash = fileHash
        )
    }

    fun calculateFileHash(file: File): String {
        return try {
            if (!file.exists()) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun extractDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            timeStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting duration for ${file.name}", e)
            0L
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    data class AudioMetaData(
        val file: File,
        val originalName: String,
        val mimeType: String,
        val durationMs: Long,
        val fileSizeBytes: Long,
        val fileHash: String = ""
    )
}
