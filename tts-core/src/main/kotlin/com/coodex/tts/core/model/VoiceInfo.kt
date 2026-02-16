package com.coodex.tts.core.model

/**
 * Metadata describing an available TTS voice.
 *
 * @property id Unique identifier for this voice.
 * @property name Human-readable display name.
 * @property locale BCP-47 locale tag this voice supports (e.g. "ar-EG").
 * @property sampleRateHz Native sample rate of the voice model.
 * @property quality Quality tier (higher = better). Used by the Android framework for voice ranking.
 */
data class VoiceInfo(
    val id: String,
    val name: String,
    val locale: String,
    val sampleRateHz: Int,
    val quality: Int = 300,
)
