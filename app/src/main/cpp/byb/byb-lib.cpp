//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>
#include <string>
#include <AmModulationProcessor.h>
#include <JniHelper.h>

#include "includes/drawing.h"
#include "includes/processing.h"

using namespace std;

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
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jobject thiz, jobject out, jbyteArray data,
                                                           jint length);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processAudioStream(JNIEnv *env, jobject thiz, jobject out, jshortArray inSamples,
                                                          jint length);
JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jobject thiz, jobject out, jshortArray inSamples,
                                                         jintArray inEventIndices, jint eventCount, jint start,
                                                         jint end, jint drawSurfaceWidth);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jobject thiz, jobject out,
                                                                  jshortArray inSamples,
                                                                  jintArray inEventIndices, jint eventCount,
                                                                  jint start, jint end,
                                                                  jint drawSurfaceWidth);
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


JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jobject thiz, jobject out, jbyteArray data,
                                                           jint length) {
    jclass cls = env->GetObjectClass(out);
    // get samples field
    jfieldID samplesFid = env->GetFieldID(cls, "samples", "[S");
    jobject samplesObj = env->GetObjectField(out, samplesFid);
    jshortArray samples = reinterpret_cast<jshortArray>(samplesObj);
    // get sampleCount field
    jfieldID sampleCountFid = env->GetFieldID(cls, "sampleCount", "I");
    // get eventIndices field
    jfieldID eventIndicesFid = env->GetFieldID(cls, "eventIndices", "[I");
    jobject eventIndicesObj = env->GetObjectField(out, eventIndicesFid);
    jintArray eventIndices = reinterpret_cast<jintArray>(eventIndicesObj);
    // get eventNames field
    jfieldID eventNamesFid = env->GetFieldID(cls, "eventNames", "[Ljava/lang/String;");
    jobject eventNamesObj = env->GetObjectField(out, eventNamesFid);
    jobjectArray eventNames = reinterpret_cast<jobjectArray>(eventNamesObj);
    // get eventCount field
    jfieldID eventCountFid = env->GetFieldID(cls, "eventCount", "I");

    int sampleCount = env->GetArrayLength(samples);
    int eventCount = env->GetArrayLength(eventIndices);

    jbyte *dataPtr = new jbyte[length];
    env->GetByteArrayRegion(data, 0, length, dataPtr);

    // exception check
    if (exception_check(env)) {
        delete[] dataPtr;
        return;
    }

    unsigned char *uDataPtr = new unsigned char[length];
    copy(dataPtr, dataPtr + length, uDataPtr);

    jshort *outSamplesPtr = new jshort[sampleCount];
    jint *outEventIndicesPtr = new jint[eventCount];
    string *outEventNamesPtr = new string[eventCount];
    jint *outCounts = new jint[2];
    processIncomingBytes(uDataPtr, length, outSamplesPtr, outEventIndicesPtr, outEventNamesPtr,
                         outCounts);

    // if we did get some events create array of strings that represent event names adn populate it
    for (int i = 0; i < outCounts[1]; i++) {
        env->SetObjectArrayElement(eventNames, i, env->NewStringUTF(outEventNamesPtr[i].c_str()));
    }

    // exception check
    if (exception_check(env)) {
        delete[] dataPtr;
        delete[] uDataPtr;
        delete[] outSamplesPtr;
        delete[] outEventIndicesPtr;
        delete[] outEventNamesPtr;
        delete[] outCounts;
        return;
    }

    env->SetShortArrayRegion(samples, 0, outCounts[0], outSamplesPtr);
    env->SetIntField(out, sampleCountFid, outCounts[0]);
    env->SetIntArrayRegion(eventIndices, 0, outCounts[1], outEventIndicesPtr);
    env->SetIntField(out, eventCountFid, outCounts[1]);
    delete[] dataPtr;
    delete[] uDataPtr;
    delete[] outSamplesPtr;
    delete[] outEventIndicesPtr;
    delete[] outEventNamesPtr;
    delete[] outCounts;

//    env->NewObject(cls, methodId, samples, eventIndices, eventLabels)
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processAudioStream(JNIEnv *env, jobject thiz, jobject out, jshortArray inSamples,
                                                          jint length) {
    jclass cls = env->GetObjectClass(out);
    jfieldID samplesFid = env->GetFieldID(cls, "samples", "[S");
    jobject samplesObj = env->GetObjectField(out, samplesFid);
    jshortArray samples = reinterpret_cast<jshortArray>(samplesObj);
    jfieldID sampleCountFid = env->GetFieldID(cls, "sampleCount", "I");

    jshort *inSamplesPtr = new jshort[length];
    env->GetShortArrayRegion(inSamples, 0, length, inSamplesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inSamplesPtr;
        return;
    }

    jshort *outSamplesPtr = new jshort[length];
    jboolean isReceivingAmSignalBefore = static_cast<jboolean>(amModulationProcessor.isReceivingAmSignal());
    amModulationProcessor.process(inSamplesPtr, outSamplesPtr, length);
    jboolean isReceivingAmSignalAfter = static_cast<jboolean>(amModulationProcessor.isReceivingAmSignal());
    if (isReceivingAmSignalBefore != isReceivingAmSignalAfter) {
        jniHelper.invokeVoid(env, "onAmDemodulationChange", "(Z)V", isReceivingAmSignalAfter);
    }

    // exception check
    if (exception_check(env)) {
        delete[] inSamplesPtr;
        delete[] outSamplesPtr;
        return;
    }

    env->SetShortArrayRegion(samples, 0, length, outSamplesPtr);
    env->SetIntField(out, sampleCountFid, length);
    delete[] inSamplesPtr;
    delete[] outSamplesPtr;

    return;
}

JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(amModulationProcessor.isReceivingAmSignal());
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jobject thiz, jobject out, jshortArray inSamples,
                                                         jintArray inEventIndices, jint eventCount, jint start,
                                                         jint end, jint drawSurfaceWidth) {
    jclass cls = env->GetObjectClass(out);
    // get samples field
    jfieldID samplesFid = env->GetFieldID(cls, "samples", "[S");
    jobject samplesObj = env->GetObjectField(out, samplesFid);
    jshortArray samples = reinterpret_cast<jshortArray>(samplesObj);
    // get sampleCount field
    jfieldID sampleCountFid = env->GetFieldID(cls, "sampleCount", "I");
    // get eventIndices field
    jfieldID eventIndicesFid = env->GetFieldID(cls, "eventIndices", "[I");
    jobject eventIndicesObj = env->GetObjectField(out, eventIndicesFid);
    jintArray eventIndices = reinterpret_cast<jintArray>(eventIndicesObj);
    // get eventCount field
    jfieldID eventCountFid = env->GetFieldID(cls, "eventCount", "I");

    int len = env->GetArrayLength(inSamples);
    jshort *inSamplesPtr = new jshort[len];
    env->GetShortArrayRegion(inSamples, 0, len, inSamplesPtr);

    jint *inEventIndicesPtr = new jint[eventCount];
    env->GetIntArrayRegion(inEventIndices, 0, eventCount, inEventIndicesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inSamplesPtr;
        delete[] inEventIndicesPtr;
        return;
    }

    int maxSampleCount = drawSurfaceWidth * 5; // can't be more than x4 when enveloping (from experience)
    int maxEventCount = 100;
    jshort *outSamplesPtr = new jshort[maxSampleCount];
    jint *outEventIndicesPtr = new int[maxEventCount];
    int *returned = prepareForDrawing(outSamplesPtr, inSamplesPtr, outEventIndicesPtr, inEventIndicesPtr, eventCount,
                                      start, end, drawSurfaceWidth);

    env->SetShortArrayRegion(samples, 0, returned[0], outSamplesPtr);
    env->SetIntField(out, sampleCountFid, returned[0]);
    env->SetIntArrayRegion(eventIndices, 0, returned[1], outEventIndicesPtr);
    env->SetIntField(out, eventCountFid, returned[1]);
    delete[] inSamplesPtr;
    delete[] inEventIndicesPtr;
    delete[] outSamplesPtr;
    delete[] outEventIndicesPtr;
    delete[] returned;
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jobject thiz, jobject out,
                                                                  jshortArray inSamples, jintArray inEventIndices,
                                                                  jint eventCount, jint start, jint end,
                                                                  jint drawSurfaceWidth) {
    int drawSamplesCount = end - start;
    int samplesCount = env->GetArrayLength(inSamples);
    int from = (int) ((samplesCount - drawSamplesCount) * .5);
    int to = (int) ((samplesCount + drawSamplesCount) * .5);

    Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(env, thiz, out, inSamples, inEventIndices, eventCount,
                                                             from, to, drawSurfaceWidth);
}