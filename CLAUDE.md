# CLAUDE.md — Codex-TTS2 Project Memory

> This file is the persistent system prompt for Claude Code when operating inside this repository.
> Every instruction here is binding unless the human explicitly overrides it in-session.

---

## 1. Project Identity

**Codex-TTS2** is a production-grade, Arabic-first Text-to-Speech platform for Android.
It ships as an Android application backed by a native C++ audio/synthesis engine, bridged through JNI.

- **Organization:** Coodex
- **License:** Apache 2.0
- **Primary language target:** Modern Standard Arabic (MSA) and common Arabic dialects.
- **Platform:** Android (minSdk defined in `app/build.gradle`), with native code compiled via the Android NDK.
- **This is not a demo, prototype, or learning project.** Treat every change as shipping to real users.

---

## 2. Architectural Principles (Non-Negotiable)

### 2.1 Layered Architecture

```
┌──────────────────────────────────┐
│       Android UI (Kotlin)        │  ← Activities, Fragments, ViewModels
├──────────────────────────────────┤
│       Service / Repository       │  ← TTS service, audio session mgmt
├──────────────────────────────────┤
│         JNI Bridge Layer         │  ← Thin, auditable, no business logic
├──────────────────────────────────┤
│      Native C++ Engine           │  ← Synthesis, DSP, phoneme pipeline
└──────────────────────────────────┘
```

- **UI layer** is Kotlin-only. No C/C++ calls from UI code; everything goes through the service/repository layer.
- **JNI bridge** is a thin, mechanically auditable boundary. It converts types and forwards calls — nothing else. No allocation-heavy logic, no string manipulation, no business rules.
- **Native engine** owns synthesis, audio DSP, phoneme mapping, and performance-critical paths. It is pure C++ with no Android framework dependencies (no `<android/log.h>` in core engine headers).

### 2.2 Hard Rules

1. **No God classes.** No file over ~400 lines without justification.
2. **No circular dependencies** between modules or layers.
3. **Single Responsibility.** Every class/struct does one thing.
4. **Dependency direction is strictly downward.** Upper layers depend on lower layers, never the reverse.
5. **All public C++ APIs consumed by JNI must be C-linkage (`extern "C"`)** and documented in a header under `jni/`.
6. **No raw `new`/`delete` in C++ engine code.** Use RAII, `std::unique_ptr`, `std::shared_ptr`, or arena allocators.

---

## 3. Arabic-First TTS Constraints

Arabic is the primary and first-class language. Every design decision must account for Arabic's characteristics:

### 3.1 Text Processing Pipeline

1. **Input normalization** — Handle Unicode normalization (NFC), kashida removal, tatweel stripping, and common encoding edge cases.
2. **Diacritization** — Text may arrive undiacritized. The pipeline must either require diacritized input or include a diacritization stage. Document which.
3. **Phoneme conversion** — Arabic grapheme-to-phoneme mapping must handle:
   - Sun/moon letter assimilation (ال الشمسية / ال القمرية)
   - Hamza variants (أ إ ؤ ئ ء)
   - Taa marbuta (ة) vs. haa (ه) context
   - Nunation (tanween) rules
   - Idgham, iqlab, ikhfaa rules when Quranic/formal text is expected
4. **Right-to-left considerations** — All text processing operates on logical character order, never visual/display order. No assumptions about LTR.
5. **Mixed-language handling** — Arabic text may contain embedded Latin tokens (brand names, numbers). The pipeline must detect script boundaries and handle code-switching gracefully.

### 3.2 Audio Quality Requirements

- Output sample rate must be at least 22050 Hz (prefer 24000 Hz or higher).
- No audible clipping, pops, or DC offset in generated audio.
- Prosody must respect Arabic sentence structure (verb-subject-object patterns, iḍāfa chains, etc.).

---

## 4. Native C++ / JNI Safety Expectations

### 4.1 Memory Safety

