//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "JniHelper.h"

void JniHelper::invokeVoid(JNIEnv *env, const char *callbackName, const char *callbackSignature, ...) {
    jclass clazz = env->FindClass(JNI_HELPER_CLASS_NAME);
    jmethodID mid = env->GetStaticMethodID(clazz, callbackName, callbackSignature);

    va_list args;
    va_start(args, callbackSignature);
    env->CallStaticVoidMethodV(clazz, mid, args);
    va_end(args);
}
