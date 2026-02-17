package com.coodex.codextts2.core

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StubTtsEngineTest {

    @Test
    fun `synthesizeToWav throws NotImplementedError`() {
        val engine: TtsEngine = StubTtsEngine()
        assertThrows(NotImplementedError::class.java) {
            engine.synthesizeToWav("test text")
        }
    }

    @Test
    fun `synthesizeToWav throws NotImplementedError with voiceId`() {
        val engine: TtsEngine = StubTtsEngine()
        assertThrows(NotImplementedError::class.java) {
            engine.synthesizeToWav("test text", voiceId = "ar-kareem")
        }
    }
}
