package com.ashasaathi.service.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val modelName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isDone: Boolean = false,
    val error: String? = null
) {
    val percent: Int get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
}

@Singleton
class ModelDownloadService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Whisper tiny multilingual quantised — ~75 MB
        private const val WHISPER_FILE = "ggml-tiny-q5_1.bin"
        private const val WHISPER_URL  =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin"

        // TinyLlama 1.1B Q4_0 — ~550 MB
        private const val LLAMA_FILE = "ggml-tinyllama-1.1b-chat-q4_0.gguf"
        private const val LLAMA_URL  =
            "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_0.gguf"
    }

    fun isWhisperReady(): Boolean = File(context.filesDir, WHISPER_FILE).exists()
    fun isLlamaReady():   Boolean = File(context.filesDir, LLAMA_FILE).exists()

    fun downloadWhisper(): Flow<DownloadProgress> =
        downloadFile(WHISPER_FILE, WHISPER_URL)

    fun downloadLlama(): Flow<DownloadProgress> =
        downloadFile(LLAMA_FILE, LLAMA_URL)

    private fun downloadFile(name: String, urlStr: String): Flow<DownloadProgress> = channelFlow {
        val dest = File(context.filesDir, name)
        if (dest.exists()) {
            send(DownloadProgress(name, dest.length(), dest.length(), isDone = true))
            return@channelFlow
        }

        val tmp = File(context.filesDir, "$name.tmp")
        runCatching {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout    = 60_000
            conn.connect()

            val total = conn.contentLengthLong
            var downloaded = 0L

            conn.inputStream.use { inp ->
                FileOutputStream(tmp).use { out ->
                    val buf = ByteArray(128 * 1024)
                    var n: Int
                    while (inp.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n)
                        downloaded += n
                        send(DownloadProgress(name, downloaded, total))
                    }
                }
            }
            tmp.renameTo(dest)
            send(DownloadProgress(name, dest.length(), dest.length(), isDone = true))
        }.onFailure { e ->
            tmp.delete()
            send(DownloadProgress(name, 0, 0, error = e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)
}
