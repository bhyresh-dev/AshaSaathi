#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef HAVE_WHISPER
#include "whisper.cpp/whisper.h"

static whisper_context* g_ctx = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ashasaathi_service_ai_WhisperService_nativeLoadModel(JNIEnv* env, jobject, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading whisper model: %s", path);

    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    struct whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    g_ctx = whisper_init_from_file_with_params(path, params);

    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_ctx) {
        LOGE("Failed to load whisper model");
        return JNI_FALSE;
    }
    LOGI("Whisper model loaded successfully");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ashasaathi_service_ai_WhisperService_nativeTranscribe(
        JNIEnv* env, jobject, jfloatArray pcmData, jint sampleRate) {
    if (!g_ctx) {
        return env->NewStringUTF("[error: model not loaded]");
    }

    jfloat* samples = env->GetFloatArrayElements(pcmData, nullptr);
    jsize nSamples = env->GetArrayLength(pcmData);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress   = false;
    params.print_realtime   = false;
    params.print_timestamps = false;
    params.language         = "auto";
    params.n_threads        = 4;
    params.single_segment   = false;
    params.translate        = false;
    params.no_context       = true;

    int result = whisper_full(g_ctx, params, samples, nSamples);
    env->ReleaseFloatArrayElements(pcmData, samples, JNI_ABORT);

    if (result != 0) {
        LOGE("whisper_full returned error: %d", result);
        return env->NewStringUTF("");
    }

    const int nSegments = whisper_full_n_segments(g_ctx);
    std::string transcript;
    for (int i = 0; i < nSegments; ++i) {
        const char* text = whisper_full_get_segment_text(g_ctx, i);
        if (text) transcript += text;
    }

    // Trim leading/trailing whitespace
    while (!transcript.empty() && transcript.front() == ' ') transcript.erase(transcript.begin());

    LOGI("Transcription: %s", transcript.c_str());
    return env->NewStringUTF(transcript.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashasaathi_service_ai_WhisperService_nativeFreeModel(JNIEnv*, jobject) {
    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
        LOGI("Whisper model freed");
    }
}

#else // HAVE_WHISPER not defined — stub implementation

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ashasaathi_service_ai_WhisperService_nativeLoadModel(JNIEnv*, jobject, jstring) {
    LOGE("Whisper not compiled — stub returns false");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ashasaathi_service_ai_WhisperService_nativeTranscribe(JNIEnv* env, jobject, jfloatArray, jint) {
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashasaathi_service_ai_WhisperService_nativeFreeModel(JNIEnv*, jobject) {}

#endif
