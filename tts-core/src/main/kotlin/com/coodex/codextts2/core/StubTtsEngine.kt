package com.coodex.codextts2.core

import java.io.File

/**
 * Placeholder [TtsEngine] that always throws [NotImplementedError].
 *
 * Use this as a compile-time stand-in until a real backend (e.g. ONNX) is wired up.
 */
class StubTtsEngine : TtsEngine {

    override fun synthesizeToWav(text: String, voiceId: String?): File {
        throw NotImplementedError(
            "StubTtsEngine has no synthesis backend. " +
                "Replace with a concrete implementation (e.g. OnnxTtsEngine)."
        )
    }
}
