package com.coodex.tts.core.phoneme

/**
 * Contract for Arabic text normalization.
 *
 * Implementations apply NFC normalization, strip kashida/tatweel,
 * expand numerals, and handle common encoding edge cases before
 * text enters the G2P stage.
 */
interface TextNormalizer {

    /**
     * Normalize the given Arabic (or mixed-script) text.
     *
     * @param text Raw UTF-8 input text.
     * @return Normalized text ready for downstream processing.
     */
    fun normalize(text: String): String
}
