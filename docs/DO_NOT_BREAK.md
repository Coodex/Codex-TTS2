# DO NOT BREAK

> **This document lists the invariants of the Codex-TTS2 project.**
> Every item here is a hard constraint. Violating any of them is a blocking issue
> that must be resolved before code is merged to `main`.
>
> Source of truth: [`CLAUDE.md`](../CLAUDE.md)

---

## 1. Layered Architecture

The dependency direction is **strictly downward**. No layer may depend on a
layer above it.

```
UI (Kotlin)  -->  Service / Repository  -->  JNI Bridge  -->  Native C++ Engine
```

| Rule | Detail |
|---|---|
| UI is Kotlin-only | No C/C++ calls from UI code. Everything goes through the service/repository layer. |
| JNI bridge is thin | Type conversion and call forwarding only. No allocation-heavy logic, no string manipulation, no business rules. |
| Native engine is pure C++ | No Android framework dependencies (`<android/log.h>`, etc.) in core engine headers. |
| No circular dependencies | Between modules or layers, at any granularity. |

---

## 2. Code Size and Structure

| Rule | Limit |
|---|---|
| Maximum file length | ~400 lines. Exceptions require written justification. |
| Single Responsibility | Every class and struct does one thing. |
| No God classes | No class accumulates unrelated responsibilities. |

---

## 3. C++ / Native Engine

### 3.1 Memory Safety

- **Zero tolerance for memory leaks.** Every allocation has a clear owner and lifecycle.
- **No raw `new`/`delete`.** Use RAII, `std::unique_ptr`, `std::shared_ptr`, or arena allocators.
- Debug builds must run with **AddressSanitizer (ASan)** and **UndefinedBehaviorSanitizer (UBSan)**.
- Hot paths (synthesis loop, DSP filters) must avoid heap allocation. Pre-allocate buffers.

### 3.2 Thread Safety

- All shared mutable state must be protected.
- Prefer **lock-free structures** for the audio output path.
- Mutexes must **not** be held during JNI callbacks to Java.
- Never call `FindClass` from a thread not attached to the JVM. Cache `jclass` and `jmethodID` during `JNI_OnLoad`.

### 3.3 Error Handling

- Native code must **never crash the host process**. Errors propagate as return codes or `jthrowable` through JNI.
- **No C++ exceptions across the JNI boundary.** Catch before the boundary and convert.
- Only JNI exports use `__attribute__((visibility("default")))`; everything else is hidden.

### 3.4 Compiler Discipline

- All C++ code must compile cleanly with **`-Wall -Wextra -Werror`**.
- Do not suppress warnings without a comment explaining why.

---

## 4. JNI Bridge

- All public C++ APIs consumed by JNI must be **C-linkage (`extern "C"`)** and documented in a header under `jni/`.
- Every JNI function must be small enough to audit in under a minute.
- `GetStringUTFChars` / `ReleaseStringUTFChars` must always be paired.
- `GetPrimitiveArrayCritical` / `ReleasePrimitiveArrayCritical` must always be paired.
- JNI local references must be explicitly deleted in loops.
- All input from Java is treated as **untrusted** and validated at the boundary.

---

## 5. Arabic Language Correctness

Arabic is the **primary and first-class language**. These invariants protect
correctness for Arabic speakers.

### 5.1 Text Processing

| Invariant | Detail |
|---|---|
| Unicode normalization | NFC normalization, kashida removal, tatweel stripping. |
| Grapheme-to-phoneme | Must handle sun/moon letter assimilation, hamza variants, taa marbuta vs. haa, nunation (tanween), idgham/iqlab/ikhfaa. |
| Logical character order | All processing uses logical order, never visual/display order. No LTR assumptions. |
| Mixed-script handling | Detect script boundaries between Arabic and Latin tokens. Handle code-switching gracefully. |
| No English defaults | Do not assume English anywhere in the text pipeline. |

### 5.2 Audio Quality

