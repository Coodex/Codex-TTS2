#ifndef CODEX_TEXT_NORMALIZER_H
#define CODEX_TEXT_NORMALIZER_H

#include <string>
#include <string_view>

namespace codex {

/**
 * Normalizes Arabic text prior to G2P processing.
 *
 * Responsibilities:
 * - Unicode NFC normalization
 * - Kashida (tatweel U+0640) removal
 * - Common encoding fixups
 * - Numeral expansion (future)
 */
class TextNormalizer {
public:
    TextNormalizer() = default;
    ~TextNormalizer() = default;

    TextNormalizer(const TextNormalizer&) = delete;
    TextNormalizer& operator=(const TextNormalizer&) = delete;

    /**
     * Normalize the given UTF-8 input text.
     *
     * @param input Raw UTF-8 text.
     * @return Normalized UTF-8 text.
     */
    std::string normalize(std::string_view input) const;
};

} // namespace codex

#endif // CODEX_TEXT_NORMALIZER_H
