#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef HAVE_LLAMA
#include "llama.cpp/llama.h"

static llama_model*   g_model   = nullptr;
static llama_context* g_lctx    = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ashasaathi_service_ai_LlamaService_nativeLoadModel(JNIEnv* env, jobject, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading llama model: %s", path);

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU only on Android

    g_model = llama_load_model_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        LOGE("Failed to load llama model");
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx    = 512;
    cparams.n_batch  = 512;
    cparams.n_threads = 4;

    g_lctx = llama_new_context_with_model(g_model, cparams);
    if (!g_lctx) {
        llama_free_model(g_model);
        g_model = nullptr;
        LOGE("Failed to create llama context");
        return JNI_FALSE;
    }

    LOGI("Llama model loaded");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ashasaathi_service_ai_LlamaService_nativeGenerate(
        JNIEnv* env, jobject, jstring promptStr, jint maxTokens) {

    if (!g_model || !g_lctx) {
        return env->NewStringUTF("");
    }

    const char* prompt = env->GetStringUTFChars(promptStr, nullptr);

    std::vector<llama_token> tokens(1024);
    int nTokens = llama_tokenize(g_model, prompt, strlen(prompt), tokens.data(), tokens.size(), true, false);
    env->ReleaseStringUTFChars(promptStr, prompt);

    if (nTokens < 0) {
        LOGE("Tokenization failed");
        return env->NewStringUTF("");
    }
    tokens.resize(nTokens);

    llama_kv_cache_clear(g_lctx);

    // Decode input tokens
    llama_batch batch = llama_batch_init(512, 0, 1);
    for (int i = 0; i < nTokens; i++) {
        batch.token[i]    = tokens[i];
        batch.pos[i]      = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i]   = (i == nTokens - 1) ? 1 : 0;
    }
    batch.n_tokens = nTokens;
    llama_decode(g_lctx, batch);
    llama_batch_free(batch);

    std::string output;
    int curPos = nTokens;

    for (int i = 0; i < maxTokens; i++) {
        const float* logits = llama_get_logits_ith(g_lctx, -1);
        llama_token newToken = llama_sample_token_greedy(g_lctx, nullptr);

        if (newToken == llama_token_eos(g_model)) break;

        char buf[256];
        int n = llama_token_to_piece(g_model, newToken, buf, sizeof(buf), false);
        if (n > 0) output.append(buf, n);

        llama_batch single = llama_batch_init(1, 0, 1);
        single.token[0]    = newToken;
        single.pos[0]      = curPos++;
        single.n_seq_id[0] = 1;
        single.seq_id[0][0] = 0;
        single.logits[0]   = 1;
        single.n_tokens    = 1;
        llama_decode(g_lctx, single);
        llama_batch_free(single);

        // Stop at closing brace of JSON
        if (output.find('}') != std::string::npos) break;
    }

    LOGI("Llama output: %s", output.c_str());
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashasaathi_service_ai_LlamaService_nativeFreeModel(JNIEnv*, jobject) {
    if (g_lctx) { llama_free(g_lctx); g_lctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    LOGI("Llama freed");
}

#else

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ashasaathi_service_ai_LlamaService_nativeLoadModel(JNIEnv*, jobject, jstring) {
    return JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ashasaathi_service_ai_LlamaService_nativeGenerate(JNIEnv* env, jobject, jstring, jint) {
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ashasaathi_service_ai_LlamaService_nativeFreeModel(JNIEnv*, jobject) {}

#endif
