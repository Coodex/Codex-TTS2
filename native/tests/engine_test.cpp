#include <gtest/gtest.h>
#include "codex/engine.h"

TEST(EngineTest, VersionIsNotNull) {
    const char* version = codex_engine_version();
    ASSERT_NE(version, nullptr);
    EXPECT_STRNE(version, "");
}

TEST(EngineTest, InitAndShutdown) {
    EXPECT_EQ(codex_engine_init(), 0);
    codex_engine_shutdown();
}