- **Zero tolerance for memory leaks in the native layer.** Every allocation must have a clear owner and lifecycle.
- Use AddressSanitizer (ASan) and UndefinedBehaviorSanitizer (UBSan) in debug builds. CI must run native tests with sanitizers enabled.
- JNI local references must be explicitly deleted in loops. Never assume the VM's local reference table is large enough.
- `GetStringUTFChars` / `ReleaseStringUTFChars` must always be paired. Same for `GetPrimitiveArrayCritical` / `ReleasePrimitiveArrayCritical`.

### 4.2 Thread Safety

- The native engine may be called from multiple threads (e.g., synthesis + UI queries). All shared mutable state must be protected.
- Prefer lock-free structures for the audio output path. Mutexes are acceptable elsewhere but must not be held during JNI callbacks to Java.
- Never call JNI `FindClass` from a thread not attached to the JVM. Cache `jclass` and `jmethodID` during `JNI_OnLoad`.

### 4.3 Error Handling

- Native code must never crash the host process. Trap errors and propagate them as return codes or exceptions through JNI.
- Use `__attribute__((visibility("default")))` only on JNI exports. Everything else is hidden.
- No C++ exceptions across JNI boundaries. Catch before the boundary and convert to `jthrowable`.

---

## 5. Kotlin / Android Conventions

- **Language:** Kotlin for all new Android code. Java only when wrapping legacy or generated code.
- **Minimum SDK:** As defined in `app/build.gradle`. Do not lower it without discussion.
- **Architecture components:** ViewModel + StateFlow/SharedFlow for UI state. No LiveData in new code.
- **Coroutines:** Use structured concurrency. All coroutine scopes must be lifecycle-aware (`viewModelScope`, `lifecycleScope`). No `GlobalScope`.
- **Dependency injection:** Follow whatever DI framework the project adopts (Hilt/Dagger/Koin). Do not mix frameworks.
- **Resource management:** All user-visible strings in `res/values/strings.xml` (and `res/values-ar/strings.xml` for Arabic). No hardcoded strings in Kotlin code.

---

## 6. Performance Discipline

- **Audio synthesis latency** is the critical metric. Measure and guard it.
- Native hot paths (synthesis loop, DSP filters) must avoid heap allocation. Pre-allocate buffers.
- Profile before optimizing. No speculative optimization without profiler data.
- APK size matters. Strip debug symbols from release native libs. Use `minifyEnabled true` and `shrinkResources true` in release builds.
- Model/data files must be loaded lazily or streamed. Do not block `Application.onCreate` with large file reads.

---

## 7. Security

- **No secrets in the repository.** API keys, signing keystores, and credentials belong in environment variables or a secrets manager. The `.gitignore` already excludes `*.jks`, `*.keystore`, and `google-services.json` — keep it that way.
- **No `Runtime.exec()` or `ProcessBuilder`** in application code unless absolutely necessary and reviewed.
- Native code must validate all input from the JNI boundary. Treat data from Java as untrusted.
- Enable ProGuard/R8 obfuscation for release builds.
- If network features are added: enforce TLS 1.2+, certificate pinning for first-party endpoints, and `android:usesCleartextTraffic="false"`.

---

## 8. Testing Requirements

### 8.1 Test Taxonomy

| Layer | Framework | Location |
|---|---|---|
| Native C++ unit tests | Google Test (gtest) | `native/tests/` or `cpp/tests/` |
| JNI integration tests | Android Instrumented | `app/src/androidTest/` |
| Kotlin unit tests | JUnit 5 + MockK | `app/src/test/` |
| UI tests | Espresso / Compose Testing | `app/src/androidTest/` |

### 8.2 Rules

- Every public function in the native engine must have at least one unit test.
- JNI bridge functions must have integration tests that round-trip data across the boundary.
- Arabic text processing must include test cases for: bare consonants, fully diacritized text, mixed Arabic-Latin input, empty strings, and edge-case Unicode (ZWJ, ZWNJ, directional marks).
- Tests must be deterministic. No flaky tests in CI. If a test is flaky, fix it or delete it.
- Code coverage targets will be defined per-module. Never merge a PR that decreases coverage without justification.

---

## 9. CI/CD Discipline

### 9.1 Pipeline Expectations

Every pull request must pass:

