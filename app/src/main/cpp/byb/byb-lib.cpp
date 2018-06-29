//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>
#include <string>
#include <AmModulationProcessor.h>
#include <JniHelper.h>

#include "includes/drawing.h"
#include "includes/processing.h"

#define HELLO "Hello from C++"

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_JniUtils_helloTest(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_testPassByRef(JNIEnv *env, jobject thiz, jshortArray test);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jobject thiz, jint sampleRate);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setFilters(JNIEnv *env, jobject thiz, jfloat lowCutOff, jfloat highCutOff);
JNIEXPORT jobject JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jobject thiz, jbyteArray data, jint length);
JNIEXPORT jobject JNICALL
Java_com_backyardbrains_utils_JniUtils_processAudioStream(JNIEnv *env, jobject thiz, jshortArray data, jint length);
JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jobject thiz);
JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jobject thiz, jshortArray envelopedSamples,
                                                         jshortArray samples, jintArray envelopedEventIndices,
                                                         jintArray eventIndices, jint eventCount, jint start, jint end,
                                                         jint drawSurfaceWidth);
JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jobject thiz,
                                                                  jshortArray envelopedSamples, jshortArray samples,
                                                                  jintArray envelopedEventIndices,
                                                                  jintArray eventIndices, jint eventCount, jint start,
                                                                  jint end, jint drawSurfaceWidth);
}

AmModulationProcessor amModulationProcessor;
JniHelper jniHelper;

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
Java_com_backyardbrains_utils_JniUtils_helloTest(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(HELLO);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_testPassByRef(JNIEnv *env, jobject thiz, jshortArray test) {
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

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jobject thiz, jint sampleRate) {
    setSampleRate(sampleRate);
    amModulationProcessor.setSampleRate(sampleRate);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setFilters(JNIEnv *env, jobject thiz, jfloat lowCutOff, jfloat highCutOff) {
    setFilters(lowCutOff, highCutOff);
    amModulationProcessor.setFilters(lowCutOff, highCutOff);
}


JNIEXPORT jobject JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jobject thiz, jbyteArray data, jint length) {
    jshortArray samples = env->NewShortArray(0);
    jintArray eventIndices = env->NewIntArray(0);
    jobjectArray eventLabels = env->NewObjectArray(0, env->FindClass("java/lang/String"), env->NewStringUTF(""));
    jclass cls = env->FindClass("com/backyardbrains/usb/SamplesWithEvents");
    jmethodID methodId = env->GetMethodID(cls, "<init>", "([S[I[Ljava/lang/String;)V");
    jobject obj = env->NewObject(cls, methodId, samples, eventIndices, eventLabels);


    jbyte *pData = new jbyte[length];
    env->GetByteArrayRegion(data, 0, length, pData);

    // exception check
    if (exception_check(env)) {
        delete[] pData;
        return obj;
    }

    unsigned char *puData = new unsigned char[length];
    std::copy(pData, pData + length, puData);

    jshort *outSamples = new jshort[MAX_BYTES];
    jint *outEventIndices = new jint[MAX_EVENTS];
    std::string *outEventLabels = new std::string[MAX_EVENTS];
    jint *outCounts = new jint[2];
    processIncomingBytes(puData, length, outSamples, outEventIndices, outEventLabels,
                         outCounts);

    samples = env->NewShortArray(outCounts[0]);
    eventIndices = env->NewIntArray(outCounts[1]);
    eventLabels = env->NewObjectArray(outCounts[1], env->FindClass("java/lang/String"), env->NewStringUTF(""));
    for (int i = 0; i < outCounts[1]; i++) {
        env->SetObjectArrayElement(eventLabels, i, env->NewStringUTF(outEventLabels[i].c_str()));
    }

    // exception check
    if (exception_check(env)) {
        delete[] pData;
        delete[] puData;
        delete[] outSamples;
        delete[] outEventIndices;
        delete[] outEventLabels;
        delete[] outCounts;
        return obj;
    }

    env->SetShortArrayRegion(samples, 0, outCounts[0], outSamples);
    env->SetIntArrayRegion(eventIndices, 0, outCounts[1], outEventIndices);
    delete[] pData;
    delete[] puData;
    delete[] outSamples;
    delete[] outEventIndices;
    delete[] outEventLabels;
    delete[] outCounts;

    return env->NewObject(cls, methodId, samples, eventIndices, eventLabels);
}

JNIEXPORT jobject JNICALL
Java_com_backyardbrains_utils_JniUtils_processAudioStream(JNIEnv *env, jobject thiz, jshortArray data, jint length) {
    jshortArray samples = env->NewShortArray(0);
    jintArray eventIndices = env->NewIntArray(0);
    jobjectArray eventLabels = env->NewObjectArray(0, env->FindClass("java/lang/String"), env->NewStringUTF(""));
    jclass cls = env->FindClass("com/backyardbrains/usb/SamplesWithEvents");
    jmethodID methodId = env->GetMethodID(cls, "<init>", "([S[I[Ljava/lang/String;)V");
    jobject obj = env->NewObject(cls, methodId, samples, eventIndices, eventLabels);

    jshort *inSamples = new jshort[length];
    env->GetShortArrayRegion(data, 0, length, inSamples);

    // exception check
    if (exception_check(env)) {
        delete[] inSamples;
        return obj;
    }

    jshort *outSamples = new jshort[length];
    jboolean isReceivingAmSignalBefore = static_cast<jboolean>(amModulationProcessor.isReceivingAmSignal());
    amModulationProcessor.process(inSamples, outSamples, length);
    jboolean isReceivingAmSignalAfter = static_cast<jboolean>(amModulationProcessor.isReceivingAmSignal());
    if (isReceivingAmSignalBefore != isReceivingAmSignalAfter) {
        jniHelper.invokeVoid(env, "onAmDemodulationChange", "(Z)V", isReceivingAmSignalAfter);
    }

    samples = env->NewShortArray(length);

    // exception check
    if (exception_check(env)) {
        delete[] inSamples;
        delete[] outSamples;
        return obj;
    }

    env->SetShortArrayRegion(samples, 0, length, outSamples);
    delete[] inSamples;
    delete[] outSamples;

    return env->NewObject(cls, methodId, samples, eventIndices, eventLabels);
}

JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(amModulationProcessor.isReceivingAmSignal());
}

JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jobject thiz,
                                                         jshortArray envelopedSamples,
                                                         jshortArray samples, jintArray envelopedEventIndices,
                                                         jintArray eventIndices, jint eventCount, jint start,
                                                         jint end,
                                                         jint drawSurfaceWidth) {
    int len = env->GetArrayLength(samples);
    jshort *inSamples = new jshort[len];
    env->GetShortArrayRegion(samples, 0, len, inSamples);

    jint *inEventIndices = new jint[eventCount];
    env->GetIntArrayRegion(eventIndices, 0, eventCount, inEventIndices);

    // exception check
    if (exception_check(env)) {
        delete[] inSamples;
        delete[] inEventIndices;
        return env->NewIntArray(2);
    }

    int maxSampleCount = drawSurfaceWidth * 5; // can't be more than x4 when enveloping (from experience)
    int maxEventCount = 100;
    jshort *oSamples = new jshort[maxSampleCount];
    jint *oEventIndices = new int[maxEventCount];
    int *returned = prepareForDrawing(oSamples, inSamples, oEventIndices, inEventIndices, eventCount, start, end,
                                      drawSurfaceWidth);

    jintArray result = env->NewIntArray(2);
    env->SetIntArrayRegion(result, 0, 2, returned);
    env->SetShortArrayRegion(envelopedSamples, 0, returned[0], oSamples);
    env->SetIntArrayRegion(envelopedEventIndices, 0, returned[1], oEventIndices);
    delete[] inSamples;
    delete[] inEventIndices;
    delete[] oSamples;
    delete[] oEventIndices;

    // exception check
    if (exception_check(env)) return env->NewIntArray(2);

    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jobject thiz,
                                                                  jshortArray envelopedSamples,
                                                                  jshortArray samples, jintArray envelopedEventIndices,
                                                                  jintArray eventIndices, jint eventCount, jint start,
                                                                  jint end, jint drawSurfaceWidth) {
    int drawSamplesCount = end - start;
    int samplesCount = env->GetArrayLength(samples);
    int from = (int) ((samplesCount - drawSamplesCount) * .5);
    int to = (int) ((samplesCount + drawSamplesCount) * .5);

    return Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(env, thiz, envelopedSamples, samples,
                                                                    envelopedEventIndices, eventIndices, eventCount,
                                                                    from, to,
                                                                    drawSurfaceWidth);
}