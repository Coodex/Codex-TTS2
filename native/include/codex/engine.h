#ifndef CODEX_ENGINE_H
#define CODEX_ENGINE_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Return the engine version string.
 * The returned pointer is valid for the lifetime of the process.
 */
__attribute__((visibility("default")))
const char* codex_engine_version(void);

/**
 * Initialize the synthesis engine.
 *
 * @return 0 on success, negative error code on failure.
 */
__attribute__((visibility("default")))
int codex_engine_init(void);

/**
 * Shut down the engine and release all resources.
 */
__attribute__((visibility("default")))
void codex_engine_shutdown(void);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // CODEX_ENGINE_H
