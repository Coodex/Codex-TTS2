package com.coodex.tts

/**
 * Single designated point for loading the native library.
 * No other class should call [System.loadLibrary] directly.
 */
object NativeLib {

    @Volatile
    private var loaded = false

    @JvmStatic
    fun ensureLoaded() {
        if (!loaded) {
            synchronized(this) {
                if (!loaded) {
                    System.loadLibrary("codex_tts")
                    loaded = true
                }
            }
        }
    }
}
