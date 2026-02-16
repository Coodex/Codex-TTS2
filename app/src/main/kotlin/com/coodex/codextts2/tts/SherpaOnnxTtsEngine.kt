package com.coodex.codextts2.tts

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * [TtsEngine] implementation backed by sherpa-onnx VITS models.
 *
 * Each language is backed by its own [OfflineTts] instance, created lazily
 * during [initialize]. Instances are kept alive until [release] is called.
 */
class SherpaOnnxTtsEngine : TtsEngine {

    override val backendName: String = "sherpa-onnx VITS"

    private val engines = ConcurrentHashMap<TtsLanguage, OfflineTts>()
    private val states = ConcurrentHashMap<TtsLanguage, EngineState>()

    @Suppress("TooGenericExceptionCaught") // JNI layer may throw arbitrary exceptions
    override fun initialize(language: TtsLanguage, modelDir: File) {
        if (states[language] == EngineState.READY && engines.containsKey(language)) {
            return
        }

        states[language] = EngineState.LOADING
        try {
            val descriptor = descriptorFor(language)
            val config = buildConfig(descriptor, modelDir)
            val tts = OfflineTts(assetManager = null, config = config)

            engines[language]?.free()
            engines[language] = tts
            states[language] = EngineState.READY

            Log.i(TAG, "Initialized $language: sampleRate=${tts.sampleRate()}, speakers=${tts.numSpeakers()}")
        } catch (e: RuntimeException) {
            states[language] = EngineState.ERROR
            throw ModelNotAvailableException("Failed to initialize $language engine: ${e.message}", e)
        } catch (e: UnsatisfiedLinkError) {
            states[language] = EngineState.ERROR
            throw ModelNotAvailableException("Native library not available for $language: ${e.message}", e)
        }
    }

    override fun state(language: TtsLanguage): EngineState =
        states[language] ?: EngineState.NOT_INITIALIZED

    @Suppress("TooGenericExceptionCaught") // JNI layer may throw arbitrary exceptions
    override fun synthesize(
        text: String,
        language: TtsLanguage,
        speed: Float,
        speakerId: Int,
    ): SynthesisResult {
        val tts = engines[language]
            ?: throw ModelNotAvailableException("Engine not initialized for $language")

        if (states[language] != EngineState.READY) {
            throw ModelNotAvailableException("Engine not ready for $language (state=${states[language]})")
        }

        return try {
            val audio = tts.generate(text = text, sid = speakerId, speed = speed)
            SynthesisResult(samples = audio.samples, sampleRate = audio.sampleRate)
        } catch (e: RuntimeException) {
            throw SynthesisException("Synthesis failed for $language: ${e.message}", e)
        }
    }

    override fun release() {
        engines.values.forEach { it.free() }
        engines.clear()
        states.clear()
        Log.i(TAG, "All engines released")
    }

    private fun descriptorFor(language: TtsLanguage): ModelDescriptor = when (language) {
        TtsLanguage.ARABIC -> ModelManager.ARABIC_MODEL
        TtsLanguage.ENGLISH -> ModelManager.ENGLISH_MODEL
    }

    private fun buildConfig(descriptor: ModelDescriptor, modelDir: File): OfflineTtsConfig {
        val modelPath = File(modelDir, descriptor.modelFileName).absolutePath
        val tokensPath = File(modelDir, descriptor.tokensFileName).absolutePath

        val espeakDataDir = if (descriptor.dataDir.isNotEmpty()) {
            File(modelDir.parentFile, descriptor.dataDir).absolutePath
        } else {
            ""
        }

        val lexiconPath = if (descriptor.lexiconFileName.isNotEmpty()) {
            File(modelDir, descriptor.lexiconFileName).absolutePath
        } else {
            ""
        }

        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelPath,
                    lexicon = lexiconPath,
                    tokens = tokensPath,
                    dataDir = espeakDataDir,
                ),
                numThreads = NUM_THREADS,
                debug = false,
                provider = "cpu",
            ),
        )
    }

    companion object {
        private const val TAG = "SherpaOnnxTtsEngine"
        private const val NUM_THREADS = 2
    }
}
