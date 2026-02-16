cmake_minimum_required(VERSION 3.22.1)
project("codex_tts" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Strict warnings for all builds
add_compile_options(-Wall -Wextra -Werror)

# Hide all symbols by default; only JNI exports are visible
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_C_VISIBILITY_PRESET hidden)

# Path to the standalone native engine module
set(NATIVE_ENGINE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../native")

# Collect JNI bridge sources (thin wrappers only)
file(GLOB JNI_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/../jni/*.cpp"
)

# Collect native engine sources
file(GLOB_RECURSE ENGINE_SOURCES
    "${NATIVE_ENGINE_DIR}/src/*.cpp"
)

add_library(codex_tts SHARED
    ${JNI_SOURCES}
    ${ENGINE_SOURCES}
)

target_include_directories(codex_tts PRIVATE
    "${NATIVE_ENGINE_DIR}/include"
    "${CMAKE_CURRENT_SOURCE_DIR}/../jni"
)

# Link Android log library (for JNI bridge logging only, not core engine)
find_library(log-lib log)
target_link_libraries(codex_tts ${log-lib})

# Address Sanitizer and UBSan for debug builds
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(codex_tts PRIVATE -fsanitize=address,undefined -fno-omit-frame-pointer)
    target_link_options(codex_tts PRIVATE -fsanitize=address,undefined)
endif()
