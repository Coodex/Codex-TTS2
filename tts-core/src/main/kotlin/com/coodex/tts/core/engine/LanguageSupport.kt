package com.coodex.tts.core.engine

/**
 * Indicates the level of support the engine has for a requested locale.
 * Values mirror the Android TextToSpeech.LANG_* constants.
 */
enum class LanguageSupport {
    /** The locale is not supported at all. */
    NOT_SUPPORTED,

    /** The language is supported but not the specific country variant. */
    LANG_AVAILABLE,

    /** Both language and country are supported. */
    LANG_COUNTRY_AVAILABLE,

    /** Language, country, and variant are all supported. */
    LANG_COUNTRY_VAR_AVAILABLE,
}