1. **Kotlin lint** (`ktlint` or `detekt`) — zero warnings policy.
2. **C++ lint** (`clang-tidy`) with the project's `.clang-tidy` config.
3. **Native build** (debug + release) with ASan enabled on debug.
4. **All unit tests** (native + Kotlin).
5. **All instrumented tests** on at least one API level.
6. **Static analysis** (Android Lint with `warningsAsErrors true`).

### 9.2 Merge Rules

- No force-pushes to `main`.
- Every PR requires at least one approval.
- CI must be green before merge. No "merge and fix later."
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `refactor:`, `test:`, `ci:`, `docs:`, `chore:`.

---

## 10. Build & Project Structure (Target)

```
Codex-TTS2/
├── app/                          # Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/ or kotlin/  # Kotlin source
│   │   │   ├── jni/              # JNI bridge (C/C++ wrappers)
│   │   │   ├── cpp/              # Native engine source
│   │   │   ├── res/              # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # Kotlin unit tests
│   │   └── androidTest/          # Instrumented tests
│   ├── build.gradle.kts
│   └── CMakeLists.txt            # Native build config
├── native/                       # (Alternative) standalone native lib
│   ├── src/
│   ├── include/
│   ├── tests/
│   └── CMakeLists.txt
├── models/                       # TTS model files (git-lfs or downloaded)
├── buildSrc/                     # Shared Gradle logic
├── .github/
│   └── workflows/                # CI/CD pipelines
├── gradle/
├── build.gradle.kts              # Root build file
├── settings.gradle.kts
├── gradle.properties
├── .clang-tidy
├── .clang-format
├── .editorconfig
├── CLAUDE.md                     # This file
├── LICENSE                       # Apache 2.0
└── .gitignore
```

This structure may evolve. Update this section when it does.

---

## 11. How Claude Should Operate in This Repo

### 11.1 Before Writing Code

1. **Read first.** Never propose changes to files you haven't read. Understand context before modifying.
2. **Check the build.** If Gradle files, CMakeLists, or manifests exist, read them to understand the current project configuration.
3. **Respect what exists.** If a pattern is already established (naming, directory structure, error handling style), follow it.

### 11.2 When Writing Code

1. **Minimal, focused changes.** Do exactly what was asked. No drive-by refactors, no unsolicited "improvements."
2. **No over-engineering.** Three similar lines are better than a premature abstraction.
3. **Arabic correctness is paramount.** When touching text processing, verify Unicode handling, bidirectional safety, and diacritization rules. When in doubt, ask.
4. **JNI code must be surgical.** Every JNI function should be small enough to audit in under a minute. If it isn't, decompose it.
5. **C++ code must compile with `-Wall -Wextra -Werror`.** Do not suppress warnings without a comment explaining why.
6. **Never introduce `TODO` or `FIXME` without a linked issue or immediate follow-up.**

### 11.3 When Reasoning

1. **Think about the Arabic speaker.** Latency, pronunciation accuracy, and naturalness are user-facing quality metrics. Never trade them for developer convenience.
2. **Think about memory.** Android devices are memory-constrained. Native allocations compete with the managed heap. Be conscious of both.
3. **Think about threads.** Audio synthesis is real-time. Blocking the audio thread is a user-audible bug.
4. **Think about the boundary.** Every JNI crossing is a potential crash site. Validate inputs, pair resource calls, and handle errors before they propagate.

### 11.4 What Not to Do

- Do not add dependencies without justification. Every dependency is a liability.
- Do not create documentation files (README, CONTRIBUTING, etc.) unless explicitly asked.
- Do not commit generated files, build artifacts, or IDE configuration.
- Do not weaken `.gitignore`, ProGuard rules, or lint configurations.
- Do not use `System.loadLibrary` outside the designated initialization point.
- Do not assume English defaults anywhere in the text pipeline.

---

## 12. Updating This File

This file is a living document. Update it when:

- A new module is added to the project.
- An architectural decision is made that future work must respect.
- A convention is established that isn't captured here.
- A hard-learned lesson should be preserved.

Every update must be committed with the message prefix `docs(claude): update CLAUDE.md — <reason>`.
