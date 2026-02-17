cmake_minimum_required(VERSION 3.22.1)

project(codex-tts-native
    VERSION 0.1.0
    LANGUAGES CXX
)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Default to hidden visibility — only JNI exports are made visible.
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

# TODO: Add native engine source files when implementation begins.
#
# The library will contain the core synthesis pipeline stages:
#   - Text normalization (UTF-8, NFC, kashida removal)
#   - Grapheme-to-phoneme (Arabic G2P with assimilation rules)
#   - Prosody prediction (duration, pitch contour, stress)
#   - Acoustic model interface (mel-spectrogram generation)
#   - Vocoder / DSP (waveform synthesis from acoustic features)
#
# Example (uncomment when source files are added):
#
# add_library(codex-tts SHARED
#     src/engine.cpp
# )
#
# target_include_directories(codex-tts
#     PUBLIC  include/
#     PRIVATE src/
# )
#
# target_compile_options(codex-tts PRIVATE
#     -Wall -Wextra -Werror
# )
#
# # Link Android log library for debug builds
# find_library(log-lib log)
# target_link_libraries(codex-tts PRIVATE ${log-lib})
