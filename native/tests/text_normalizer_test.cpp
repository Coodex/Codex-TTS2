#include <gtest/gtest.h>
#include "codex/text_normalizer.h"

using codex::TextNormalizer;

TEST(TextNormalizerTest, EmptyInput) {
    TextNormalizer normalizer;
    EXPECT_EQ(normalizer.normalize(""), "");
}

TEST(TextNormalizerTest, StripsTatweel) {
    TextNormalizer normalizer;
    // Arabic word with tatweel (U+0640) between characters
    // U+0628 (ba) = D8 A8, U+0640 (tatweel) = D9 80, U+0631 (ra) = D8 B1
    const std::string input = "\xD8\xA8\xD9\x80\xD8\xB1"; // بـر
    const std::string expected = "\xD8\xA8\xD8\xB1";       // بر
    EXPECT_EQ(normalizer.normalize(input), expected);
}

TEST(TextNormalizerTest, PreservesNonTatweel) {
    TextNormalizer normalizer;
    const std::string input = "\xD8\xA8\xD8\xB3\xD9\x85"; // بسم
    EXPECT_EQ(normalizer.normalize(input), input);
}
