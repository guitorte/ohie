package com.example.util

import android.content.Context
import android.media.*
import android.util.Log
import com.example.data.model.AudioClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioMerger {

    private const val TAG = "AudioMerger"

    enum class ExportFormat(val extension: String, val mimeType: String, val title: String, val subtitle: String) {
        M4A_AAC("m4a", "audio/mp4", "Áudio M4A / AAC", "Compacto, otimizado e compatível com WhatsApp"),
        OPUS_OGG("opus", "audio/ogg", "Áudio Opus / OGG", "Formato nativo do WhatsApp"),
        WAV_PCM("wav", "audio/wav", "Áudio Sem Compressão (WAV)", "Qualidade máxima sem perdas")
    }

    suspend fun mergeClips(
        context: Context,
        clips: List<AudioClip>,
        projectName: String,
        format: ExportFormat = ExportFormat.M4A_AAC,
        onProgress: (progress: Float, currentClip: Int, totalClips: Int, status: String) -> Unit = { _, _, _, _ -> }
    ): MergeResult = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            return@withContext MergeResult.Error("Nenhum áudio selecionado para unificação.")
        }

        val sortedClips = clips.sortedBy { it.orderIndex }
        val outputDir = AudioStorageManager.getMergedAudiosDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedProjectName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "Unificado_${sanitizedProjectName}_$timeStamp.${format.extension}")

        try {
            when (format) {
                ExportFormat.WAV_PCM -> mergeToWav(sortedClips, outputFile, onProgress)
                ExportFormat.M4A_AAC, ExportFormat.OPUS_OGG -> mergeToM4a(sortedClips, outputFile, onProgress)
            }

            val durationMs = AudioStorageManager.extractDurationMs(outputFile)
            MergeResult.Success(
                file = outputFile,
                totalDurationMs = durationMs,
                fileSizeBytes = outputFile.length(),
                clipCount = sortedClips.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error merging audio clips", e)
            // Fallback: try WAV output if M4A encoder failed
            if (format == ExportFormat.M4A_AAC) {
                try {
                    val fallbackFile = File(outputDir, "Unificado_${sanitizedProjectName}_$timeStamp.wav")
                    mergeToWav(sortedClips, fallbackFile, onProgress)
                    val durationMs = AudioStorageManager.extractDurationMs(fallbackFile)
                    return@withContext MergeResult.Success(
                        file = fallbackFile,
                        totalDurationMs = durationMs,
                        fileSizeBytes = fallbackFile.length(),
                        clipCount = sortedClips.size
                    )
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fallback WAV merge also failed", fallbackEx)
                }
            }
            MergeResult.Error(e.message ?: "Falha ao unificar áudios")
        }
    }

    /**
     * Decodes all audio clips to raw 16-bit PCM and writes a standard uncompressed WAV file.
     * Extremely reliable, fast, and supported everywhere.
     */
    private fun mergeToWav(
        clips: List<AudioClip>,
        outputFile: File,
        onProgress: (progress: Float, currentClip: Int, totalClips: Int, status: String) -> Unit
    ) {
        val rawPcmFile = File(outputFile.parentFile, "temp_${System.currentTimeMillis()}.pcm")
        var targetSampleRate = 44100
        var targetChannels = 1
        var totalPcmBytes = 0L

        try {
            FileOutputStream(rawPcmFile).use { pcmOut ->
                val bufferedOut = BufferedOutputStream(pcmOut)
                val totalClips = clips.size.toFloat()

                clips.forEachIndexed { index, clip ->
                    val file = File(clip.localFilePath)
                    if (!file.exists()) return@forEachIndexed

                    val currentNum = index + 1
                    val startProg = (index / totalClips) * 0.8f
                    onProgress(
                        startProg,
                        currentNum,
                        clips.size,
                        "Processando áudio $currentNum de ${clips.size}: ${clip.originalFileName}"
                    )

                    val (sampleRate, channels, pcmBytes) = decodeToPcm(file, bufferedOut)
                    if (sampleRate > 0) targetSampleRate = sampleRate
                    if (channels > 0) targetChannels = channels
                    totalPcmBytes += pcmBytes

                    val doneProg = (currentNum / totalClips) * 0.8f
                    onProgress(
                        doneProg,
                        currentNum,
                        clips.size,
                        "Concluído $currentNum de ${clips.size}"
                    )
                }
                bufferedOut.flush()
            }

            onProgress(0.9f, clips.size, clips.size, "Gerando arquivo WAV final...")
            // Write WAV Header + PCM data to outputFile
            writeWavFile(rawPcmFile, outputFile, targetSampleRate, targetChannels, totalPcmBytes)
            onProgress(1.0f, clips.size, clips.size, "Unificação concluída com sucesso!")
        } finally {
            if (rawPcmFile.exists()) {
                rawPcmFile.delete()
            }
        }
    }

    /**
     * Decodes all clips to PCM and encodes to clean M4A (AAC) using MediaCodec + MediaMuxer.
     */
    private fun mergeToM4a(
        clips: List<AudioClip>,
        outputFile: File,
        onProgress: (progress: Float, currentClip: Int, totalClips: Int, status: String) -> Unit
    ) {
        val targetSampleRate = 44100
        val targetChannels = 1
        val bitRate = 128000

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, targetSampleRate, targetChannels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val totalClips = clips.size.toFloat()

        var globalPtsUs = 0L

        clips.forEachIndexed { index, clip ->
            val file = File(clip.localFilePath)
            if (!file.exists()) return@forEachIndexed

            val currentNum = index + 1
            val startProg = (index / totalClips) * 0.9f
            onProgress(
                startProg,
                currentNum,
                clips.size,
                "Processando áudio $currentNum de ${clips.size}: ${clip.originalFileName}"
            )

            decodeAndEncodeClip(
                file = file,
                encoder = encoder,
                muxer = muxer,
                bufferInfo = bufferInfo,
                getTrackIndex = { audioTrackIndex },
                setTrackIndex = { audioTrackIndex = it },
                isMuxerStarted = { muxerStarted },
                setMuxerStarted = { muxerStarted = it },
                targetSampleRate = targetSampleRate,
                targetChannels = targetChannels,
                globalPtsUsSupplier = { globalPtsUs },
                onPtsUpdated = { globalPtsUs = it }
            )

            val doneProg = (currentNum / totalClips) * 0.9f
            onProgress(
                doneProg,
                currentNum,
                clips.size,
                "Concluído $currentNum de ${clips.size}"
            )
        }

        onProgress(0.95f, clips.size, clips.size, "Finalizando arquivo unificado...")

        // Finish encoding EOS
        drainEncoder(encoder, muxer, bufferInfo, audioTrackIndex, muxerStarted, true)

        try {
            encoder.stop()
            encoder.release()
            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping muxer/encoder", e)
        }

        onProgress(1.0f, clips.size, clips.size, "Unificação concluída!")
    }

    private fun decodeToPcm(inputFile: File, pcmOutputStream: BufferedOutputStream): Triple<Int, Int, Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(inputFile.absolutePath)
        } catch (e: Exception) {
            return Triple(0, 0, 0L)
        }

        var trackIndex = -1
        var format: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = trackFormat
                break
            }
        }

        if (trackIndex < 0 || format == null) {
            extractor.release()
            return Triple(0, 0, 0L)
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
        val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val info = MediaCodec.BufferInfo()
        var isExtractorEof = false
        var isDecoderEof = false
        var totalWrittenBytes = 0L

        val pcmBuffer = ByteArray(8192)

        while (!isDecoderEof) {
            if (!isExtractorEof) {
                val inIndex = decoder.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = decoder.getInputBuffer(inIndex)
                    val sampleSize = buffer?.let { extractor.readSampleData(it, 0) } ?: -1
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtractorEof = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = decoder.dequeueOutputBuffer(info, 10000)
            if (outIndex >= 0) {
                val outBuffer = decoder.getOutputBuffer(outIndex)
                if (outBuffer != null && info.size > 0) {
                    outBuffer.position(info.offset)
                    outBuffer.limit(info.offset + info.size)

                    val bytesToRead = info.size
                    var remaining = bytesToRead
                    while (remaining > 0) {
                        val chunk = Math.min(remaining, pcmBuffer.size)
                        outBuffer.get(pcmBuffer, 0, chunk)
                        pcmOutputStream.write(pcmBuffer, 0, chunk)
                        remaining -= chunk
                    }
                    totalWrittenBytes += bytesToRead
                }
                decoder.releaseOutputBuffer(outIndex, false)

                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isDecoderEof = true
                }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        return Triple(sampleRate, channels, totalWrittenBytes)
    }

    private fun decodeAndEncodeClip(
        file: File,
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        getTrackIndex: () -> Int,
        setTrackIndex: (Int) -> Unit,
        isMuxerStarted: () -> Boolean,
        setMuxerStarted: (Boolean) -> Unit,
        targetSampleRate: Int,
        targetChannels: Int,
        globalPtsUsSupplier: () -> Long,
        onPtsUpdated: (Long) -> Unit
    ) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
        } catch (e: Exception) {
            return
        }

        var trackIndex = -1
        var format: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = trackFormat
                break
            }
        }

        if (trackIndex < 0 || format == null) {
            extractor.release()
            return
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val decoderInfo = MediaCodec.BufferInfo()
        var isExtractorEof = false
        var isDecoderEof = false

        var currentPtsUs = globalPtsUsSupplier()

        while (!isDecoderEof) {
            if (!isExtractorEof) {
                val inIndex = decoder.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = decoder.getInputBuffer(inIndex)
                    val sampleSize = buffer?.let { extractor.readSampleData(it, 0) } ?: -1
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtractorEof = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = decoder.dequeueOutputBuffer(decoderInfo, 10000)
            if (outIndex >= 0) {
                val outBuffer = decoder.getOutputBuffer(outIndex)
                if (outBuffer != null && decoderInfo.size > 0) {
                    outBuffer.position(decoderInfo.offset)
                    outBuffer.limit(decoderInfo.offset + decoderInfo.size)

                    // Feed raw PCM bytes into encoder
                    val pcmBytes = ByteArray(decoderInfo.size)
                    outBuffer.get(pcmBytes)

                    var pcmOffset = 0
                    while (pcmOffset < pcmBytes.size) {
                        val encInIndex = encoder.dequeueInputBuffer(10000)
                        if (encInIndex >= 0) {
                            val encBuffer = encoder.getInputBuffer(encInIndex)
                            if (encBuffer != null) {
                                val chunk = Math.min(encBuffer.capacity(), pcmBytes.size - pcmOffset)
                                encBuffer.clear()
                                encBuffer.put(pcmBytes, pcmOffset, chunk)

                                encoder.queueInputBuffer(encInIndex, 0, chunk, currentPtsUs, 0)
                                pcmOffset += chunk

                                // Calculate chunk duration in us for 16-bit PCM (2 bytes/sample)
                                val samples = chunk / (2 * targetChannels)
                                val chunkUs = (samples * 1_000_000L) / targetSampleRate
                                currentPtsUs += chunkUs
                            }
                        }
                        drainEncoder(encoder, muxer, bufferInfo, getTrackIndex(), isMuxerStarted(), false,
                            onMuxerStart = { trackIdx ->
                                setTrackIndex(trackIdx)
                                setMuxerStarted(true)
                            })
                    }
                }
                decoder.releaseOutputBuffer(outIndex, false)

                if ((decoderInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isDecoderEof = true
                }
            }

            drainEncoder(encoder, muxer, bufferInfo, getTrackIndex(), isMuxerStarted(), false,
                onMuxerStart = { trackIdx ->
                    setTrackIndex(trackIdx)
                    setMuxerStarted(true)
                })
        }

        onPtsUpdated(currentPtsUs)
        decoder.stop()
        decoder.release()
        extractor.release()
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        trackIndex: Int,
        muxerStarted: Boolean,
        endOfStream: Boolean,
        onMuxerStart: (Int) -> Unit = {}
    ) {
        if (endOfStream) {
            val inIndex = encoder.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                encoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        }

        var currentTrackIndex = trackIndex
        var isStarted = muxerStarted

        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isStarted) {
                    Log.w(TAG, "Format changed twice")
                } else {
                    val newFormat = encoder.outputFormat
                    currentTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isStarted = true
                    onMuxerStart(currentTrackIndex)
                }
            } else if (outIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outIndex)
                if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (bufferInfo.size != 0 && isStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(currentTrackIndex, encodedData, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun writeWavFile(pcmFile: File, outputFile: File, sampleRate: Int, channels: Int, pcmDataLength: Long) {
        val byteRate = 16 * sampleRate * channels / 8
        val totalDataLen = pcmDataLength + 36

        FileOutputStream(outputFile).use { out ->
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // PCM
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * 16 / 8).toByte() // Block align
            header[33] = 0
            header[34] = 16 // Bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (pcmDataLength and 0xff).toByte()
            header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
            header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
            header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

            out.write(header)

            FileInputStream(pcmFile).use { inStream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
        }
    }

    sealed class MergeResult {
        data class Success(
            val file: File,
            val totalDurationMs: Long,
            val fileSizeBytes: Long,
            val clipCount: Int
        ) : MergeResult()

        data class Error(val message: String) : MergeResult()
    }
}
