#include <jni.h>
#include <codex/engine.h>

extern "C" {

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    // Cache jclass / jmethodID references here in future.
    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_coodex_tts_NativeLib_nativeEngineVersion(JNIEnv* env, jobject /* this */) {
    const char* version = codex_engine_version();
    return env->NewStringUTF(version);
}

} // extern "C"
