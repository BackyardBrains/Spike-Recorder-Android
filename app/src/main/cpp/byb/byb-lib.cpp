//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <jni.h>
#include <algorithm>
#include <string>

#include "SignalUtils.h"
#include "AmModulationProcessor.h"
#include "SampleStreamProcessor.h"
#include "ThresholdProcessor.h"
#include "DrawingUtils.h"
#include "SpikeAnalysis.h"
#include "AutocorrelationAnalysis.h"
#include "CrossCorrelationAnalysis.h"
#include "IsiAnalysis.h"
#include "AverageSpikeAnalysis.h"
#include "AnalysisUtils.h"
#include "JniHelper.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#define DR_WAV_IMPLEMENTATION

#include "dr_wav.h"

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_JniUtils_helloTest(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_testPassByRef(JNIEnv *env, jclass type, jshortArray test);
JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_interleaveSignal(JNIEnv *env, jclass type, jshortArray out, jobject in);
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
Java_com_backyardbrains_utils_JniUtils_setThreshold(JNIEnv *env, jclass type, jfloat threshold);
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
Java_com_backyardbrains_utils_JniUtils_processThreshold(JNIEnv *env, jclass type, jobject out, jobject in,
                                                        jboolean averageSamples);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jclass type, jobject out, jobjectArray inSamples,
                                                         int frameCount, jintArray inEventIndices, jint eventCount,
                                                         jint start, jint end, jint drawSurfaceWidth);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jclass type, jobject out,
                                                                  jobjectArray inSamples, jint sampleCount,
                                                                  jintArray inEventIndices, jint eventCount,
                                                                  jint start, jint end, jint drawSurfaceWidth);
JNIEXPORT jobjectArray JNICALL
Java_com_backyardbrains_utils_JniUtils_findSpikes(JNIEnv *env, jclass type, jstring filePath, jobjectArray valuesPos,
                                                  jobjectArray indicesPos, jobjectArray timesPos,
                                                  jobjectArray valuesNeg, jobjectArray indicesNeg,
                                                  jobjectArray timesNeg, jint channelCount, jint maxSpikes);

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

JavaVM *vm = nullptr;
jfieldID channelCountFid;
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
    HeartbeatListener() = default;

    ~HeartbeatListener() = default;

    void onHeartbeat(int bmp) override { JniHelper::invokeStaticVoid(vm, "onHeartbeat", "(I)V", bmp); }
};

class EventListener : public OnEventListenerListener {
public:
    EventListener() {
        sampleSourceObj = nullptr;
    }

    ~EventListener() = default;

    void setSampleSourceObj(jobject object) {
        sampleSourceObj = object;
    }

    void onSpikerBoxHardwareTypeDetected(int hardwareType) override {
        JniHelper::invokeVoid(vm, sampleSourceObj, "setHardwareType", "(I)V", hardwareType);
    };

    void onMaxSampleRateAndNumOfChannelsReply(int maxSampleRate, int channelCount) override {
        JniHelper::invokeVoid(vm, sampleSourceObj, "setSampleRate", "(I)V", maxSampleRate);
        JniHelper::invokeVoid(vm, sampleSourceObj, "setChannelCount", "(I)V", channelCount);

        sampleStreamProcessor->setSampleRate(maxSampleRate);
        sampleStreamProcessor->setChannelCount(channelCount);
    };

