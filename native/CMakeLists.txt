cmake_minimum_required(VERSION 3.22.1)
project("codex_tts_engine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_compile_options(-Wall -Wextra -Werror)

# Engine is a static library consumed by the app's shared JNI lib.
# When built standalone (for desktop tests), it produces a static archive.

file(GLOB_RECURSE ENGINE_SOURCES "src/*.cpp")

add_library(codex_tts_engine STATIC ${ENGINE_SOURCES})

target_include_directories(codex_tts_engine PUBLIC
    "${CMAKE_CURRENT_SOURCE_DIR}/include"
)

# --- Tests (desktop only, not cross-compiled for Android) ---
option(CODEX_BUILD_TESTS "Build native unit tests" OFF)

if(CODEX_BUILD_TESTS)
    enable_testing()

    # Expect googletest to be available (e.g. via FetchContent or system install)
    include(FetchContent)
    FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.15.2
    )
    FetchContent_MakeAvailable(googletest)

    file(GLOB TEST_SOURCES "tests/*.cpp")
    add_executable(codex_tts_tests ${TEST_SOURCES})
    target_link_libraries(codex_tts_tests PRIVATE codex_tts_engine GTest::gtest_main)

    include(GoogleTest)
    gtest_discover_tests(codex_tts_tests)
endif()
