//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "JniHelper.h"

void JniHelper::invokeVoid(JavaVM *vm, const char *callbackName, const char *callbackSignature, ...) {
    // get current thread JNIEnv
    JNIEnv *env;
    int stat = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (stat == JNI_EDETACHED)  //We are on a different thread, attach
        vm->AttachCurrentThread(&env, NULL);
    if (env == NULL)
        return;  //Cant attach to java, bail

    jclass clazz = env->FindClass(JNI_HELPER_CLASS_NAME);
    jmethodID mid = env->GetStaticMethodID(clazz, callbackName, callbackSignature);

    va_list args;
    va_start(args, callbackSignature);
    env->CallStaticVoidMethodV(clazz, mid, args);
    va_end(args);
}