    void onExpansionBoardTypeDetection(int expansionBoardType) override {
        JniHelper::invokeVoid(vm, sampleSourceObj, "setExpansionBoardType", "(I)V", expansionBoardType);

        switch (expansionBoardType) {
            default:
            case SampleStreamUtils::NONE_BOARD_DETACHED:
                sampleStreamProcessor->setSampleRate(10000.0);
                sampleStreamProcessor->setChannelCount(2);
                break;
            case SampleStreamUtils::ADDITIONAL_INPUTS_EXPANSION_BOARD:
                sampleStreamProcessor->setSampleRate(5000.0);
                sampleStreamProcessor->setChannelCount(4);
                break;
            case SampleStreamUtils::HAMMER_EXPANSION_BOARD:
            case SampleStreamUtils::JOYSTICK_EXPANSION_BOARD:
                sampleStreamProcessor->setSampleRate(5000.0);
                sampleStreamProcessor->setChannelCount(3);
                break;
        }
    }

private:
    jobject sampleSourceObj{};
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
    channelCountFid = env->GetFieldID(cls, "channelCount", "I");
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_backyardbrains_utils_JniUtils_helloTest(JNIEnv *env, jclass type) {
    return env->NewStringUTF("Hello from C++");
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_testPassByRef(JNIEnv *env, jclass type, jshortArray test) {
    int len = env->GetArrayLength(test);
    auto *pTest = new jshort[len];
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

extern "C" JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_interleaveSignal(JNIEnv *env, jclass type, jshortArray out, jobject in) {
    jint channelCount = env->GetIntField(in, channelCountFid);
    auto samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(in, samplesMFid));
    auto inSampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(in, sampleCountsMFid));

    jint *inSampleCountsPtr = new jint[channelCount];
    env->GetIntArrayRegion(inSampleCountsM, 0, channelCount, inSampleCountsPtr);

    // check if we didn't get any samples... in that case we can return
    jint frameCount = INT_MAX;
    for (int i = 0; i < channelCount; i++) {
        if (inSampleCountsPtr[i] == 0) return 0;
        if (inSampleCountsPtr[i] < frameCount) frameCount = inSampleCountsPtr[i];
    }
    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        inSamplesPtr[i] = new jshort[frameCount]{0};
        env->GetShortArrayRegion(reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i)), 0,
                                 frameCount, inSamplesPtr[i]);
    }

    // exception check
    if (exception_check(env)) {
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        delete[] inSampleCountsPtr;
        return 0;
    }

    jint sampleCount = channelCount * frameCount;
    jshort *outSamplesPtr = SignalUtils::interleaveSignal(inSamplesPtr, frameCount, channelCount);

    if (exception_check(env)) {
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        delete[] inSampleCountsPtr;
        delete[] outSamplesPtr;
        return 0;
    }

    env->SetShortArrayRegion(out, 0, sampleCount, outSamplesPtr);

    for (int i = 0; i < channelCount; i++) {
        delete[] inSamplesPtr[i];
    }
    delete[] inSamplesPtr;
    delete[] inSampleCountsPtr;
    delete[] outSamplesPtr;
    return sampleCount;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jclass type, jint sampleRate) {
    amModulationProcessor->setSampleRate(sampleRate);
    sampleStreamProcessor->setSampleRate(sampleRate);
    thresholdProcessor->setSampleRate(sampleRate);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setChannelCount(JNIEnv *env, jclass type, jint channelCount) {
    amModulationProcessor->setChannelCount(channelCount);
    sampleStreamProcessor->setChannelCount(channelCount);
    thresholdProcessor->setChannelCount(channelCount);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setFilters(JNIEnv *env, jclass type, jfloat lowCutOff, jfloat highCutOff) {
    amModulationProcessor->setFilters(lowCutOff, highCutOff);
    sampleStreamProcessor->setFilters(lowCutOff, highCutOff);
    thresholdProcessor->setFilters(lowCutOff, highCutOff);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                           jint length, jobject sampleSourceObject) {
    jint channelCount = env->GetIntField(out, channelCountFid);
    auto samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    auto samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    auto sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));
    auto eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, eventIndicesFid));
    auto eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, eventNamesFid));

    jint eventCount = env->GetArrayLength(eventIndices);

    auto *inBytesPtr = new jbyte[length];
    env->GetByteArrayRegion(inBytes, 0, length, inBytesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        return;
    }

    auto *uInBytesPtr = new unsigned char[length];
    std::copy(inBytesPtr, inBytesPtr + length, uInBytesPtr);

    // pass sample source object to event listener so proper method can be triggered on it when necessary
    eventListener->setSampleSourceObj(sampleSourceObject);

    auto **outSamplesPtr = new jshort *[channelCount];
    jint *outSampleCounts = new jint[channelCount];
    jint *outEventIndicesPtr = new jint[eventCount];
    auto *outEventNamesPtr = new std::string[eventCount];
    jint outEventCount;
    sampleStreamProcessor->process(uInBytesPtr, length, outSamplesPtr, outSampleCounts, outEventIndicesPtr,
                                   outEventNamesPtr, outEventCount, channelCount);

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
        auto channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
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

