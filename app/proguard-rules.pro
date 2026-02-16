# sherpa-onnx JNI bindings — keep all native method declarations
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Keep data classes used by JNI
-keepclassmembers class com.k2fsa.sherpa.onnx.GeneratedAudio {
    <fields>;
    <methods>;
}

-keepclassmembers class com.k2fsa.sherpa.onnx.GenerationConfig {
    <fields>;
    <methods>;
}
