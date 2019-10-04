
#include <jni.h>
#include <android/log.h>
#include <cmath>


#include "SequenceGenerator.h"

static SequenceGenerator generator;

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_INFO, __FUNCTION__, "onLoad");
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    // Get jclass with env->FindClass.
    // Register methods with env->RegisterNatives.

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_vigurskiy_speedometerdatasource_external_SequenceGenerator_setMaxValue(
        JNIEnv *pEnv,
        jobject pThis,
        jfloat value
) {
    generator.max_value = value;
}


extern "C"
JNIEXPORT jfloat JNICALL
Java_com_vigurskiy_speedometerdatasource_external_SequenceGenerator_getNextValue(
        JNIEnv *pEnv,
        jobject pThis
) {
    return generator.next_value();
}


float SequenceGenerator::next_value() {
    current_value += GENERATION_STEP;

    double sinFunctionValue = (sin(2 * M_PI * current_value / GENERATION_SIN_KOEFF1) +
                            sin(M_PI * current_value / GENERATION_SIN_KOEFF2)) / 2;

    return max_value * static_cast<float>(sinFunctionValue);
}