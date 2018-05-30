//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>
#include <string>

#include "byb_lib.h"

#define HELLO "Hello from C++"

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_NativePOC_helloTest(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_NativePOC_testPassByRef(JNIEnv *env, jobject thiz, jshortArray test);
JNIEXPORT jshortArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                  jint start, jint end,
                                                                  jint returnCount);
JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForMarkerDrawing(JNIEnv *env, jobject thiz, jintArray eventIndices,
                                                                jint fromSample, jint toSample, jint returnCount);
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

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_NativePOC_testPassByRef(JNIEnv *env, jobject thiz, jshortArray test) {
    int len = env->GetArrayLength(test);
    jshort *pTest = new jshort[len];
    env->GetShortArrayRegion(test, 0, len, pTest);

    // exception check
    if (exception_check(env)) delete[] pTest;


    int value = 10;
    for (int i = 0; i < len; i++) {
        pTest[i] = static_cast<jshort>(value + i);
    }

    env->SetShortArrayRegion(test, 0, static_cast<jsize>(len * .5), pTest);
    delete[] pTest;

    // exception check
    exception_check(env);
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
    jshort *output = new jshort[resultLength];
    int returned = prepareForDrawing(output, pSamples, start, end, returnCount);

    jshortArray result = env->NewShortArray(returned);
    if (result == NULL) {
        delete[] pSamples;
        delete[] output;
        return env->NewShortArray(0);
    }

    env->SetShortArrayRegion(result, 0, returned, output);
    delete[] pSamples;
    delete[] output;

    // exception check
    if (exception_check(env)) return env->NewShortArray(0);

    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForMarkerDrawing(JNIEnv *env, jobject thiz, jintArray eventIndices,
                                                                jint fromSample, jint toSample, jint returnCount) {
    int len = env->GetArrayLength(eventIndices);
    jint *pEventIndices = new jint[len];
    env->GetIntArrayRegion(eventIndices, 0, len, pEventIndices);

    // exception check
    if (exception_check(env)) {
        delete[] pEventIndices;
        return env->NewIntArray(0);
    }


    float scaleX = (float) returnCount / (toSample - fromSample);
    int counter = 0;
    int index;
    for (int i = 0; i < len; i++) {
        index = (int) ((pEventIndices[i] - fromSample) * scaleX);
        if (index >= 0) {
            pEventIndices[counter++] = index;
        }
    }

    jintArray result = env->NewIntArray(counter);
    if (result == NULL) {
        delete[] pEventIndices;
        return env->NewIntArray(0);
    }


    env->SetIntArrayRegion(result, 0, counter, pEventIndices);
    delete[] pEventIndices;

    // exception check
    if (exception_check(env)) return env->NewIntArray(0);

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