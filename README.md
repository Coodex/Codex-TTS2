# Codex-TTS2

Arabic-first Text-to-Speech engine for Android.

## Goals

- **Natural Arabic speech** — Produce synthesis that native Arabic speakers find acceptable for daily use, with correct pronunciation and natural prosody.
- **Offline-first** — Core synthesis works entirely offline with no network dependency. Target sub-200 ms time-to-first-audio on mid-range devices.
- **System TTS integration** — Ship as a standard Android `TextToSpeechService` so any app can use it as the device's TTS engine.
- **MSA + dialect support** — Modern Standard Arabic as the baseline, with pluggable voice/model packs for Egyptian, Levantine, Gulf, and Maghrebi variants.
- **Low footprint** — Runs well on memory-constrained Android devices. APK size and RAM usage are first-class constraints.

## Non-Goals

- This is not a general-purpose multilingual TTS engine. Arabic is the primary and first-class language.
- This is not a cloud service or API. The engine runs on-device.
- This project does not include pre-trained model weights in the repository. Models are loaded at runtime from app assets or downloaded separately.
- No streaming/real-time voice cloning or voice conversion.

## Project Structure

```
Codex-TTS2/
├── app/                     Android application module
│   ├── src/main/
│   │   ├── kotlin/          Kotlin source (UI, service, repository)
│   │   ├── jni/             JNI bridge (thin C wrappers)
│   │   ├── cpp/             CMake config for native build
│   │   └── res/             Android resources
│   └── build.gradle.kts
├── tts-core/                Kotlin library — domain types & interfaces
│   ├── src/main/kotlin/     TtsEngine, Phoneme, TextNormalizer, etc.
│   └── build.gradle.kts
├── native/                  C++ synthesis engine
│   ├── include/codex/       Public headers
│   ├── src/                 Implementation
│   ├── tests/               Google Test unit tests
│   └── CMakeLists.txt
├── docs/                    Architecture and project docs
├── gradle/                  Wrapper + version catalog
├── build.gradle.kts         Root build file
└── settings.gradle.kts
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for module boundaries and data-flow diagrams.

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17+**
- **Android SDK** with compileSdk 35
- **Android NDK** (installed via SDK Manager; CMake 3.22.1+)

## Build

### Android (full project)

```bash
./gradlew assembleDebug
```

### Native tests (desktop, no Android SDK needed)

```bash
cd native
cmake -B build -DCODEX_BUILD_TESTS=ON
cmake --build build
ctest --test-dir build --output-on-failure
```

## License

Apache 2.0 — see [LICENSE](LICENSE).
