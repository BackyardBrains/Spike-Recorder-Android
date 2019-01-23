//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "JniHelper.h"

void JniHelper::invokeStaticVoid(JavaVM *vm, const char *methodName, const char *methodSignature, ...) {
    // get current thread JNIEnv
    JNIEnv *env;
    int stat = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (stat == JNI_EDETACHED)  //We are on a different thread, attach
        vm->AttachCurrentThread(&env, nullptr);
    if (env == nullptr)
        return;  //Cant attach to java, bail

    jclass clazz = env->FindClass(JNI_HELPER_CLASS_NAME);
    jmethodID mid = env->GetStaticMethodID(clazz, methodName, methodSignature);
    if (mid == nullptr) return;

    va_list args;
    va_start(args, methodSignature);
    env->CallStaticVoidMethodV(clazz, mid, args);
    va_end(args);
}

void
JniHelper::invokeVoid(JavaVM *vm, jobject object, const char *methodName, const char *methodSignature, ...) {
    // get current thread JNIEnv
    JNIEnv *env;
    int stat = vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (stat == JNI_EDETACHED)  //We are on a different thread, attach
        vm->AttachCurrentThread(&env, nullptr);
    if (env == nullptr)
        return;  //Cant attach to java, bail

    jclass clazz = env->FindClass(ABSTRACT_USB_SAMPLE_SOURCE_CLASS_NAME);
    jmethodID mid = env->GetMethodID(clazz, methodName, methodSignature);
    if (mid == nullptr) return;

    va_list args;
    va_start(args, methodSignature);
    env->CallNonvirtualVoidMethodV(object, clazz, mid, args);
    va_end(args);
}