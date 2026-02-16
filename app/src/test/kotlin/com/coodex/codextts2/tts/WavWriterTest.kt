package com.coodex.codextts2.tts

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterTest {

    @Test
    fun `header has correct size`() {
        val header = WavWriter.buildHeader(pcmDataSize = 0, sampleRate = 22050)
        assertEquals(44, header.size)
    }

    @Test
    fun `header starts with RIFF WAVE`() {
        val header = WavWriter.buildHeader(pcmDataSize = 100, sampleRate = 22050)
        val riff = String(header, 0, 4, Charsets.US_ASCII)
        val wave = String(header, 8, 4, Charsets.US_ASCII)
        assertEquals("RIFF", riff)
        assertEquals("WAVE", wave)
    }

    @Test
    fun `header encodes sample rate correctly`() {
        val sampleRate = 24000
        val header = WavWriter.buildHeader(pcmDataSize = 0, sampleRate = sampleRate)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        // Sample rate is at byte offset 24
        val encoded = buf.getInt(24)
        assertEquals(sampleRate, encoded)
    }

    @Test
    fun `header chunk size equals data size plus 36`() {
        val dataSize = 1000
        val header = WavWriter.buildHeader(pcmDataSize = dataSize, sampleRate = 22050)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        // Chunk size at offset 4 = dataSize + 44 - 8 = dataSize + 36
        val chunkSize = buf.getInt(4)
        assertEquals(dataSize + 36, chunkSize)
    }

    @Test
    fun `header data sub-chunk size matches pcm data size`() {
        val dataSize = 2048
        val header = WavWriter.buildHeader(pcmDataSize = dataSize, sampleRate = 22050)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        // data sub-chunk size at offset 40
        val subChunkSize = buf.getInt(40)
        assertEquals(dataSize, subChunkSize)
    }

    @Test
    fun `samplesToInt16 converts silence to zeros`() {
        val silence = FloatArray(10) { 0.0f }
        val pcm = WavWriter.samplesToInt16(silence)
        assertEquals(20, pcm.size)
        for (byte in pcm) {
            assertEquals(0.toByte(), byte)
        }
    }

    @Test
    fun `samplesToInt16 clamps values outside range`() {
        val samples = floatArrayOf(-2.0f, 2.0f)
        val pcm = WavWriter.samplesToInt16(samples)
        val buf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(Short.MIN_VALUE.toInt() + 1, buf.getShort(0).toInt())
        assertEquals(Short.MAX_VALUE.toInt(), buf.getShort(2).toInt())
    }

    @Test
    fun `samplesToInt16 converts positive peak correctly`() {
        val samples = floatArrayOf(1.0f)
        val pcm = WavWriter.samplesToInt16(samples)
        val buf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(Short.MAX_VALUE.toInt(), buf.getShort(0).toInt())
    }

    @Test
    fun `write produces valid WAV output`() {
        val samples = floatArrayOf(0.0f, 0.5f, -0.5f, 1.0f)
        val sampleRate = 22050
        val output = ByteArrayOutputStream()

        WavWriter.write(samples, sampleRate, output)

        val bytes = output.toByteArray()
        // 44 byte header + 4 samples * 2 bytes = 52 bytes total
        assertEquals(52, bytes.size)

        // Verify RIFF header
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
    }

    @Test
    fun `write handles empty sample array`() {
        val output = ByteArrayOutputStream()
        WavWriter.write(floatArrayOf(), 22050, output)
        // Should produce a valid 44-byte header with 0 data
        assertEquals(44, output.toByteArray().size)
    }
}
