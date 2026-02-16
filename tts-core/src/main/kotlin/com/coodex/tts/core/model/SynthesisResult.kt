package com.coodex.tts.core.model

/**
 * Result of a synthesis operation.
 *
 * @property audioData Raw PCM audio data (16-bit signed, mono).
 * @property sampleRateHz Actual sample rate of the generated audio.
 * @property channelCount Number of audio channels (1 = mono).
 * @property audioBitDepth Bits per sample (typically 16).
 */
data class SynthesisResult(
    val audioData: ByteArray,
    val sampleRateHz: Int,
    val channelCount: Int = 1,
    val audioBitDepth: Int = 16,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SynthesisResult) return false
        return audioData.contentEquals(other.audioData) &&
            sampleRateHz == other.sampleRateHz &&
            channelCount == other.channelCount &&
            audioBitDepth == other.audioBitDepth
    }

    override fun hashCode(): Int {
        var result = audioData.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + channelCount
        result = 31 * result + audioBitDepth
        return result
    }
}
