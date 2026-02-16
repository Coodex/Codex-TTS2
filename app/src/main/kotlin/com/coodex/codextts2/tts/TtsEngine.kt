package com.coodex.codextts2.tts

import java.io.File

/**
 * Result of a synthesis operation.
 *
 * @property samples PCM audio samples normalized to [-1, 1].
 * @property sampleRate Sample rate in Hz.
 */
data class SynthesisResult(
    val samples: FloatArray,
    val sampleRate: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SynthesisResult) return false
        return sampleRate == other.sampleRate && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int = 31 * samples.contentHashCode() + sampleRate
}

/** Language identifiers supported by the engine. */
enum class TtsLanguage(val iso639: String) {
    ARABIC("ara"),
    ENGLISH("eng"),
}

/** Current readiness state of the engine for a given language. */
enum class EngineState {
    NOT_INITIALIZED,
    MODEL_MISSING,
    LOADING,
    READY,
    ERROR,
}

/**
 * Clean abstraction over a text-to-speech backend.
 *
 * Implementations must be safe to call from any thread; however, only one
 * [synthesize] call is active at a time (callers serialize access).
 */
interface TtsEngine {

    /** Human-readable name of the underlying backend (e.g. "sherpa-onnx VITS"). */
    val backendName: String

    /**
     * Initializes the engine for [language].
     *
     * The model files at [modelDir] must already exist on disk. If they are
     * missing, this method must throw [ModelNotAvailableException].
     *
     * This method is idempotent: calling it twice with the same language is a no-op
     * as long as the engine is already [EngineState.READY].
     */
    fun initialize(language: TtsLanguage, modelDir: File)

    /** Returns the current state of the engine for the given language. */
    fun state(language: TtsLanguage): EngineState

    /**
     * Synthesizes [text] and returns raw PCM audio.
     *
     * @param text Input text (UTF-8). Must not be empty.
     * @param language Target language.
     * @param speed Playback speed multiplier (1.0 = normal).
     * @param speakerId Speaker index for multi-speaker models (default 0).
     * @throws ModelNotAvailableException if the engine has not been initialized for [language].
     * @throws SynthesisException on any synthesis failure.
     */
    fun synthesize(
        text: String,
        language: TtsLanguage,
        speed: Float = 1.0f,
        speakerId: Int = 0,
    ): SynthesisResult

    /** Releases all resources held by the engine. Safe to call multiple times. */
    fun release()
}

class ModelNotAvailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class SynthesisException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
