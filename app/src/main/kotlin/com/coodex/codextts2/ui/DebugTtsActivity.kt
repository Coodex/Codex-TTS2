package com.coodex.codextts2.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.coodex.codextts2.R
import com.coodex.codextts2.tts.EngineState
import com.coodex.codextts2.tts.ModelManager
import com.coodex.codextts2.tts.ModelNotAvailableException
import com.coodex.codextts2.tts.SherpaOnnxTtsEngine
import com.coodex.codextts2.tts.SynthesisException
import com.coodex.codextts2.tts.SynthesisResult
import com.coodex.codextts2.tts.TtsLanguage
import com.coodex.codextts2.tts.WavWriter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Minimal debug screen for testing TTS synthesis.
 *
 * Allows entering text, selecting a language (Arabic/English),
 * synthesizing speech, saving to WAV, and playing back the result.
 */
class DebugTtsActivity : AppCompatActivity() {

    private lateinit var modelManager: ModelManager
    private lateinit var ttsEngine: SherpaOnnxTtsEngine

    private lateinit var inputText: EditText
    private lateinit var languageGroup: RadioGroup
    private lateinit var btnSynthesize: MaterialButton
    private lateinit var btnPlay: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    private var lastResult: SynthesisResult? = null
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_tts)

        modelManager = ModelManager(this)
        ttsEngine = SherpaOnnxTtsEngine()

        bindViews()
        setupListeners()
        prefillSampleText()
    }

    override fun onDestroy() {
        audioTrack?.release()
        ttsEngine.release()
        super.onDestroy()
    }

    private fun bindViews() {
        inputText = findViewById(R.id.input_text)
        languageGroup = findViewById(R.id.language_group)
        btnSynthesize = findViewById(R.id.btn_synthesize)
        btnPlay = findViewById(R.id.btn_play)
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupListeners() {
        languageGroup.setOnCheckedChangeListener { _, _ -> prefillSampleText() }

        btnSynthesize.setOnClickListener { onSynthesize() }
        btnPlay.setOnClickListener { onPlay() }
    }

    private fun prefillSampleText() {
        val sample = if (selectedLanguage() == TtsLanguage.ARABIC) {
            getString(R.string.sample_arabic)
        } else {
            getString(R.string.sample_english)
        }
        inputText.setText(sample)
    }

    private fun selectedLanguage(): TtsLanguage =
        if (languageGroup.checkedRadioButtonId == R.id.radio_arabic) {
            TtsLanguage.ARABIC
        } else {
            TtsLanguage.ENGLISH
        }

    private fun onSynthesize() {
        val text = inputText.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        val language = selectedLanguage()
        setUiEnabled(false)
        btnPlay.isEnabled = false
        lastResult = null

        lifecycleScope.launch {
            try {
                ensureModelReady(language)
                setStatus(getString(R.string.status_synthesizing))
                showProgress(true)

                val result = withContext(Dispatchers.Default) {
                    ttsEngine.synthesize(text, language)
                }

                lastResult = result
                val outputFile = saveWav(result, language)
                setStatus(getString(R.string.status_done, outputFile.name))
                btnPlay.isEnabled = true
            } catch (e: SynthesisException) {
                setStatus(getString(R.string.status_error, e.message ?: "synthesis error"))
            } catch (e: ModelNotAvailableException) {
                setStatus(getString(R.string.status_error, e.message ?: "model not available"))
            } catch (e: java.io.IOException) {
                setStatus(getString(R.string.status_error, e.message ?: "I/O error"))
            } finally {
                showProgress(false)
                setUiEnabled(true)
            }
        }
    }

    private fun onPlay() {
        val result = lastResult ?: return

        audioTrack?.release()
        audioTrack = null

        lifecycleScope.launch(Dispatchers.Default) {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(result.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(result.samples.size * Float.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(result.samples, 0, result.samples.size, AudioTrack.WRITE_BLOCKING)
            track.play()
            audioTrack = track
        }
    }

    private suspend fun ensureModelReady(language: TtsLanguage) {
        if (ttsEngine.state(language) == EngineState.READY) return

        val descriptor = when (language) {
            TtsLanguage.ARABIC -> ModelManager.ARABIC_MODEL
            TtsLanguage.ENGLISH -> ModelManager.ENGLISH_MODEL
        }

        if (!modelManager.isEspeakDataAvailable()) {
            setStatus(getString(R.string.status_downloading) + " (espeak-ng-data)")
            showProgress(true)
            withContext(Dispatchers.IO) {
                modelManager.downloadEspeakData()
            }
        }

        if (!modelManager.isModelAvailable(descriptor)) {
            setStatus(getString(R.string.status_downloading) + " (${descriptor.displayName})")
            showProgress(true)
            withContext(Dispatchers.IO) {
                modelManager.downloadModel(descriptor)
            }
        }

        setStatus(getString(R.string.status_loading_model))
        val modelDir = modelManager.modelDir(descriptor)
        withContext(Dispatchers.IO) {
            ttsEngine.initialize(language, modelDir)
        }
    }

    private suspend fun saveWav(result: SynthesisResult, language: TtsLanguage): File {
        val outputDir = File(filesDir, "output")
        outputDir.mkdirs()
        val filename = "tts_${language.iso639}_${System.currentTimeMillis()}.wav"
        val outputFile = File(outputDir, filename)

        withContext(Dispatchers.IO) {
            WavWriter.write(result.samples, result.sampleRate, outputFile)
        }
        return outputFile
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    private fun showProgress(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setUiEnabled(enabled: Boolean) {
        btnSynthesize.isEnabled = enabled
        inputText.isEnabled = enabled
        for (i in 0 until languageGroup.childCount) {
            languageGroup.getChildAt(i).isEnabled = enabled
        }
    }
}
