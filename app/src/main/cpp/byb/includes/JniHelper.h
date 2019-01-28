//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_JNIHELPER_H
#define SPIKE_RECORDER_ANDROID_JNIHELPER_H


#include <jni.h>

#define JNI_HELPER_CLASS_NAME "com/backyardbrains/utils/JniHelper"
#define ABSTRACT_USB_SAMPLE_SOURCE_CLASS_NAME "com/backyardbrains/dsp/usb/AbstractUsbSignalSource"

class JniHelper {
public:
    /**
     * Invokes public static method with the specified methodName and specified methodSignature on a JniHelper java class.
     */
    static void invokeStaticVoid(JavaVM *vm, const char *methodName, const char *methodSignature, ...);

    /**
     * Invokes public method with the specified methodName and specified methodSignature on a specified object.
     */
    static void invokeVoid(JavaVM *vm, jobject object, const char *methodName, const char *methodSignature, ...);
};


#endif //SPIKE_RECORDER_ANDROID_JNIHELPER_H
