//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>
#include <algorithm>
#include <string>

#include "AmModulationProcessor.h"
#include "SampleStreamProcessor.h"
#include "AnalysisUtils.h"
#include "SpikeAnalysis.h"
#include "AutocorrelationAnalysis.h"
#include "JniHelper.h"

#include "includes/drawing.h"

#define DR_WAV_IMPLEMENTATION

#include "dr_wav.h"

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
                                                                  jshortArray inSamples, jintArray inEventIndices,
                                                                  jint eventCount, jint start, jint end,
                                                                  jint drawSurfaceWidth);
JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_findSpikes(JNIEnv *env, jobject thiz, jstring filePath, jshortArray valuesPos,
                                                  jintArray indicesPos, jfloatArray timesPos, jshortArray valuesNeg,
                                                  jintArray indicesNeg, jfloatArray timesNeg, jint maxSpikes);

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_autocorrelationAnalysis(JNIEnv *env, jobject thiz, jobjectArray spikeTrains,
                                                               jint spikeTrainCount, jintArray spikeCounts,
                                                               jobjectArray analysis, jint analysisBinCount);
}

AmModulationProcessor amModulationProcessor;
SampleStreamProcessor sampleStreamProcessor;
SpikeAnalysis spikeAnalysis;
AutocorrelationAnalysis autocorrelationAnalysis;
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
    return env->NewStringUTF("Hello from C++");
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
    amModulationProcessor.setSampleRate(sampleRate);
    sampleStreamProcessor.setSampleRate(sampleRate);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setFilters(JNIEnv *env, jobject thiz, jfloat lowCutOff, jfloat highCutOff) {
    amModulationProcessor.setFilters(lowCutOff, highCutOff);
    sampleStreamProcessor.setFilters(lowCutOff, highCutOff);
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
    std::copy(dataPtr, dataPtr + length, uDataPtr);

    jshort *outSamplesPtr = new jshort[sampleCount];
    jint *outEventIndicesPtr = new jint[eventCount];
    std::string *outEventNamesPtr = new std::string[eventCount];
    jint *outCounts = new jint[2];
    sampleStreamProcessor.process(uDataPtr, length, outSamplesPtr, outEventIndicesPtr, outEventNamesPtr, outCounts);

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


JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_findSpikes(JNIEnv *env, jobject thiz, jstring filePath, jshortArray valuesPos,
                                                  jintArray indicesPos, jfloatArray timesPos, jshortArray valuesNeg,
                                                  jintArray indicesNeg, jfloatArray timesNeg, jint maxSpikes) {
    // get pointer to file path string
    const char *filePathPtr = env->GetStringUTFChars(filePath, JNI_FALSE);
    // get pointer to positive values array
    jshort *valuesPosPtr = new jshort[maxSpikes];
    env->GetShortArrayRegion(valuesPos, 0, maxSpikes, valuesPosPtr);
    // get pointer to positive indices array
    jint *indicesPosPtr = new jint[maxSpikes];
    env->GetIntArrayRegion(indicesPos, 0, maxSpikes, indicesPosPtr);
    // get pointer to positive times array
    jfloat *timesPosPtr = new jfloat[maxSpikes];
    env->GetFloatArrayRegion(timesPos, 0, maxSpikes, timesPosPtr);
    // get pointer to negative values array
    jshort *valuesNegPtr = new jshort[maxSpikes];
    env->GetShortArrayRegion(valuesNeg, 0, maxSpikes, valuesNegPtr);
    // get pointer to negative indices array
    jint *indicesNegPtr = new jint[maxSpikes];
    env->GetIntArrayRegion(indicesNeg, 0, maxSpikes, indicesNegPtr);
    // get pointer to negative times array
    jfloat *timesNegPtr = new jfloat[maxSpikes];
    env->GetFloatArrayRegion(timesNeg, 0, maxSpikes, timesNegPtr);

    // exception check
    if (exception_check(env)) {
        env->ReleaseStringUTFChars(filePath, filePathPtr);
        delete[] indicesPosPtr;
        delete[] timesPosPtr;
        delete[] valuesNegPtr;
        delete[] indicesNegPtr;
        delete[] timesNegPtr;
        return env->NewIntArray(2);
    }

    jint *resultPtr = spikeAnalysis.findSpikes(filePathPtr, valuesPosPtr, indicesPosPtr, timesPosPtr, valuesNegPtr,
                                               indicesNegPtr, timesNegPtr);

    jintArray result = env->NewIntArray(2);
    env->SetIntArrayRegion(result, 0, 2, resultPtr);

    if (exception_check(env)) {
        env->ReleaseStringUTFChars(filePath, filePathPtr);
        delete[] valuesPosPtr;
        delete[] indicesPosPtr;
        delete[] timesPosPtr;
        delete[] valuesNegPtr;
        delete[] indicesNegPtr;
        delete[] timesNegPtr;
        delete[] resultPtr;
        return env->NewIntArray(2);
    }

    env->SetShortArrayRegion(valuesPos, 0, resultPtr[0], valuesPosPtr);
    env->SetIntArrayRegion(indicesPos, 0, resultPtr[0], indicesPosPtr);
    env->SetFloatArrayRegion(timesPos, 0, resultPtr[0], timesPosPtr);
    env->SetShortArrayRegion(valuesNeg, 0, resultPtr[1], valuesNegPtr);
    env->SetIntArrayRegion(indicesNeg, 0, resultPtr[1], indicesNegPtr);
    env->SetFloatArrayRegion(timesNeg, 0, resultPtr[1], timesNegPtr);
    env->ReleaseStringUTFChars(filePath, filePathPtr);
    delete[] valuesPosPtr;
    delete[] indicesPosPtr;
    delete[] timesPosPtr;
    delete[] valuesNegPtr;
    delete[] indicesNegPtr;
    delete[] timesNegPtr;
    delete[] resultPtr;

    return result;
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_autocorrelationAnalysis(JNIEnv *env, jobject thiz, jobjectArray spikeTrains,
                                                               jint spikeTrainCount, jintArray spikeCounts,
                                                               jobjectArray analysis, jint analysisBinCount) {
    jint *spikeCountsPtr = new jint[spikeTrainCount];
    env->GetIntArrayRegion(spikeCounts, 0, spikeTrainCount, spikeCountsPtr);

    jfloat **spikeTrainsPtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray spikeTrain = (jfloatArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jfloat[spikeCountsPtr[i]];
        env->GetFloatArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }
    jint **analysisPtr = new jint *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jintArray trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

        analysisPtr[i] = new jint[analysisBinCount];
        env->GetIntArrayRegion(trainAnalysis, 0, analysisBinCount, analysisPtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    // exception check
    if (exception_check(env)) {
        for (int i = 0; i < spikeTrainCount; i++) {
            delete[] spikeTrainsPtr[i];
            delete[] analysisPtr[i];
        }
        delete[] spikeCountsPtr;
        delete[] spikeTrainsPtr;
        delete[] analysisPtr;
        return;
    }

    autocorrelationAnalysis.process(spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, analysisPtr, analysisBinCount);

    for (int i = 0; i < spikeTrainCount; ++i) {
        jintArray trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

        env->SetIntArrayRegion(trainAnalysis, 0, analysisBinCount, analysisPtr[i]);
        env->SetObjectArrayElement(analysis, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; i++) {
        delete[] spikeTrainsPtr[i];
        delete[] analysisPtr[i];
    }
    delete[] spikeCountsPtr;
    delete[] spikeTrainsPtr;
    delete[] analysisPtr;
}