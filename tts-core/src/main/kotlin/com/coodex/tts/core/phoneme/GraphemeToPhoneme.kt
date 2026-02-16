package com.coodex.tts.core.phoneme

/**
 * Contract for grapheme-to-phoneme conversion.
 *
 * Implementations must handle Arabic-specific rules:
 * - Sun/moon letter assimilation
 * - Hamza variants
 * - Taa marbuta context
 * - Tanween (nunation)
 * - Gemination (shadda)
 * - Emphatic spreading
 * - Pausal forms
 */
interface GraphemeToPhoneme {

    /**
     * Convert normalized text to a sequence of phonemes.
     *
     * @param text Normalized input text (output of [TextNormalizer]).
     * @return Ordered list of phonemes representing the pronunciation.
     */
    fun convert(text: String): List<Phoneme>
}
