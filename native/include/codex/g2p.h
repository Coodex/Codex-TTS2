#ifndef CODEX_G2P_H
#define CODEX_G2P_H

#include <string>
#include <string_view>
#include <vector>

namespace codex {

/** Represents a single phoneme with its IPA symbol and duration hint. */
struct PhonemeToken {
    std::string symbol;   // IPA symbol (e.g. "b", "sˤ", "aː")
    bool is_emphatic = false;
    float duration_hint = 0.0f; // 0 = use default duration
};

/**
 * Grapheme-to-phoneme converter for Arabic text.
 *
 * Handles:
 * - Sun/moon letter assimilation
 * - Hamza variants
 * - Taa marbuta context
 * - Tanween (nunation)
 * - Gemination (shadda)
 * - Emphatic spreading
 * - Pausal forms
 */
class GraphemeToPhoneme {
public:
    GraphemeToPhoneme() = default;
    ~GraphemeToPhoneme() = default;

    GraphemeToPhoneme(const GraphemeToPhoneme&) = delete;
    GraphemeToPhoneme& operator=(const GraphemeToPhoneme&) = delete;

    /**
     * Convert normalized Arabic text to a phoneme sequence.
     *
     * @param text Normalized UTF-8 text (post-TextNormalizer).
     * @return Ordered vector of phoneme tokens.
     */
    std::vector<PhonemeToken> convert(std::string_view text) const;
};

} // namespace codex

#endif // CODEX_G2P_H
