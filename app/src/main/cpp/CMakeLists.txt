cmake_minimum_required(VERSION 3.22.1)
project(ashasaathi_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── Whisper.cpp ────────────────────────────────────────────────────────────────
# Clone whisper.cpp into app/src/main/cpp/whisper.cpp/ as a git submodule:
#   git submodule add https://github.com/ggerganov/whisper.cpp app/src/main/cpp/whisper.cpp
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)
if(EXISTS ${WHISPER_DIR}/whisper.h)
    set(WHISPER_SOURCES
        ${WHISPER_DIR}/whisper.cpp
        ${WHISPER_DIR}/ggml.c
        ${WHISPER_DIR}/ggml-alloc.c
        ${WHISPER_DIR}/ggml-backend.c
        ${WHISPER_DIR}/ggml-quants.c
    )
    add_library(whisper STATIC ${WHISPER_SOURCES})
    target_include_directories(whisper PUBLIC ${WHISPER_DIR})
    set(HAVE_WHISPER TRUE)
else()
    message(WARNING "whisper.cpp not found — voice transcription disabled")
    set(HAVE_WHISPER FALSE)
endif()

# ── llama.cpp ──────────────────────────────────────────────────────────────────
# Clone llama.cpp into app/src/main/cpp/llama.cpp/ as a git submodule:
#   git submodule add https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
if(EXISTS ${LLAMA_DIR}/llama.h)
    set(LLAMA_SOURCES
        ${LLAMA_DIR}/llama.cpp
        ${LLAMA_DIR}/ggml.c
        ${LLAMA_DIR}/ggml-alloc.c
        ${LLAMA_DIR}/ggml-backend.c
        ${LLAMA_DIR}/ggml-quants.c
    )
    add_library(llama STATIC ${LLAMA_SOURCES})
    target_include_directories(llama PUBLIC ${LLAMA_DIR})
    set(HAVE_LLAMA TRUE)
else()
    message(WARNING "llama.cpp not found — AI extraction disabled, falling back to keyword rules")
    set(HAVE_LLAMA FALSE)
endif()

# ── JNI bridge ────────────────────────────────────────────────────────────────
find_library(log-lib log)
find_library(android-lib android)

set(JNI_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper_jni.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/llama_jni.cpp
)

add_library(ashasaathi_jni SHARED ${JNI_SOURCES})

target_include_directories(ashasaathi_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(HAVE_WHISPER)
    target_include_directories(ashasaathi_jni PRIVATE ${WHISPER_DIR})
    target_compile_definitions(ashasaathi_jni PRIVATE HAVE_WHISPER=1)
    target_link_libraries(ashasaathi_jni whisper)
endif()

if(HAVE_LLAMA)
    target_include_directories(ashasaathi_jni PRIVATE ${LLAMA_DIR})
    target_compile_definitions(ashasaathi_jni PRIVATE HAVE_LLAMA=1)
    target_link_libraries(ashasaathi_jni llama)
endif()

target_link_libraries(ashasaathi_jni
    ${log-lib}
    ${android-lib}
)

# 16 KB page-size alignment — required for Google Play (Android 15+)
target_link_options(ashasaathi_jni PRIVATE "-Wl,-z,max-page-size=16384")
