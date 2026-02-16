package com.coodex.tts.core.engine

import com.coodex.tts.core.model.SynthesisRequest
import com.coodex.tts.core.model.SynthesisResult
import com.coodex.tts.core.model.VoiceInfo

/**
 * Core contract for a TTS synthesis engine.
 *
 * Implementations bridge to the native C++ layer via JNI.
 * All methods may be called from worker threads; implementations must be thread-safe.
 */
interface TtsEngine {

    /** Initialize the engine. Must be called before [synthesize]. */
    fun initialize()

    /** Release all resources held by the engine. */
    fun release()

    /** List available voices. */
    fun availableVoices(): List<VoiceInfo>

    /**
     * Check whether the given locale is supported.
     *
     * @return One of [LanguageSupport] values.
     */
    fun isLanguageAvailable(locale: String): LanguageSupport

    /** Perform synchronous synthesis and return the generated audio. */
    fun synthesize(request: SynthesisRequest): SynthesisResult

    /** Cancel any in-progress synthesis. Thread-safe. */
    fun stop()
}
