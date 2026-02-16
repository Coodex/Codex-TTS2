#include "codex/engine.h"

static const char* const kVersion = "0.1.0";

const char* codex_engine_version(void) {
    return kVersion;
}

int codex_engine_init(void) {
    // Placeholder: allocate pipeline stages, load config.
    return 0;
}

void codex_engine_shutdown(void) {
    // Placeholder: release resources.
}
