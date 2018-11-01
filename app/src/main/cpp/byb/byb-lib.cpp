//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>
#include <algorithm>
#include <string>

#include "AmModulationProcessor.h"
#include "SampleStreamProcessor.h"
#include "ThresholdProcessor.h"
#include "SpikeAnalysis.h"
#include "AutocorrelationAnalysis.h"
#include "CrossCorrelationAnalysis.h"
#include "IsiAnalysis.h"
#include "AverageSpikeAnalysis.h"
#include "AnalysisUtils.h"
#include "JniHelper.h"

#include "includes/drawing.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#define DR_WAV_IMPLEMENTATION

#include "dr_wav.h"

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_JniUtils_helloTest(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_testPassByRef(JNIEnv *env, jclass type, jshortArray test);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jclass type, jint sampleRate);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setChannelCount(JNIEnv *env, jclass type, jint channelCount);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setFilters(JNIEnv *env, jclass type, jfloat lowCutOff, jfloat highCutOff);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                           jint length, jobject sampleSourceObject);
JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processMicrophoneStream(JNIEnv *env, jclass type, jobject out,
                                                               jbyteArray inBytes, jint length);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processPlaybackStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                             jint length, jintArray inEventIndices,
                                                             jobjectArray inEventNames, jint inEventCount, jlong start,
                                                             jlong end, jint prependSamples);
JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragedSampleCount(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragedSampleCount(JNIEnv *env, jclass type, jint averagedSampleCount);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSelectedChannel(JNIEnv *env, jclass type, jint selectedChannel);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setThreshold(JNIEnv *env, jclass type, jint threshold);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resetThreshold(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_pauseThreshold(JNIEnv *env, jclass type);
JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragingTriggerType(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragingTriggerType(JNIEnv *env, jclass type, jint triggerType);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resumeThreshold(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setBpmProcessing(JNIEnv *env, jclass type, jboolean processBpm);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processThreshold(JNIEnv *env, jclass type, jobject out);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jclass type, jobject out, jshortArray inSamples,
                                                         jintArray inEventIndices, jint eventCount, jint start,
                                                         jint end, jint drawSurfaceWidth);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jclass type, jobject out,
                                                                  jshortArray inSamples, jintArray inEventIndices,
                                                                  jint eventCount, jint start, jint end,
                                                                  jint drawSurfaceWidth);
JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_findSpikes(JNIEnv *env, jclass type, jstring filePath, jshortArray valuesPos,
                                                  jintArray indicesPos, jfloatArray timesPos, jshortArray valuesNeg,
                                                  jintArray indicesNeg, jfloatArray timesNeg, jint maxSpikes);

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_autocorrelationAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                               jint spikeTrainCount, jintArray spikeCounts,
                                                               jobjectArray analysis, jint analysisBinCount);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_isiAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                   jint spikeTrainCount, jintArray spikeCounts, jobjectArray analysis,
                                                   jint analysisBinCount);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_crossCorrelationAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                                jint spikeTrainCount, jintArray spikeCounts,
                                                                jobjectArray analysis, jint analysisCount,
                                                                jint analysisBinCount);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_averageSpikeAnalysis(JNIEnv *env, jclass type, jstring filePath,
                                                            jobjectArray spikeTrains, jint spikeTrainCount,
                                                            jintArray spikeCounts, jobjectArray averageSpike,
                                                            jobjectArray normAverageSpike, jobjectArray normTopStdLine,
                                                            jobjectArray normBottomStdLine, jint batchSpikeCount);
}

AmModulationProcessor *amModulationProcessor;
SampleStreamProcessor *sampleStreamProcessor;
ThresholdProcessor *thresholdProcessor;
SpikeAnalysis *spikeAnalysis;
AutocorrelationAnalysis *autocorrelationAnalysis;
IsiAnalysis *isiAnalysis;
AverageSpikeAnalysis *averageSpikeAnalysis;
CrossCorrelationAnalysis *crossCorrelationAnalysis;

JniHelper jniHelper;

JavaVM *vm = NULL;
jfieldID samplesFid;
jfieldID sampleCountFid;
jfieldID samplesMFid;
jfieldID sampleCountsMFid;
jfieldID eventIndicesFid;
jfieldID eventNamesFid;
jfieldID eventCountFid;
jfieldID lastSampleIndexFid;

class HeartbeatListener : public OnHeartbeatListener {
public:
    HeartbeatListener() {}

