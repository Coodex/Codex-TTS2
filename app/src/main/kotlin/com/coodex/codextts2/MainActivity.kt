package com.coodex.codextts2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.coodex.codextts2.core.StubTtsEngine
import com.coodex.codextts2.core.TtsEngine

/**
 * Minimal debug activity for exercising the [TtsEngine] interface.
 *
 * Calls [TtsEngine.synthesizeToWav] on a hardcoded Arabic and English sentence.
 * Currently wired to [StubTtsEngine], so synthesis will report "not implemented"
 * until a real backend is connected.
 */
class MainActivity : AppCompatActivity() {

    private val engine: TtsEngine = StubTtsEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val btnArabic = findViewById<Button>(R.id.btn_arabic)
        val btnEnglish = findViewById<Button>(R.id.btn_english)

        btnArabic.setOnClickListener {
            synthesizeAndReport(getString(R.string.sample_arabic), statusText)
        }

        btnEnglish.setOnClickListener {
            synthesizeAndReport(getString(R.string.sample_english), statusText)
        }
    }

    private fun synthesizeAndReport(text: String, statusText: TextView) {
        try {
            val wav = engine.synthesizeToWav(text)
            statusText.text = getString(R.string.status_done, wav.name)
        } catch (@Suppress("SwallowedException") e: NotImplementedError) {
            statusText.text = getString(R.string.status_not_implemented)
        }
    }
}
