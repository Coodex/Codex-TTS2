package com.coodex.codextts2.core

import java.io.File

/**
 * Core abstraction for text-to-speech synthesis.
 *
 * Implementations produce a WAV file from input text. The engine manages
 * its own output location (typically a cache directory provided at construction).
 */
interface TtsEngine {

    /**
     * Synthesizes [text] into a WAV audio file and returns it.
     *
     * @param text Input text (UTF-8). Must not be blank.
     * @param voiceId Optional voice identifier. When `null`, the engine uses its default voice.
     * @return A [File] pointing to the generated WAV. Caller owns the file.
     * @throws NotImplementedError if the engine is a stub with no real backend.
     * @throws IllegalArgumentException if [text] is blank.
     */
    fun synthesizeToWav(text: String, voiceId: String? = null): File
}
