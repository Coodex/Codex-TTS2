# Codex-TTS2 ProGuard Rules

# Keep JNI native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the NativeLib entry point
-keep class com.coodex.tts.NativeLib { *; }