| Invariant | Threshold |
|---|---|
| Minimum sample rate | 22 050 Hz (prefer 24 000 Hz or higher) |
| No audible artifacts | No clipping, pops, or DC offset in generated audio |
| Prosody | Must respect Arabic sentence structure (VSO patterns, idafa chains) |

---

## 6. Kotlin / Android

| Rule | Detail |
|---|---|
| Language | Kotlin for all new code. Java only for wrapping legacy or generated code. |
| Minimum SDK | As defined in `app/build.gradle`. Never lower without discussion. |
| State management | ViewModel + StateFlow/SharedFlow. **No LiveData in new code.** |
| Coroutine scopes | Lifecycle-aware only (`viewModelScope`, `lifecycleScope`). **No `GlobalScope`.** |
| DI consistency | One framework. Do not mix Hilt/Dagger/Koin. |
| String resources | All user-visible strings in `res/values/strings.xml` and `res/values-ar/strings.xml`. No hardcoded strings. |
| `System.loadLibrary` | Only at the designated initialization point. Nowhere else. |
| No `Runtime.exec()` / `ProcessBuilder` | Unless absolutely necessary and reviewed. |

---

## 7. Security

| Rule | Detail |
|---|---|
| No secrets in the repo | API keys, signing keystores, credentials belong in environment variables or a secrets manager. |
| `.gitignore` coverage | `*.jks`, `*.keystore`, `google-services.json` must remain excluded. Do not weaken `.gitignore`. |
| ProGuard/R8 | Obfuscation enabled for release builds. Do not weaken ProGuard rules. |
| Network (if added) | TLS 1.2+, certificate pinning for first-party endpoints, `android:usesCleartextTraffic="false"`. |

---

## 8. Testing

### 8.1 Coverage Requirements

- Every public function in the native engine must have at least one unit test.
- JNI bridge functions must have integration tests that round-trip data across the boundary.
- PRs must not decrease code coverage without justification.

### 8.2 Arabic Test Cases (Mandatory)

Any change to Arabic text processing must include tests for:

- Bare (undiacritized) consonants
- Fully diacritized text
- Mixed Arabic-Latin input
- Empty strings
- Edge-case Unicode (ZWJ, ZWNJ, directional marks)

### 8.3 Determinism

- All tests must be deterministic. No flaky tests in CI.
- If a test is flaky, fix it or delete it.

---

## 9. CI / Merge Gates

Every pull request must pass **all** of the following before merge:

1. **Kotlin lint** (`ktlint` or `detekt`) -- zero warnings.
2. **C++ lint** (`clang-tidy`) with the project's `.clang-tidy` config.
3. **Native build** (debug + release), ASan enabled on debug.
4. **All unit tests** (native + Kotlin).
5. **All instrumented tests** on at least one API level.
6. **Static analysis** (Android Lint with `warningsAsErrors true`).

### Merge Rules

- No force-pushes to `main`.
- Every PR requires at least one approval.
- CI must be green before merge. No "merge and fix later."
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `refactor:`, `test:`, `ci:`, `docs:`, `chore:`.

---

## 10. Performance

| Rule | Detail |
|---|---|
| Audio synthesis latency | The critical metric. Measure and guard it. |
| No speculative optimization | Profile before optimizing. |
| Release native libs | Strip debug symbols. |
| Release builds | `minifyEnabled true`, `shrinkResources true`. |
| Model/data loading | Lazy or streamed. Never block `Application.onCreate`. |
| APK size | Matters. Monitor it. |

---

## 11. Repository Hygiene

| Rule | Detail |
|---|---|
| No generated files committed | Build artifacts, IDE config, and generated sources stay out of version control. |
| No `TODO`/`FIXME` without a linked issue | Or an immediate follow-up in the same PR. |
| No dependencies without justification | Every dependency is a liability. |
| Do not weaken lint configurations | Includes Android Lint, `clang-tidy`, `ktlint`/`detekt`, and ProGuard rules. |
| License | Apache 2.0. Do not change. |
