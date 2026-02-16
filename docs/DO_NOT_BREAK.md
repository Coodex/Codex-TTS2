# Do Not Break Checklist

> **PR merge gate.** Every box must be checked before merging to `main`.
> Source of truth: [`CLAUDE.md`](../CLAUDE.md)

---

## Architecture

- [ ] Dependency direction is strictly downward (UI → Service → JNI → Native). No upward or circular dependencies.
- [ ] UI layer contains zero C/C++ calls — all native access goes through the service/repository layer.
- [ ] JNI bridge does type conversion and call forwarding only — no business logic, string manipulation, or heavy allocation.
- [ ] Each pipeline stage (normalization, G2P, prosody, acoustic model, vocoder) remains behind its own C++ interface. Swapping one stage does not require changes above its boundary.
- [ ] No file exceeds ~400 lines without written justification.

## Native / JNI Safety

- [ ] No raw `new`/`delete` in engine code. All ownership via RAII / smart pointers / arena allocators.
- [ ] Debug build compiles and passes with ASan + UBSan enabled.
- [ ] Every `GetStringUTFChars` is paired with `ReleaseStringUTFChars`. Same for `GetPrimitiveArrayCritical`.
- [ ] JNI local references are explicitly deleted in loops.
- [ ] No C++ exceptions cross the JNI boundary — caught and converted to `jthrowable` before the boundary.
- [ ] All JNI-consumed C++ APIs are `extern "C"` and declared in a header under `jni/`.
- [ ] No mutex held during JNI callbacks to Java. _Why: deadlocks the audio thread, causing audible glitches users cannot dismiss._
- [ ] `FindClass` / `jmethodID` lookups are cached in `JNI_OnLoad`, never called from arbitrary threads.
- [ ] All data from Java is validated at the JNI boundary (treated as untrusted input).
- [ ] C++ compiles cleanly with `-Wall -Wextra -Werror`. Suppressed warnings have a justifying comment.

## Arabic Correctness

- [ ] Text processing uses logical character order — never visual/display order. No LTR assumptions.
- [ ] Input normalization applies NFC, strips kashida/tatweel, and handles common encoding edge cases.
- [ ] G2P handles: sun/moon letter assimilation, hamza variants, taa marbuta context, tanween, gemination (shadda), emphatic spreading, and pausal forms.
- [ ] Dialectal phoneme remapping is config-driven, not hardcoded. _Why: hardcoding dialect rules prevents adding new dialects without engine changes, violating the extensibility goal._
- [ ] Mixed Arabic-Latin input detects script boundaries and code-switches gracefully.
- [ ] No English-default assumptions anywhere in the text pipeline.

## Voice & Audio Compatibility

- [ ] Output sample rate >= 22050 Hz (prefer >= 24000 Hz).
- [ ] Generated audio has no clipping, pops, or DC offset.
- [ ] Prosody respects Arabic sentence structure (VSO patterns, idafa chains not split by pauses).
- [ ] Existing voice/model packs continue to load and synthesize correctly after the change.
- [ ] `TextToSpeechService` contract is intact: `onSynthesizeText` streams PCM via `callback.start()` → `audioAvailable()` → `done()`.
- [ ] `onStop()` promptly cancels in-progress synthesis without corruption.

## Performance

- [ ] No heap allocation on the synthesis hot path (DSP loop, vocoder). Buffers are pre-allocated.
- [ ] Model/data files are loaded lazily — `Application.onCreate` is never blocked.
- [ ] Release builds use `minifyEnabled true`, `shrinkResources true`, and stripped native symbols.
- [ ] No new dependency added without written justification. _Why: each dependency increases APK size, attack surface, and build time on memory-constrained CI runners._

## Security

- [ ] No secrets (API keys, keystores, credentials) in the commit.
- [ ] `.gitignore` still excludes `*.jks`, `*.keystore`, `google-services.json` — not weakened.
- [ ] ProGuard/R8 obfuscation remains enabled for release builds.
- [ ] No `Runtime.exec()` or `ProcessBuilder` added without explicit review.
- [ ] If network code is added: TLS 1.2+, certificate pinning, `usesCleartextTraffic="false"`.

## Testing

- [ ] Every new/changed public native function has at least one unit test.
- [ ] JNI bridge changes include an integration test that round-trips data across the boundary.
- [ ] Arabic text changes include test cases for: bare consonants, fully diacritized text, mixed Arabic-Latin, empty strings, and edge-case Unicode (ZWJ, ZWNJ, directional marks).
- [ ] All tests are deterministic. No flaky tests introduced.
- [ ] Code coverage has not decreased (or decrease is justified in the PR description).

## CI / Merge Rules

- [ ] Kotlin lint (`ktlint`/`detekt`) passes with zero warnings.
- [ ] `clang-tidy` passes with the project's `.clang-tidy` config.
- [ ] Native build succeeds for both debug (ASan on) and release.
- [ ] All unit tests pass (native + Kotlin).
- [ ] All instrumented tests pass on at least one API level.
- [ ] Android Lint passes with `warningsAsErrors true`.
- [ ] Commit messages follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `ci:`, `docs:`, `chore:`).
- [ ] At least one approving review.

## Kotlin / Android

- [ ] All new code is Kotlin. Java only for legacy/generated wrappers.
- [ ] `minSdk` has not been lowered.
- [ ] UI state uses StateFlow/SharedFlow — no new LiveData. _Why: LiveData lacks backpressure and structured concurrency; mixing both creates inconsistent state delivery patterns._
- [ ] All coroutine scopes are lifecycle-aware. No `GlobalScope`.
- [ ] All user-visible strings are in `res/values/strings.xml` (+ `res/values-ar/`). None hardcoded.
- [ ] `System.loadLibrary` is called only at the single designated initialization point.