extern "C" JNIEXPORT jboolean JNICALL
Java_com_backyardbrains_utils_JniUtils_isAudioStreamAmModulated(JNIEnv *env, jclass type) {
    return static_cast<jboolean>(amModulationProcessor->isReceivingAmSignal());
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processMicrophoneStream(JNIEnv *env, jclass type, jobject out,
                                                               jbyteArray inBytes, jint length) {
    jint channelCount = env->GetIntField(out, channelCountFid);
    auto samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    auto samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    auto sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));

    auto *inBytesPtr = new jbyte[length];
    env->GetByteArrayRegion(inBytes, 0, length, inBytesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        return;
    }

    jint sampleCount = length / 2;
    jint frameCount = sampleCount / channelCount;
    auto **outSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++)
        outSamplesPtr[i] = new jshort[frameCount]{0};
    auto isReceivingAmSignalBefore = static_cast<jboolean>(amModulationProcessor->isReceivingAmSignal());
    amModulationProcessor->process(reinterpret_cast<short *>(inBytesPtr), outSamplesPtr, sampleCount, frameCount);
    auto isReceivingAmSignalAfter = static_cast<jboolean>(amModulationProcessor->isReceivingAmSignal());
    // if we detected that AM modulation started/ended java code needs to be informed
    if (isReceivingAmSignalBefore != isReceivingAmSignalAfter) {
        JniHelper::invokeStaticVoid(vm, "onAmDemodulationChange", "(Z)V", isReceivingAmSignalAfter);
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
        auto deinterleavedSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
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

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processPlaybackStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                             jint length, jintArray inEventIndices,
                                                             jobjectArray inEventNames, jint inEventCount, jlong start,
                                                             jlong end, jint prependSamples) {
    jint channelCount = env->GetIntField(out, channelCountFid);
    auto samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    auto samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    auto sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));
    auto eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, eventIndicesFid));
    auto eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, eventNamesFid));

    auto *inBytesPtr = new jbyte[length];
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
    jint frameCount = sampleCount / channelCount;
    auto **outSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        outSamplesPtr[i] = new jshort[frameCount]{0};
    }
    SignalUtils::deinterleaveSignal(outSamplesPtr, reinterpret_cast<short *>(inBytesPtr),
                                    sampleCount, channelCount);
    jint *outEventIndicesPtr = new jint[inEventCount];

    jint eventCounter = 0;
    jint prepend = std::min(0, prependSamples);
    for (int i = 0; i < inEventCount; i++) {
        jint sampleIndex = inEventIndicesPtr[i] - prepend;
        if (start <= sampleIndex && sampleIndex < end) {
            outEventIndicesPtr[eventCounter] = static_cast<jint>(sampleIndex - start);

            auto string = (jstring) (env->GetObjectArrayElement(inEventNames, i));
            const char *rawString = env->GetStringUTFChars(string, JNI_FALSE);
            env->SetObjectArrayElement(eventNames, eventCounter++, env->NewStringUTF(rawString));
            env->ReleaseStringUTFChars(string, rawString);
        }
    }

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        for (int i = 0; i < channelCount; i++)
            delete[] outSamplesPtr[i];
        delete[] outSamplesPtr;
        delete[] inEventIndicesPtr;
        delete[] outEventIndicesPtr;
        return;
    }

    env->SetShortArrayRegion(samples, 0, frameCount, outSamplesPtr[0]);
    env->SetIntField(out, sampleCountFid, frameCount);
    jint *deinterleavedSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        auto deinterleavedSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
        env->SetShortArrayRegion(deinterleavedSamples, 0, frameCount, outSamplesPtr[i]);
        deinterleavedSampleCounts[i] = frameCount;
    }
    env->SetIntArrayRegion(sampleCountsM, 0, channelCount, deinterleavedSampleCounts);
    env->SetIntArrayRegion(eventIndices, 0, eventCounter, outEventIndicesPtr);
    env->SetIntField(out, eventCountFid, eventCounter);
    env->SetLongField(out, lastSampleIndexFid, prepend + end);
    delete[] inBytesPtr;
    for (int i = 0; i < channelCount; i++)
        delete[] outSamplesPtr[i];
    delete[] outSamplesPtr;
    delete[] deinterleavedSampleCounts;
    delete[] inEventIndicesPtr;
    delete[] outEventIndicesPtr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragedSampleCount(JNIEnv *env, jclass type) {
    return thresholdProcessor->getAveragedSampleCount();
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragedSampleCount(JNIEnv *env, jclass type, jint averagedSampleCount) {
    thresholdProcessor->setAveragedSampleCount(averagedSampleCount);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSelectedChannel(JNIEnv *env, jclass type, jint selectedChannel) {
    thresholdProcessor->setSelectedChannel(selectedChannel);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setThreshold(JNIEnv *env, jclass type, jfloat threshold) {
    thresholdProcessor->setThreshold(threshold);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resetThreshold(JNIEnv *env, jclass type) {
    thresholdProcessor->resetThreshold();
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_pauseThreshold(JNIEnv *env, jclass type) {
    thresholdProcessor->setPaused(true);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragingTriggerType(JNIEnv *env, jclass type) {
    return thresholdProcessor->getTriggerType();
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragingTriggerType(JNIEnv *env, jclass type, jint triggerType) {
    thresholdProcessor->setTriggerType(triggerType);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resumeThreshold(JNIEnv *env, jclass type) {
    thresholdProcessor->setPaused(false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setBpmProcessing(JNIEnv *env, jclass type, jboolean processBpm) {
    thresholdProcessor->setBpmProcessing(processBpm);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processThreshold(JNIEnv *env, jclass type, jobject out, jobject in,
                                                        jboolean averageSamples) {
    auto inSamplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(in, samplesMFid));
    auto inSampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(in, sampleCountsMFid));

    jint channelCount = env->GetArrayLength(inSamplesM);
    jint *inSampleCountsPtr = new jint[channelCount];
    env->GetIntArrayRegion(inSampleCountsM, 0, channelCount, inSampleCountsPtr);

    // check if we didn't get any samples... in that case we can return
    for (int i = 0; i < channelCount; i++) {
        if (inSampleCountsPtr[i] == 0) return;
    }

    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        inSamplesPtr[i] = new jshort[inSampleCountsPtr[i]]{0};
        env->GetShortArrayRegion(reinterpret_cast<jshortArray>(env->GetObjectArrayElement(inSamplesM, i)), 0,
                                 inSampleCountsPtr[i], inSamplesPtr[i]);
    }

    // exception check
    if (exception_check(env)) {
        delete[] inSampleCountsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        return;
    }

    if (!averageSamples) {
        thresholdProcessor->appendIncomingSamples(inSamplesPtr, inSampleCountsPtr);

        delete[] inSampleCountsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;

        return;
    }

    auto outSamples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    auto outSamplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    auto outSampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));
    auto inEventIndices = reinterpret_cast<jintArray>(env->GetObjectField(in, eventIndicesFid));
    auto inEventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(in, eventNamesFid));
    jint inEventCount = env->GetIntField(in, eventCountFid);

    jint *inEventIndicesPtr = new jint[inEventCount];
    env->GetIntArrayRegion(inEventIndices, 0, inEventCount, inEventIndicesPtr);
    jint *inEventsPtr = new jint[inEventCount];
    for (int i = 0; i < inEventCount; i++) {
        auto string = (jstring) (env->GetObjectArrayElement(inEventNames, i));
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

    auto **outSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) outSamplesPtr[i] = new jshort[ThresholdProcessor::DEFAULT_SAMPLE_COUNT]{0};
    jint *averagedSampleCount = new jint[channelCount]{0};
    thresholdProcessor->process(outSamplesPtr, averagedSampleCount, inSamplesPtr, inSampleCountsPtr, inEventIndicesPtr,
                                inEventsPtr, inEventCount);

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

    jint *channelSampleCounts = new jint[channelCount];
    env->SetShortArrayRegion(outSamples, 0, averagedSampleCount[0], outSamplesPtr[0]);
    env->SetIntField(out, sampleCountFid, averagedSampleCount[0]);
    for (int i = 0; i < channelCount; i++) {
        auto channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(outSamplesM, i));
        env->SetShortArrayRegion(channelSamples, 0, averagedSampleCount[i], outSamplesPtr[i]);
        env->SetObjectArrayElement(outSamplesM, i, channelSamples);
        env->DeleteLocalRef(channelSamples);

        channelSampleCounts[i] = averagedSampleCount[i];
    }
    env->SetIntArrayRegion(outSampleCountsM, 0, channelCount, channelSampleCounts);

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

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(JNIEnv *env, jclass type, jobject out, jobjectArray inSamples,
                                                         jint frameCount, jintArray inEventIndices, jint eventCount,
                                                         jint start, jint end, jint drawSurfaceWidth) {
    auto samples = reinterpret_cast<jshortArray>(env->GetObjectField(out, samplesFid));
    auto samplesM = reinterpret_cast<jobjectArray>(env->GetObjectField(out, samplesMFid));
    auto sampleCountsM = reinterpret_cast<jintArray>(env->GetObjectField(out, sampleCountsMFid));
    auto eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, eventIndicesFid));

    jint channelCount = env->GetArrayLength(inSamples);
    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; ++i) {
        auto tmpSamples = (jshortArray) env->GetObjectArrayElement(inSamples, i);
        inSamplesPtr[i] = new jshort[frameCount];
        env->GetShortArrayRegion(tmpSamples, 0, frameCount, inSamplesPtr[i]);
        env->DeleteLocalRef(tmpSamples);
    }

    jint *inEventIndicesPtr = new jint[eventCount];
    env->GetIntArrayRegion(inEventIndices, 0, eventCount, inEventIndicesPtr);

    // exception check
    if (exception_check(env)) {
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        delete[] inEventIndicesPtr;
        return;
    }

    int maxSampleCount = drawSurfaceWidth * 5; // can't be more than x5 when enveloping (deducted from testing)
    int maxEventCount = 100;
    auto **outSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++)
        outSamplesPtr[i] = new jshort[maxSampleCount]{0};
    jint *outSampleCounts = new jint[channelCount]{0};
    jint *outEventIndicesPtr = new int[maxEventCount];
    jint outEventCount;
    DrawingUtils::prepareForDrawing(outSamplesPtr, outSampleCounts, outEventIndicesPtr, outEventCount, inSamplesPtr,
                                    channelCount, inEventIndicesPtr, eventCount, start, end, drawSurfaceWidth);

    env->SetShortArrayRegion(samples, 0, outSampleCounts[0], outSamplesPtr[0]);
    env->SetIntField(out, sampleCountFid, outSampleCounts[0]);
    jint *channelSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        auto channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samplesM, i));
        env->SetShortArrayRegion(channelSamples, 0, outSampleCounts[i], outSamplesPtr[i]);
        env->SetObjectArrayElement(samplesM, i, channelSamples);
        env->DeleteLocalRef(channelSamples);

        channelSampleCounts[i] = outSampleCounts[i];
    }
    env->SetIntArrayRegion(sampleCountsM, 0, channelCount, channelSampleCounts);
    env->SetIntArrayRegion(eventIndices, 0, outEventCount, outEventIndicesPtr);
    env->SetIntField(out, eventCountFid, outEventCount);

    for (int i = 0; i < channelCount; i++) {
        delete[] inSamplesPtr[i];
    }
    delete[] inSamplesPtr;
    delete[] inEventIndicesPtr;
    for (int i = 0; i < channelCount; i++) {
        delete[] outSamplesPtr[i];
    }
    delete[] outSamplesPtr;
    delete[] outSampleCounts;
    delete[] outEventIndicesPtr;
    delete[] channelSampleCounts;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jclass type, jobject out,
                                                                  jobjectArray inSamples, jint sampleCount,
                                                                  jintArray inEventIndices, jint eventCount,
                                                                  jint start, jint end, jint drawSurfaceWidth) {
    int drawSamplesCount = end - start;
    int from = (int) ((sampleCount - drawSamplesCount) * .5);
    int to = (int) ((sampleCount + drawSamplesCount) * .5);

    Java_com_backyardbrains_utils_JniUtils_prepareForDrawing(env, type, out, inSamples, sampleCount, inEventIndices,
                                                             eventCount, from, to, drawSurfaceWidth);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_backyardbrains_utils_JniUtils_findSpikes(JNIEnv *env, jclass type, jstring filePath, jobjectArray valuesPos,
                                                  jobjectArray indicesPos, jobjectArray timesPos,
                                                  jobjectArray valuesNeg, jobjectArray indicesNeg,
                                                  jobjectArray timesNeg, jint channelCount, jint maxSpikes) {
    // get pointer to file path string
    const char *filePathPtr = env->GetStringUTFChars(filePath, JNI_FALSE);
    // get pointer to positive values array
    auto **valuesPosPtr = new jshort *[channelCount];
    jint **indicesPosPtr = new jint *[channelCount];
    auto **timesPosPtr = new jfloat *[channelCount];
    auto **valuesNegPtr = new jshort *[channelCount];
    jint **indicesNegPtr = new jint *[channelCount];
    auto **timesNegPtr = new jfloat *[channelCount];
    for (int i = 0; i < channelCount; ++i) {
        valuesPosPtr[i] = new jshort[maxSpikes];
        indicesPosPtr[i] = new jint[maxSpikes];
        timesPosPtr[i] = new jfloat[maxSpikes];
        valuesNegPtr[i] = new jshort[maxSpikes];
        indicesNegPtr[i] = new jint[maxSpikes];
        timesNegPtr[i] = new jfloat[maxSpikes];
    }

    jclass intArrayClass = env->FindClass("[I");
    jobjectArray result = env->NewObjectArray(channelCount, intArrayClass, nullptr);
    for (int i = 0; i < channelCount; i++) {
        jintArray tmpResult = env->NewIntArray(2);
        env->SetIntArrayRegion(tmpResult, 0, 2, new int[2]{0});
        env->SetObjectArrayElement(result, i, tmpResult);
        env->DeleteLocalRef(tmpResult);
    }

    // exception check
    if (exception_check(env)) {
        env->ReleaseStringUTFChars(filePath, filePathPtr);
        for (int i = 0; i < channelCount; ++i) {
            delete[] valuesPosPtr[i];
            delete[] indicesPosPtr[i];
            delete[] timesPosPtr[i];
            delete[] valuesNegPtr[i];
            delete[] indicesNegPtr[i];
            delete[] timesNegPtr[i];
        }
        delete[] valuesPosPtr;
        delete[] indicesPosPtr;
        delete[] timesPosPtr;
        delete[] valuesNegPtr;
        delete[] indicesNegPtr;
        delete[] timesNegPtr;
        return result;
    }

    jint *outPosCount = new jint[channelCount];
    jint *outNegCount = new jint[channelCount];
    spikeAnalysis->findSpikes(filePathPtr, valuesPosPtr, indicesPosPtr, timesPosPtr, valuesNegPtr, indicesNegPtr,
                              timesNegPtr, channelCount, outPosCount, outNegCount);

    for (int i = 0; i < channelCount; i++) {
        auto tmpResult = reinterpret_cast<jintArray>(env->GetObjectArrayElement(result, i));
        env->SetIntArrayRegion(tmpResult, 0, 2, new int[2]{outPosCount[i], outNegCount[i]});
        env->SetObjectArrayElement(result, i, tmpResult);
        env->DeleteLocalRef(tmpResult);

        auto tempValuesPos = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(valuesPos, i));
        env->SetShortArrayRegion(tempValuesPos, 0, outPosCount[i], valuesPosPtr[i]);
        env->SetObjectArrayElement(valuesPos, i, tempValuesPos);
        env->DeleteLocalRef(tempValuesPos);

        auto tempIndicesPos = reinterpret_cast<jintArray>(env->GetObjectArrayElement(indicesPos, i));
        env->SetIntArrayRegion(tempIndicesPos, 0, outPosCount[i], indicesPosPtr[i]);
        env->SetObjectArrayElement(indicesPos, i, tempIndicesPos);
        env->DeleteLocalRef(tempIndicesPos);

        auto tempTimesPos = reinterpret_cast<jfloatArray>(env->GetObjectArrayElement(timesPos, i));
        env->SetFloatArrayRegion(tempTimesPos, 0, outPosCount[i], timesPosPtr[i]);
        env->SetObjectArrayElement(timesPos, i, tempTimesPos);
        env->DeleteLocalRef(tempTimesPos);

        auto tempValuesNeg = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(valuesNeg, i));
        env->SetShortArrayRegion(tempValuesNeg, 0, outNegCount[i], valuesNegPtr[i]);
        env->SetObjectArrayElement(valuesNeg, i, tempValuesNeg);
        env->DeleteLocalRef(tempValuesNeg);

        auto tempIndicesNeg = reinterpret_cast<jintArray>(env->GetObjectArrayElement(indicesNeg, i));
        env->SetIntArrayRegion(tempIndicesNeg, 0, outNegCount[i], indicesNegPtr[i]);
        env->SetObjectArrayElement(indicesNeg, i, tempIndicesNeg);
        env->DeleteLocalRef(tempIndicesNeg);

        auto tempTimesNeg = reinterpret_cast<jfloatArray>(env->GetObjectArrayElement(timesNeg, i));
        env->SetFloatArrayRegion(tempTimesNeg, 0, outNegCount[i], timesNegPtr[i]);
        env->SetObjectArrayElement(timesNeg, i, tempTimesNeg);
        env->DeleteLocalRef(tempTimesNeg);
    }

    env->ReleaseStringUTFChars(filePath, filePathPtr);
    for (int i = 0; i < channelCount; ++i) {
        delete[] valuesPosPtr[i];
        delete[] indicesPosPtr[i];
        delete[] timesPosPtr[i];
        delete[] valuesNegPtr[i];
        delete[] indicesNegPtr[i];
        delete[] timesNegPtr[i];
    }
    delete[] valuesPosPtr;
    delete[] indicesPosPtr;
    delete[] timesPosPtr;
    delete[] valuesNegPtr;
    delete[] indicesNegPtr;
    delete[] timesNegPtr;
    delete[] outPosCount;
    delete[] outNegCount;

    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_autocorrelationAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                               jint spikeTrainCount, jintArray spikeCounts,
                                                               jobjectArray analysis, jint analysisBinCount) {
    jint *spikeCountsPtr = new jint[spikeTrainCount];
    env->GetIntArrayRegion(spikeCounts, 0, spikeTrainCount, spikeCountsPtr);

    auto **spikeTrainsPtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto spikeTrain = (jfloatArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jfloat[spikeCountsPtr[i]];
        env->GetFloatArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }
    jint **analysisPtr = new jint *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

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
        auto trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

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

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_isiAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                   jint spikeTrainCount, jintArray spikeCounts, jobjectArray analysis,
                                                   jint analysisBinCount) {
    jint *spikeCountsPtr = new jint[spikeTrainCount];
    env->GetIntArrayRegion(spikeCounts, 0, spikeTrainCount, spikeCountsPtr);

    auto **spikeTrainsPtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto spikeTrain = (jfloatArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jfloat[spikeCountsPtr[i]];
        env->GetFloatArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }
    jint **analysisPtr = new jint *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

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
        auto trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

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

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_crossCorrelationAnalysis(JNIEnv *env, jclass type, jobjectArray spikeTrains,
                                                                jint spikeTrainCount, jintArray spikeCounts,
                                                                jobjectArray analysis, jint analysisCount,
                                                                jint analysisBinCount) {
    jint *spikeCountsPtr = new jint[spikeTrainCount];
    env->GetIntArrayRegion(spikeCounts, 0, spikeTrainCount, spikeCountsPtr);

    auto **spikeTrainsPtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto spikeTrain = (jfloatArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jfloat[spikeCountsPtr[i]];
        env->GetFloatArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }
    jint **analysisPtr = new jint *[analysisCount];
    for (int i = 0; i < analysisCount; ++i) {
        auto trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

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
        auto trainAnalysis = (jintArray) env->GetObjectArrayElement(analysis, i);

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

extern "C" JNIEXPORT void JNICALL
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
        auto spikeTrain = (jintArray) env->GetObjectArrayElement(spikeTrains, i);

        spikeTrainsPtr[i] = new jint[spikeCountsPtr[i]];
        env->GetIntArrayRegion(spikeTrain, 0, spikeCountsPtr[i], spikeTrainsPtr[i]);

        env->DeleteLocalRef(spikeTrain);
    }

    auto **averageSpikePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(averageSpike, i);

        averageSpikePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, averageSpikePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    auto **normAverageSpikePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normAverageSpike, i);

        normAverageSpikePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normAverageSpikePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }

    auto **normTopStdLinePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normTopStdLine, i);

        normTopStdLinePtr[i] = new jfloat[batchSpikeCount];
        env->GetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normTopStdLinePtr[i]);

        env->DeleteLocalRef(trainAnalysis);
    }
    auto **normBottomStdLinePtr = new jfloat *[spikeTrainCount];
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normBottomStdLine, i);

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
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(averageSpike, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, averageSpikePtr[i]);
        env->SetObjectArrayElement(averageSpike, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normAverageSpike, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normAverageSpikePtr[i]);
        env->SetObjectArrayElement(normAverageSpike, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normTopStdLine, i);

        env->SetFloatArrayRegion(trainAnalysis, 0, batchSpikeCount, normTopStdLinePtr[i]);
        env->SetObjectArrayElement(normTopStdLine, i, trainAnalysis);

        env->DeleteLocalRef(trainAnalysis);
    }
    for (int i = 0; i < spikeTrainCount; ++i) {
        auto trainAnalysis = (jfloatArray) env->GetObjectArrayElement(normBottomStdLine, i);

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