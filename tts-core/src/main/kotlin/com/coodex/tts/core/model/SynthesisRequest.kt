package com.coodex.tts.core.model

/**
 * Immutable request for a single synthesis operation.
 *
 * @property text The UTF-8 input text to synthesize.
 * @property locale BCP-47 locale tag (e.g. "ar", "ar-EG", "ar-SA").
 * @property voiceId Identifier of the voice/model to use, or null for the default voice.
 * @property sampleRateHz Requested output sample rate in Hz. The engine may adjust this
 *   to the nearest supported rate.
 * @property pitch Pitch multiplier (1.0 = normal). Range [0.5, 2.0].
 * @property speed Speech rate multiplier (1.0 = normal). Range [0.25, 4.0].
 */
data class SynthesisRequest(
    val text: String,
    val locale: String = "ar",
    val voiceId: String? = null,
    val sampleRateHz: Int = 24000,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
)
