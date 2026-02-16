#include "codex/text_normalizer.h"

#include <algorithm>

namespace codex {

std::string TextNormalizer::normalize(std::string_view input) const {
    std::string result;
    result.reserve(input.size());

    // Minimal skeleton: copy input, stripping tatweel (U+0640 = 0xD9 0x80 in UTF-8).
    for (size_t i = 0; i < input.size(); ++i) {
        // Tatweel is UTF-8 bytes 0xD9 0x80
        if (i + 1 < input.size() &&
            static_cast<unsigned char>(input[i]) == 0xD9 &&
            static_cast<unsigned char>(input[i + 1]) == 0x80) {
            ++i; // skip both bytes
            continue;
        }
        result.push_back(input[i]);
    }

    return result;
}

} // namespace codex