    ~HeartbeatListener() {}

    void onHeartbeat(int bmp) { JniHelper::invokeStaticVoid(vm, "onHeartbeat", "(I)V", bmp); }
};

class EventListener : public OnEventListenerListener {
public:
    EventListener() {}

    ~EventListener() {}

    void setSampleSourceObj(jobject object) {
        EventListener::sampleSourceObj = object;
    }

    void onSpikerBoxHardwareTypeDetected(int hardwareType) {
        JniHelper::invokeVoid(vm, sampleSourceObj, "setHardwareType", "(I)V", hardwareType);
    };

    void onMaxSampleRateAndNumOfChannelsReply(int maxSampleRate, int channelCount) {
        JniHelper::invokeVoid(vm, sampleSourceObj, "setSampleRate", "(I)V", maxSampleRate);
        JniHelper::invokeVoid(vm, sampleSourceObj, "setChannelCount", "(I)V", channelCount);

        sampleStreamProcessor->setChannelCount(channelCount);
    };

private:
    jobject sampleSourceObj;
};

EventListener *eventListener;

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

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    // save VM for later reference
    ::vm = vm;

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    amModulationProcessor = new AmModulationProcessor();
    eventListener = new EventListener();
    sampleStreamProcessor = new SampleStreamProcessor(eventListener);
    thresholdProcessor = new ThresholdProcessor(new HeartbeatListener());
    spikeAnalysis = new SpikeAnalysis();
    autocorrelationAnalysis = new AutocorrelationAnalysis();
    isiAnalysis = new IsiAnalysis();
    averageSpikeAnalysis = new AverageSpikeAnalysis();
    crossCorrelationAnalysis = new CrossCorrelationAnalysis();

    // let's cache fields of the SampleWithEvents java object
    jclass cls = env->FindClass("com/backyardbrains/dsp/SamplesWithEvents");
    samplesFid = env->GetFieldID(cls, "samples", "[S");
    sampleCountFid = env->GetFieldID(cls, "sampleCount", "I");
    samplesMFid = env->GetFieldID(cls, "samplesM", "[[S");
    sampleCountsMFid = env->GetFieldID(cls, "sampleCountM", "[I");
    eventIndicesFid = env->GetFieldID(cls, "eventIndices", "[I");
    eventNamesFid = env->GetFieldID(cls, "eventNames", "[Ljava/lang/String;");
    eventCountFid = env->GetFieldID(cls, "eventCount", "I");
    lastSampleIndexFid = env->GetFieldID(cls, "lastSampleIndex", "J");

    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_JniUtils_helloTest(JNIEnv *env, jclass type) {
    return env->NewStringUTF("Hello from C++");
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_testPassByRef(JNIEnv *env, jclass type, jshortArray test) {
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
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jclass type, jint sampleRate) {
    amModulationProcessor->setSampleRate(sampleRate);
    sampleStreamProcessor->setSampleRate(sampleRate);
    thresholdProcessor->setSampleRate(sampleRate);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setChannelCount(JNIEnv *env, jclass type, jint channelCount) {
    amModulationProcessor->setChannelCount(channelCount);
    sampleStreamProcessor->setChannelCount(channelCount);
    thresholdProcessor->setChannelCount(channelCount);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setFilters(JNIEnv *env, jclass type, jfloat lowCutOff, jfloat highCutOff) {
    amModulationProcessor->setFilters(lowCutOff, highCutOff);
    sampleStreamProcessor->setFilters(lowCutOff, highCutOff);
    thresholdProcessor->setFilters(lowCutOff, highCutOff);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                           jint length, jobject sampleSourceObject) {
    jshortArray samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    jobjectArray samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    jintArray sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));
    jintArray eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, eventIndicesFid));
    jobjectArray eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, eventNamesFid));

    jint eventCount = env->GetArrayLength(eventIndices);

    jbyte *inBytesPtr = new jbyte[length];
    env->GetByteArrayRegion(inBytes, 0, length, inBytesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        return;
    }

    unsigned char *uInBytesPtr = new unsigned char[length];
    std::copy(inBytesPtr, inBytesPtr + length, uInBytesPtr);

    // pass sample source object to event listener so proper method can be triggered on it when necessary
    eventListener->setSampleSourceObj(sampleSourceObject);

    jint channelCount = sampleStreamProcessor->getChannelCount();
    jshort **outSamplesPtr = new jshort *[channelCount];
    jint *outSampleCounts = new jint[channelCount];
    jint *outEventIndicesPtr = new jint[eventCount];
    std::string *outEventNamesPtr = new std::string[eventCount];
    jint outEventCount;
    sampleStreamProcessor->process(uInBytesPtr, length, outSamplesPtr, outSampleCounts, outEventIndicesPtr,
                                   outEventNamesPtr, outEventCount);

    // if we did get some events create array of strings that represent event names and populate it
    for (int i = 0; i < outEventCount; i++) {
        env->SetObjectArrayElement(eventNames, i, env->NewStringUTF(outEventNamesPtr[i].c_str()));
    }

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        delete[] uInBytesPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] outSamplesPtr[i];
        }
        delete[] outSamplesPtr;
        delete[] outSampleCounts;
        delete[] outEventIndicesPtr;
        delete[] outEventNamesPtr;
        return;
    }

    env->SetShortArrayRegion(samples, 0, outSampleCounts[0], outSamplesPtr[0]);
    env->SetIntField(out, sampleCountFid, outSampleCounts[0]);
    jint *channelSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        jshortArray channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
        env->SetShortArrayRegion(channelSamples, 0, outSampleCounts[i], outSamplesPtr[i]);
        env->SetObjectArrayElement(samplesM, i, channelSamples);
        channelSampleCounts[i] = outSampleCounts[i];
    }
    env->SetIntArrayRegion(sampleCountsM, 0, channelCount, channelSampleCounts);
    env->SetIntArrayRegion(eventIndices, 0, outEventCount, outEventIndicesPtr);
    env->SetIntField(out, eventCountFid, outEventCount);
    delete[] inBytesPtr;
    delete[] uInBytesPtr;
    for (int i = 0; i < channelCount; i++) {
        delete[] outSamplesPtr[i];
    }
    delete[] outSamplesPtr;
    delete[] outSampleCounts;
    delete[] outEventIndicesPtr;
    delete[] outEventNamesPtr;
    delete[] channelSampleCounts;
}

JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jclass type) {
    return static_cast<jboolean>(amModulationProcessor->isReceivingAmSignal());
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processMicrophoneStream(JNIEnv *env, jclass type, jobject out,
                                                               jbyteArray inBytes, jint length) {
    jshortArray samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    jobjectArray samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    jintArray sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));

    jbyte *inBytesPtr = new jbyte[length];
    env->GetByteArrayRegion(inBytes, 0, length, inBytesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        return;
    }

    jint sampleCount = length / 2;
    jint channelCount = amModulationProcessor->getChannelCount();
    jint frameCount = sampleCount / channelCount;
    jshort **outSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++)
        outSamplesPtr[i] = new jshort[frameCount];
    jboolean isReceivingAmSignalBefore = static_cast<jboolean>(amModulationProcessor->isReceivingAmSignal());
    amModulationProcessor->process(reinterpret_cast<short *>(inBytesPtr), outSamplesPtr, sampleCount, frameCount);
    jboolean isReceivingAmSignalAfter = static_cast<jboolean>(amModulationProcessor->isReceivingAmSignal());
    // if we detected that AM modulation started/ended java code needs to be informed
    if (isReceivingAmSignalBefore != isReceivingAmSignalAfter) {
        jniHelper.invokeStaticVoid(vm, "onAmDemodulationChange", "(Z)V", isReceivingAmSignalAfter);
    }

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        for (int i = 0; i < channelCount; i++)
            delete[] outSamplesPtr[i];
        delete[] outSamplesPtr;
        return;
    }

    env->SetShortArrayRegion(samples, 0, frameCount, outSamplesPtr[0]);
    env->SetIntField(out, sampleCountFid, frameCount);
    jint *deinterleavedSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        jshortArray deinterleavedSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
        env->SetShortArrayRegion(deinterleavedSamples, 0, frameCount, outSamplesPtr[i]);
        deinterleavedSampleCounts[i] = frameCount;
    }
    env->SetIntArrayRegion(sampleCountsM, 0, channelCount, deinterleavedSampleCounts);

    delete[] inBytesPtr;
    for (int i = 0; i < channelCount; i++)
        delete[] outSamplesPtr[i];
    delete[] outSamplesPtr;
    delete[] deinterleavedSampleCounts;
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processPlaybackStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                             jint length, jintArray inEventIndices,
                                                             jobjectArray inEventNames, jint inEventCount, jlong start,
                                                             jlong end, jint prependSamples) {
    jshortArray samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    jintArray eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, eventIndicesFid));
    jobjectArray eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, eventNamesFid));

    jbyte *inBytesPtr = new jbyte[length];
    env->GetByteArrayRegion(inBytes, 0, length, inBytesPtr);

    jint *inEventIndicesPtr = new jint[inEventCount];
    env->GetIntArrayRegion(inEventIndices, 0, inEventCount, inEventIndicesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        delete[] inEventIndicesPtr;
        return;
    }

    jint sampleCount = length / 2;
    jshort *outSamplesPtr = reinterpret_cast<short *>(inBytesPtr);
    jint *outEventIndicesPtr = new jint[inEventCount];

    jint eventCounter = 0;
    jint prepend = std::min(0, prependSamples);
    for (int i = 0; i < inEventCount; i++) {
        jint sampleIndex = inEventIndicesPtr[i] - prepend;
        if (start <= sampleIndex && sampleIndex < end) {
            outEventIndicesPtr[eventCounter] = static_cast<jint>(sampleIndex - start);

            jstring string = (jstring) (env->GetObjectArrayElement(inEventNames, i));
            const char *rawString = env->GetStringUTFChars(string, JNI_FALSE);
            env->SetObjectArrayElement(eventNames, eventCounter++, env->NewStringUTF(rawString));
            env->ReleaseStringUTFChars(string, rawString);
        }
    }

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        delete[] inEventIndicesPtr;
        delete[] outEventIndicesPtr;
        return;
    }

    env->SetShortArrayRegion(samples, 0, sampleCount, outSamplesPtr);
    env->SetIntField(out, sampleCountFid, sampleCount);
    env->SetIntArrayRegion(eventIndices, 0, eventCounter, outEventIndicesPtr);
    env->SetIntField(out, eventCountFid, eventCounter);
    env->SetLongField(out, lastSampleIndexFid, prepend + end);
    delete[] inBytesPtr;
    delete[] inEventIndicesPtr;
    delete[] outEventIndicesPtr;
}

JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragedSampleCount(JNIEnv *env, jclass type) {
    return thresholdProcessor->getAveragedSampleCount();
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragedSampleCount(JNIEnv *env, jclass type, jint averagedSampleCount) {
    thresholdProcessor->setAveragedSampleCount(averagedSampleCount);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSelectedChannel(JNIEnv *env, jclass type, jint selectedChannel) {
    thresholdProcessor->setSelectedChannel(selectedChannel);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setThreshold(JNIEnv *env, jclass type, jint threshold) {
    thresholdProcessor->setThreshold(threshold);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resetThreshold(JNIEnv *env, jclass type) {
    thresholdProcessor->resetThreshold();
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_pauseThreshold(JNIEnv *env, jclass type) {
    thresholdProcessor->setPaused(true);
}

JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragingTriggerType(JNIEnv *env, jclass type) {
    return thresholdProcessor->getTriggerType();
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragingTriggerType(JNIEnv *env, jclass type, jint triggerType) {
    thresholdProcessor->setTriggerType(triggerType);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resumeThreshold(JNIEnv *env, jclass type) {
    thresholdProcessor->setPaused(false);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setBpmProcessing(JNIEnv *env, jclass type, jboolean processBpm) {
    thresholdProcessor->setBpmProcessing(processBpm);
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processThreshold(JNIEnv *env, jclass type, jobject out) {
    jshortArray samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    jobjectArray samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    jintArray sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));
    jintArray eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, eventIndicesFid));
    jobjectArray eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, eventNamesFid));
    jint eventCount = env->GetIntField(out, eventCountFid);

    jint channelCount = sampleStreamProcessor->getChannelCount();
    jint *inSampleCountsPtr = new jint[channelCount];
    env->GetIntArrayRegion(sampleCountsM, 0, channelCount, inSampleCountsPtr);
    jshort **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        inSamplesPtr[i] = new jshort[inSampleCountsPtr[i]];
        env->GetShortArrayRegion(reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i)), 0,
                                 inSampleCountsPtr[i], inSamplesPtr[i]);
    }
    jint *inEventIndicesPtr = new jint[eventCount];
    env->GetIntArrayRegion(eventIndices, 0, eventCount, inEventIndicesPtr);
    jint *inEventsPtr = new jint[eventCount];
    for (int i = 0; i < eventCount; i++) {
        jstring string = (jstring) (env->GetObjectArrayElement(eventNames, i));
        const char *rawString = env->GetStringUTFChars(string, JNI_FALSE);
        inEventsPtr[i] = std::stoi(rawString);
        env->ReleaseStringUTFChars(string, rawString);
    }

    // exception check
    if (exception_check(env)) {
        delete[] inSampleCountsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        delete[] inEventIndicesPtr;
        delete[] inEventsPtr;
        return;
    }

    jint averagedSampleCount = thresholdProcessor->getSampleCount();
    jshort **outSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) outSamplesPtr[i] = new jshort[averagedSampleCount]{0};
    thresholdProcessor->process(outSamplesPtr, inSamplesPtr, inSampleCountsPtr, inEventIndicesPtr, inEventsPtr,
                                eventCount);

    // exception check
    if (exception_check(env)) {
        delete[] inSampleCountsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        delete[] inEventIndicesPtr;
        delete[] inEventsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] outSamplesPtr[i];
        }
        delete[] outSamplesPtr;
        return;
    }

    env->SetShortArrayRegion(samples, 0, averagedSampleCount, outSamplesPtr[0]);
    env->SetIntField(out, sampleCountFid, averagedSampleCount);
    jint *channelSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        jshortArray channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
        env->SetShortArrayRegion(channelSamples, 0, averagedSampleCount, outSamplesPtr[i]);
        env->SetObjectArrayElement(samplesM, i, channelSamples);
        channelSampleCounts[i] = averagedSampleCount;
    }
    env->SetIntArrayRegion(sampleCountsM, 0, channelCount, channelSampleCounts);

    delete[] inSampleCountsPtr;
    for (int i = 0; i < channelCount; i++) {
        delete[] inSamplesPtr[i];
    }
    delete[] inSamplesPtr;
    delete[] inEventIndicesPtr;
    delete[] inEventsPtr;
    for (int i = 0; i < channelCount; i++) {
        delete[] outSamplesPtr[i];
    }
    delete[] outSamplesPtr;
    delete[] channelSampleCounts;
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jclass type, jobject out, jshortArray inSamples,
                                                         jintArray inEventIndices, jint eventCount, jint start,
                                                         jint end, jint drawSurfaceWidth) {
    jobject samplesObj = env->GetObjectField(out, samplesFid);
    jshortArray samples = reinterpret_cast<jshortArray>(samplesObj);
    jobject eventIndicesObj = env->GetObjectField(out, eventIndicesFid);
    jintArray eventIndices = reinterpret_cast<jintArray>(eventIndicesObj);

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
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jclass type, jobject out,
                                                                  jshortArray inSamples, jintArray inEventIndices,
                                                                  jint eventCount, jint start, jint end,
                                                                  jint drawSurfaceWidth) {
    int drawSamplesCount = end - start;
    int samplesCount = env->GetArrayLength(inSamples);
    int from = (int) ((samplesCount - drawSamplesCount) * .5);
    int to = (int) ((samplesCount + drawSamplesCount) * .5);

    Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(env, type, out, inSamples, inEventIndices, eventCount,
                                                             from, to, drawSurfaceWidth);
}

