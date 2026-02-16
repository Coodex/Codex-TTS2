package com.coodex.codextts2.tts

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages TTS model downloads and local caching.
 *
 * Models are stored under `context.filesDir / "models" / <modelName>`.
 * The espeak-ng-data directory is shared across all Piper-based models.
 */
class ModelManager(private val context: Context) {

    private val modelsRoot: File
        get() = File(context.filesDir, "models")

    /** Returns the root directory for the given model, whether or not it exists. */
    fun modelDir(descriptor: ModelDescriptor): File =
        File(modelsRoot, descriptor.dirName)

    /** Returns `true` if the model's required files are present on disk. */
    fun isModelAvailable(descriptor: ModelDescriptor): Boolean {
        val dir = modelDir(descriptor)
        return descriptor.requiredFiles.all { File(dir, it).exists() }
    }

    /**
     * Returns `true` if the shared espeak-ng-data directory is present.
     *
     * Piper-based models need this directory extracted alongside the model.
     */
    fun isEspeakDataAvailable(): Boolean {
        val espeakDir = File(modelsRoot, ESPEAK_DATA_DIR)
        return espeakDir.exists() && espeakDir.isDirectory
    }

    /** Absolute path to the espeak-ng-data directory. */
    fun espeakDataPath(): String =
        File(modelsRoot, ESPEAK_DATA_DIR).absolutePath

    /**
     * Downloads and extracts a model archive from [descriptor].url.
     *
     * @param onProgress Called with (bytesDownloaded, totalBytes). totalBytes may be -1.
     * @throws IOException on network or extraction failure.
     */
    fun downloadModel(
        descriptor: ModelDescriptor,
        onProgress: ((Long, Long) -> Unit)? = null,
    ) {
        val archiveFile = File(context.cacheDir, "${descriptor.dirName}.tar.bz2")
        try {
            downloadFile(descriptor.downloadUrl, archiveFile, onProgress)
            extractTarBz2(archiveFile, modelsRoot)
        } finally {
            archiveFile.delete()
        }
    }

    /**
     * Downloads and extracts the shared espeak-ng-data archive.
     */
    fun downloadEspeakData(onProgress: ((Long, Long) -> Unit)? = null) {
        val archiveFile = File(context.cacheDir, "espeak-ng-data.tar.bz2")
        try {
            downloadFile(ESPEAK_DATA_URL, archiveFile, onProgress)
            extractTarBz2(archiveFile, modelsRoot)
        } finally {
            archiveFile.delete()
        }
    }

    private fun downloadFile(
        urlString: String,
        destination: File,
        onProgress: ((Long, Long) -> Unit)?,
    ) {
        destination.parentFile?.mkdirs()
        val connection = openConnection(urlString)
        try {
            handleRedirects(connection, destination, onProgress)?.let { return }
            validateResponse(connection, urlString)
            writeToFile(connection, destination, onProgress)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(urlString: String): HttpURLConnection {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.connect()
        return connection
    }

    private fun handleRedirects(
        connection: HttpURLConnection,
        destination: File,
        onProgress: ((Long, Long) -> Unit)?,
    ): Unit? {
        val code = connection.responseCode
        if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM) {
            val redirectUrl = connection.getHeaderField("Location")
            connection.disconnect()
            downloadFile(redirectUrl, destination, onProgress)
            return Unit
        }
        return null
    }

    private fun validateResponse(connection: HttpURLConnection, urlString: String) {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP ${connection.responseCode} for $urlString")
        }
    }

    private fun writeToFile(
        connection: HttpURLConnection,
        destination: File,
        onProgress: ((Long, Long) -> Unit)?,
    ) {
        val totalBytes = connection.contentLengthLong
        var downloaded = 0L
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    onProgress?.invoke(downloaded, totalBytes)
                }
            }
        }
    }

    private fun extractTarBz2(archive: File, targetDir: File) {
        targetDir.mkdirs()
        val process = ProcessBuilder(
            "tar", "xjf", archive.absolutePath, "-C", targetDir.absolutePath,
        ).redirectErrorStream(true).start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.inputStream.bufferedReader().readText()
            Log.e(TAG, "tar extraction failed: $error")
            throw IOException("Failed to extract ${archive.name}: exit code $exitCode")
        }
    }

    companion object {
        private const val TAG = "ModelManager"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val BUFFER_SIZE = 8192

        private const val RELEASES_BASE =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

        private const val ESPEAK_DATA_DIR = "espeak-ng-data"
        private const val ESPEAK_DATA_URL = "$RELEASES_BASE/espeak-ng-data.tar.bz2"

        val ENGLISH_MODEL = ModelDescriptor(
            dirName = "vits-piper-en_US-amy-low",
            displayName = "English (Amy)",
            language = TtsLanguage.ENGLISH,
            downloadUrl = "$RELEASES_BASE/vits-piper-en_US-amy-low.tar.bz2",
            modelFileName = "en_US-amy-low.onnx",
            tokensFileName = "tokens.txt",
            dataDir = "espeak-ng-data",
            requiredFiles = listOf("en_US-amy-low.onnx", "tokens.txt"),
        )

        val ARABIC_MODEL = ModelDescriptor(
            dirName = "vits-piper-ar_JO-kareem-medium",
            displayName = "Arabic (Kareem)",
            language = TtsLanguage.ARABIC,
            downloadUrl = "$RELEASES_BASE/vits-piper-ar_JO-kareem-medium.tar.bz2",
            modelFileName = "ar_JO-kareem-medium.onnx",
            tokensFileName = "tokens.txt",
            dataDir = "espeak-ng-data",
            requiredFiles = listOf("ar_JO-kareem-medium.onnx", "tokens.txt"),
        )
    }
}

/**
 * Describes a downloadable TTS model.
 */
data class ModelDescriptor(
    val dirName: String,
    val displayName: String,
    val language: TtsLanguage,
    val downloadUrl: String,
    val modelFileName: String,
    val tokensFileName: String,
    val dataDir: String,
    val lexiconFileName: String = "",
    val requiredFiles: List<String>,
)
