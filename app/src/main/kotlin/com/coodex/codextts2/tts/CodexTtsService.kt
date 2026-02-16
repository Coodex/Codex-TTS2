package com.coodex.codextts2.tts

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * System-level [TextToSpeechService] exposing Codex-TTS2 to all apps on the device.
 *
 * This is a minimal skeleton that delegates to [SherpaOnnxTtsEngine].
 * Full voice management, async model loading, and concurrent handling
 * will be built out in subsequent iterations.
 */
class CodexTtsService : TextToSpeechService() {

    private var engine: SherpaOnnxTtsEngine? = null
    private lateinit var modelManager: ModelManager

    override fun onCreate() {
        super.onCreate()
        modelManager = ModelManager(this)
        engine = SherpaOnnxTtsEngine()
        initializeAvailableLanguages()
    }

    override fun onDestroy() {
        engine?.release()
        engine = null
        super.onDestroy()
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return when {
            lang == "ara" || lang == "ar" -> TextToSpeech.LANG_AVAILABLE
            lang == "eng" || lang == "en" -> TextToSpeech.LANG_AVAILABLE
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetLanguage(): Array<String> = arrayOf("ar", "", "")

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onStop() {
        // Synthesis is synchronous in the current implementation.
        // Future: signal cancellation to the native engine.
    }

    @Suppress("DEPRECATION")
    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return

        val text = request.charSequenceText?.toString() ?: request.text ?: return
        if (text.isBlank()) {
            callback.done()
            return
        }

        val language = resolveLanguage(request.language)
        val ttsEngine = engine
        if (ttsEngine == null || ttsEngine.state(language) != EngineState.READY) {
            callback.error()
            return
        }

        try {
            val result = ttsEngine.synthesize(text, language)
            val pcmBytes = floatToPcm16(result.samples)

            callback.start(result.sampleRate, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.audioAvailable(pcmBytes, 0, pcmBytes.size)
            callback.done()
        } catch (e: SynthesisException) {
            Log.e(TAG, "Synthesis failed", e)
            callback.error()
        } catch (e: ModelNotAvailableException) {
            Log.e(TAG, "Model not available", e)
            callback.error()
        }
    }

    private fun initializeAvailableLanguages() {
        for (language in TtsLanguage.entries) {
            val descriptor = when (language) {
                TtsLanguage.ARABIC -> ModelManager.ARABIC_MODEL
                TtsLanguage.ENGLISH -> ModelManager.ENGLISH_MODEL
            }
            val dir = modelManager.modelDir(descriptor)
            if (modelManager.isModelAvailable(descriptor) && modelManager.isEspeakDataAvailable()) {
                try {
                    engine?.initialize(language, dir)
                } catch (e: ModelNotAvailableException) {
                    Log.w(TAG, "Failed to initialize $language on service start", e)
                }
            }
        }
    }

    private fun resolveLanguage(lang: String?): TtsLanguage = when {
        lang == "ara" || lang == "ar" -> TtsLanguage.ARABIC
        else -> TtsLanguage.ENGLISH
    }

    private fun floatToPcm16(samples: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            buffer.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
        }
        return buffer.array()
    }

    companion object {
        private const val TAG = "CodexTtsService"
    }
}
