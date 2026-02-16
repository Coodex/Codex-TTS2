package com.coodex.codextts2.tts

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes PCM float samples to a standard 16-bit WAV file.
 *
 * The input is expected to be mono, float32 samples in the range [-1, 1].
 */
object WavWriter {

    private const val BITS_PER_SAMPLE = 16
    private const val NUM_CHANNELS = 1
    private const val HEADER_SIZE = 44

    /**
     * Writes [samples] to a WAV file at [outputFile].
     *
     * @param samples Mono float samples normalized to [-1, 1].
     * @param sampleRate Sample rate in Hz (e.g. 22050).
     * @param outputFile Destination file. Parent directory must exist.
     */
    fun write(samples: FloatArray, sampleRate: Int, outputFile: File) {
        FileOutputStream(outputFile).use { fos ->
            write(samples, sampleRate, fos)
        }
    }

    /**
     * Writes [samples] as a complete WAV stream to [output].
     *
     * @param samples Mono float samples normalized to [-1, 1].
     * @param sampleRate Sample rate in Hz.
     * @param output Destination stream. Caller is responsible for closing it.
     */
    fun write(samples: FloatArray, sampleRate: Int, output: OutputStream) {
        val pcmBytes = samplesToInt16(samples)
        val header = buildHeader(pcmBytes.size, sampleRate)
        output.write(header)
        output.write(pcmBytes)
    }

    /**
     * Builds a 44-byte RIFF/WAVE header for the given PCM data size.
     */
    internal fun buildHeader(pcmDataSize: Int, sampleRate: Int): ByteArray {
        val byteRate = sampleRate * NUM_CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = NUM_CHANNELS * BITS_PER_SAMPLE / 8

        val buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmDataSize + HEADER_SIZE - 8) // chunk size
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

        // fmt sub-chunk
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // sub-chunk size (PCM)
        buffer.putShort(1) // audio format: PCM
        buffer.putShort(NUM_CHANNELS.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(BITS_PER_SAMPLE.toShort())

        // data sub-chunk
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmDataSize)

        return buffer.array()
    }

    /**
     * Converts float samples [-1, 1] to signed 16-bit PCM little-endian bytes.
     */
    internal fun samplesToInt16(samples: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            val int16 = (clamped * Short.MAX_VALUE).toInt().toShort()
            buffer.putShort(int16)
        }
        return buffer.array()
    }
}