JNIEXPORT jintArray JNICALL
Java_com_backyardbrains_utils_JniUtils_findSpikes(JNIEnv *env, jclass type, jstring filePath, jshortArray valuesPos,
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

    jint *resultPtr = spikeAnalysis->findSpikes(filePathPtr, valuesPosPtr, indicesPosPtr, timesPosPtr, valuesNegPtr,
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
Java_com_backyardbrains_utils_JniUtils_autocorrelationAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
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

    autocorrelationAnalysis->process(spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, analysisPtr, analysisBinCount);

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

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_isiAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                   jint spikeTrainCount, jintArray spikeCounts, jobjectArray analysis,
                                                   jint analysisBinCount) {
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

    isiAnalysis->process(spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, analysisPtr, analysisBinCount);

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

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_crossCorrelationAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                                jint spikeTrainCount, jintArray spikeCounts,
                                                                jobjectArray analysis, jint analysisCount,
                                                                jint analysisBinCount) {
    jint *spikeCountsPtr = new jint[spikeTrainCount];
    env->GetIntArrayRegion(spikeCounts, 0, spikeTrainCount, spikeCountsPtr);

    jfloat **spikeTrainsPtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray spikeTrain = (jfloatArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jfloat[spikeCountsPtr[i]];
        env->GetFloatArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }
    jint **analysisPtr = new jint *[analysisCount];
    for (int i = 0; i < analysisCount; ++i) {
        jintArray trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

        analysisPtr[i] = new jint[analysisBinCount];
        env->GetIntArrayRegion(trainAnalysis, 0, analysisBinCount, analysisPtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    // exception check
    if (exception_check(env)) {
        for (int i = 0; i < spikeTrainCount; i++) delete[] spikeTrainsPtr[i];
        for (int i = 0; i < analysisCount; i++) delete[] analysisPtr[i];
        delete[] spikeCountsPtr;
        delete[] spikeTrainsPtr;
        delete[] analysisPtr;
        return;
    }

    crossCorrelationAnalysis->process(spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, analysisPtr, analysisBinCount);

    for (int i = 0; i < analysisCount; ++i) {
        jintArray trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

        env->SetIntArrayRegion(trainAnalysis, 0, analysisBinCount, analysisPtr[i]);
        env->SetObjectArrayElement(analysis, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; i++) delete[] spikeTrainsPtr[i];
    for (int i = 0; i < analysisCount; i++) delete[] analysisPtr[i];
    delete[] spikeCountsPtr;
    delete[] spikeTrainsPtr;
    delete[] analysisPtr;
}

JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_averageSpikeAnalysis(JNIEnv *env, jclass type, jstring filePath,
                                                            jobjectArray spikeTrains, jint spikeTrainCount,
                                                            jintArray spikeCounts, jobjectArray averageSpike,
                                                            jobjectArray normAverageSpike, jobjectArray normTopStdLine,
                                                            jobjectArray normBottomStdLine, jint batchSpikeCount) {
    // get pointer to file path string
    const char *filePathPtr = env->GetStringUTFChars(filePath, JNI_FALSE);

    jint *spikeCountsPtr = new jint[spikeTrainCount];
    env->GetIntArrayRegion(spikeCounts, 0, spikeTrainCount, spikeCountsPtr);

    jint **spikeTrainsPtr = new jint *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jintArray spikeTrain = (jintArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jint[spikeCountsPtr[i]];
        env->GetIntArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }

    jfloat **averageSpikePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(averageSpike, i);

        averageSpikePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, averageSpikePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    jfloat **normAverageSpikePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normAverageSpike, i);

        normAverageSpikePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normAverageSpikePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    jfloat **normTopStdLinePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normTopStdLine, i);

        normTopStdLinePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normTopStdLinePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }
    jfloat **normBottomStdLinePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normBottomStdLine, i);

        normBottomStdLinePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normBottomStdLinePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    // exception check
    if (exception_check(env)) {
        env->ReleaseStringUTFChars(filePath, filePathPtr);
        for (int i = 0; i < spikeTrainCount; i++) {
            delete[] spikeTrainsPtr[i];
            delete[] averageSpikePtr[i];
            delete[] normAverageSpikePtr[i];
            delete[] normTopStdLinePtr[i];
            delete[] normBottomStdLinePtr[i];
        }
        delete[] spikeCountsPtr;
        delete[] spikeTrainsPtr;
        delete[] averageSpikePtr;
        delete[] normAverageSpikePtr;
        delete[] normTopStdLinePtr;
        delete[] normBottomStdLinePtr;
        return;
    }

    averageSpikeAnalysis->process(filePathPtr, spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, averageSpikePtr,
                                  normAverageSpikePtr, normTopStdLinePtr, normBottomStdLinePtr, batchSpikeCount);

    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(averageSpike, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, averageSpikePtr[i]);
        env->SetObjectArrayElement(averageSpike, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normAverageSpike, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normAverageSpikePtr[i]);
        env->SetObjectArrayElement(normAverageSpike, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normTopStdLine, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normTopStdLinePtr[i]);
        env->SetObjectArrayElement(normTopStdLine, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; ++i) {
        jfloatArray trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normBottomStdLine, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normBottomStdLinePtr[i]);
        env->SetObjectArrayElement(normBottomStdLine, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    env->ReleaseStringUTFChars(filePath, filePathPtr);
    for (int i = 0; i < spikeTrainCount; i++) {
        delete[] spikeTrainsPtr[i];
        delete[] averageSpikePtr[i];
        delete[] normAverageSpikePtr[i];
        delete[] normTopStdLinePtr[i];
        delete[] normBottomStdLinePtr[i];
    }
    delete[] spikeCountsPtr;
    delete[] spikeTrainsPtr;
}

#pragma clang diagnostic pop