//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>

#include "byb_lib.h"

#define HELLO "Hello from C++"

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_NativePOC_helloTest(JNIEnv *env, jobject thiz);
JNIEXPORT jshortArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                  jint start, jint end, jint returnCount);
JNIEXPORT jshortArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForThresholdDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                   jint start, jint end, jint returnCount);
}

static jboolean exception_check(JNIEnv *env) {
    if (env->ExceptionCheck()) {
#ifndef NDEBUG
        env->ExceptionDescribe();
#endif
        env->ExceptionClear();

        return (JNI_TRUE);
    }

    return (JNI_FALSE);
}

JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_NativePOC_helloTest(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(HELLO);
}

JNIEXPORT jshortArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                  jint start, jint end, jint returnCount) {
    int len = env->GetArrayLength(samples);
    jshort *pSamples = new jshort[len];
    env->GetShortArrayRegion(samples, 0, len, pSamples);

    // exception check
    if (exception_check(env)) {
        delete[] pSamples;
        return env->NewShortArray(0);
    }

    int resultLength = returnCount * 2; // *2 for x and y
    short *output = new jshort[resultLength];
    int returned = prepareForDrawing(output, pSamples, start, end, returnCount);

    jshortArray result = env->NewShortArray(returned);
    if (result == NULL) {
        delete[] pSamples;
        return env->NewShortArray(0);
    }

    env->SetShortArrayRegion(result, 0, returned, output);
    delete[] pSamples;

    // exception check
    if (exception_check(env)) return env->NewShortArray(0);

    return result;
}

JNIEXPORT jshortArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForThresholdDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                   jint start, jint end, jint returnCount) {
    int drawSamplesCount = end - start;
    int samplesCount = env->GetArrayLength(samples);
    int from = (int) ((samplesCount - drawSamplesCount) * .5);
    int to = (int) ((samplesCount + drawSamplesCount) * .5);

    return Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(env, thiz, samples, from, to, returnCount);
}
