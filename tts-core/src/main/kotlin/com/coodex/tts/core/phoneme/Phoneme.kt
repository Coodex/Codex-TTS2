package com.coodex.tts.core.phoneme

/**
 * Represents a single phoneme in the Arabic phoneme inventory.
 *
 * @property symbol IPA symbol for this phoneme (e.g. "/b/", "/sˤ/").
 * @property category Broad category: CONSONANT, VOWEL, or DIPHTHONG.
 * @property isEmphatic True if this is a pharyngealized (emphatic) consonant.
 */
data class Phoneme(
    val symbol: String,
    val category: PhonemeCategory,
    val isEmphatic: Boolean = false,
)

enum class PhonemeCategory {
    CONSONANT,
    VOWEL,
    DIPHTHONG,
}
