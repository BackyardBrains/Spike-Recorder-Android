//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_JNIHELPER_H
#define SPIKE_RECORDER_ANDROID_JNIHELPER_H


#include <jni.h>

#define JNI_HELPER_CLASS_NAME "com/backyardbrains/utils/JniHelper"

class JniHelper {
public:

    static void invokeVoid(JNIEnv *env, const char *callbackName, const char *callbackSignature, ...);
};


#endif //SPIKE_RECORDER_ANDROID_JNIHELPER_H
