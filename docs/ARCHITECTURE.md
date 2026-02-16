# Architecture

This document describes the module structure and data-flow boundaries of Codex-TTS2.

## Module Map

```
┌─────────────────────────────────────────────────────────┐
│                        :app                             │
│                  (Android Application)                  │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Activities   │  │  ViewModels  │  │   Layouts /   │  │
│  │  Fragments    │  │  StateFlow   │  │   Resources   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘  │
│         │                 │                             │
│         ▼                 ▼                             │
│  ┌─────────────────────────────────┐                    │
│  │   Service / Repository Layer    │                    │
│  │   (CodexTtsService, future)     │                    │
│  └──────────────┬──────────────────┘                    │
│                 │                                       │
│                 │ depends on                            │
│                 ▼                                       │
│  ┌─────────────────────────────────┐                    │
│  │     JNI Bridge  (app/jni/)      │                    │
│  │     Thin C wrappers only        │                    │
│  │     extern "C", no logic        │                    │
│  └──────────────┬──────────────────┘                    │
│                 │                                       │
└─────────────────┼───────────────────────────────────────┘
                  │ links to
                  ▼
┌─────────────────────────────────────────────────────────┐
│                      :native                            │
│               (C++ Engine — static lib)                 │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │                include/codex/                     │   │
│  │   engine.h   text_normalizer.h   g2p.h   ...     │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │                    src/                           │   │
│  │   engine.cpp   text_normalizer.cpp   g2p.cpp     │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │                   tests/                          │   │
│  │   engine_test.cpp   text_normalizer_test.cpp      │   │
│  │   (Google Test, desktop only)                     │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                     :tts-core                           │
│           (Kotlin library — domain layer)               │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  core/engine/                                     │   │
│  │    TtsEngine (interface)                          │   │
│  │    LanguageSupport (enum)                         │   │
│  ├──────────────────────────────────────────────────┤   │
│  │  core/model/                                      │   │
│  │    SynthesisRequest                               │   │
│  │    SynthesisResult                                │   │
│  │    VoiceInfo                                      │   │
│  ├──────────────────────────────────────────────────┤   │
│  │  core/phoneme/                                    │   │
│  │    Phoneme, PhonemeCategory                       │   │
│  │    TextNormalizer (interface)                      │   │
│  │    GraphemeToPhoneme (interface)                   │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Dependency Direction

```
:app  ──depends on──▶  :tts-core
:app  ──links to────▶  :native  (via CMake / JNI)
```

Dependencies flow **strictly downward**. No module depends upward.

- `:app` is the only Android application module. It owns the UI, the `TextToSpeechService`
  implementation, and the JNI bridge layer.
- `:tts-core` is a pure Kotlin Android library that defines domain types (requests, results,
  voice metadata) and interfaces (`TtsEngine`, `TextNormalizer`, `GraphemeToPhoneme`). It has
  **no native code** and **no Android UI**. The app module provides concrete implementations
  that delegate to native code.
- `:native` is a standalone C++ library built by CMake. It is compiled into the app's `.so`
  through the app module's `externalNativeBuild` configuration. It can also be built and tested
  independently on a desktop host for fast iteration.

## Synthesis Pipeline (Data Flow)

```
Input Text (UTF-8)
    │
    ▼
┌─────────────────────┐
│  Text Normalization  │  NFC, kashida strip, numeral expansion
├─────────────────────┤
│  Diacritization      │  Add missing tashkeel (future)
├─────────────────────┤
│  Grapheme-to-Phoneme │  Arabic G2P with assimilation rules
├─────────────────────┤
│  Prosody Prediction  │  Duration, pitch, stress (future)
├─────────────────────┤
│  Acoustic Model      │  Mel-spectrogram generation (future)
├─────────────────────┤
│  Vocoder / DSP       │  Waveform synthesis (future)
└─────────────────────┘
    │
    ▼
PCM Audio Output (16-bit, mono, >= 22050 Hz)
```

Each stage is a replaceable C++ component behind an interface in `native/include/codex/`.
Stages marked **(future)** are not yet implemented; their interfaces will be added as
development proceeds.

## JNI Boundary Rules

The JNI bridge in `app/src/main/jni/` must:

1. Contain **only** type conversion and call forwarding — no business logic.
2. Export symbols with `extern "C"` and `__attribute__((visibility("default")))`.
3. Pair every `GetStringUTFChars` with `ReleaseStringUTFChars`.
4. Delete local references in loops.
5. Catch all C++ exceptions before the boundary and convert to Java exceptions.
6. Cache `jclass` and `jmethodID` in `JNI_OnLoad`, never look them up from arbitrary threads.

## Build System

- **Gradle Kotlin DSL** for Android modules (`:app`, `:tts-core`).
- **CMake 3.22.1+** for native C++ code, integrated via `app/build.gradle.kts` `externalNativeBuild`.
- **Version catalog** at `gradle/libs.versions.toml` — all dependency versions pinned, no `+` ranges.
- Desktop native tests built with `cmake -DCODEX_BUILD_TESTS=ON` and run via `ctest`.
