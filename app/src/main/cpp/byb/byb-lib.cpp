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
#include "FftProcessor.h"
#include "DrawingUtils.h"
#include "SpikeAnalysis.h"
#include "AutocorrelationAnalysis.h"
#include "CrossCorrelationAnalysis.h"
#include "IsiAnalysis.h"
#include "AverageSpikeAnalysis.h"
#include "AnalysisUtils.h"
#include "JniHelper.h"

using namespace backyardbrains::processing;
using namespace backyardbrains::analysis;

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
JNIEXPORT jfloat JNICALL
Java_com_backyardbrains_utils_JniUtils_rms(JNIEnv *env, jclass type, jshortArray in, jint length);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jclass type, jint sampleRate);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setChannelCount(JNIEnv *env, jclass type, jint channelCount);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSelectedChannel(JNIEnv *env, jclass type, jint selectedChannel);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setBandFilter(JNIEnv *env, jclass type, jfloat lowCutOffFreq,
                                                     jfloat highCutOffFreq);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setNotchFilter(JNIEnv *env, jclass type, jfloat centerFreq);
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
JNIEXPORT jint JNICALL
Java_com_backyardbrains_utils_JniUtils_getAveragingTriggerType(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setAveragingTriggerType(JNIEnv *env, jclass type, jint triggerType);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setBpmProcessing(JNIEnv *env, jclass type, jboolean processBpm);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setThreshold(JNIEnv *env, jclass type, jfloat threshold);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resetThreshold(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resumeThreshold(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_pauseThreshold(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processThreshold(JNIEnv *env, jclass type, jobject out, jobject in,
                                                        jboolean averageSamples);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resetFft(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processFft(JNIEnv *env, jclass type, jobject out, jobject in,
                                                  jfloat fftSampleRate, jint downsamplingFactor);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForSignalDrawing(JNIEnv *env, jclass type, jobject outSignal,
                                                               jobject outEvents, jobjectArray inSignal,
                                                               jint inFrameCount, jintArray inEventIndices,
                                                               jint inEventCount, jint drawStartIndex,
                                                               jint drawEndIndex, jint drawSurfaceWidth);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jclass type, jobject outSignal,
                                                                  jobject outEvents, jobjectArray inSignal,
                                                                  jint inFrameCount, jintArray inEventIndices,
                                                                  jint inEventCount, jint drawStartIndex,
                                                                  jint rawEndIndex, jint drawSurfaceWidth);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForFftDrawing(JNIEnv *env, jclass type, jobject out, jobjectArray in,
                                                            jint drawStartIndex, jint drawEndIndex,
                                                            jint drawSurfaceWidth, jint drawSurfaceHeight,
                                                            jfloat fftScaleFactor);
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForSpikesDrawing(JNIEnv *env, jclass type, jobject out, jobjectArray in,
                                                               jfloatArray colorInRange, jfloatArray colorOutOfRange,
                                                               jint rangeStart, jint rangeEnd, jint sampleStartIndex,
                                                               jint sampleEndIndex, jint drawStartIndex,
                                                               jint drawEndIndex, jint sampleCount,
                                                               jint drawSurfaceWidth);
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

const char *TAG = "BybLib";

AmModulationProcessor *amModulationProcessor;
SampleStreamProcessor *sampleStreamProcessor;
ThresholdProcessor *thresholdProcessor;
FftProcessor *fftProcessor;
SpikeAnalysis *spikeAnalysis;
AutocorrelationAnalysis *autocorrelationAnalysis;
IsiAnalysis *isiAnalysis;
AverageSpikeAnalysis *averageSpikeAnalysis;
CrossCorrelationAnalysis *crossCorrelationAnalysis;

JavaVM *vm = nullptr;
// SignalData field IDs
jfieldID sdChannelCountFid;
jfieldID sdSamplesFid;
jfieldID sdSampleCountsFid;
jfieldID sdEventIndicesFid;
jfieldID sdEventNamesFid;
jfieldID sdEventCountFid;
jfieldID sdLastSampleIndexFid;
// SignalDrawData
jfieldID sddSamplesFid;
jfieldID sddSampleCountsFid;
// EventsDrawData
jfieldID eddEventIndicesFid;
jfieldID eddEventNamesFid;
jfieldID eddEventCountFid;
// FftData field IDs
jfieldID fdMaxWindowCountFid;
jfieldID fdMaxWindowSizeFid;
jfieldID fdFftFid;
jfieldID fdWindowCountFid;
jfieldID fdWindowSizeFid;
// FftDrawData field IDs
jfieldID fddVerticesFid;
jfieldID fddIndicesFid;
jfieldID fddColorsFid;
jfieldID fddVertexCountFid;
jfieldID fddIndexCountFid;
jfieldID fddColorCountFid;
jfieldID fddScaleXFid;
jfieldID fddScaleYFid;
// SpikeIndexValue
jfieldID sivValueFid;
jfieldID sivIndexFid;
// SpikesDrawData
jfieldID spddVerticesFid;
jfieldID spddColorsFid;
jfieldID spddVertexCountFid;
jfieldID spddColorCountFid;

class HeartbeatListener : public backyardbrains::utils::OnHeartbeatListener {
public:
    HeartbeatListener() = default;

    ~HeartbeatListener() = default;

    void onHeartbeat(int bmp) override {
        backyardbrains::utils::JniHelper::invokeStaticVoid(vm, "onHeartbeat", "(I)V", bmp);
    }
};

class EventListener : public backyardbrains::utils::OnEventListenerListener {
public:
    EventListener() {
        sampleSourceObj = nullptr;
    }

    ~EventListener() = default;

    void setSampleSourceObj(jobject object) {
        sampleSourceObj = object;
    }

    void onSpikerBoxHardwareTypeDetected(int hardwareType) override {
        backyardbrains::utils::JniHelper::invokeVoid(vm, sampleSourceObj, "setHardwareType", "(I)V", hardwareType);
    };

    void onMaxSampleRateAndNumOfChannelsReply(int maxSampleRate, int channelCount) override {
        backyardbrains::utils::JniHelper::invokeVoid(vm, sampleSourceObj, "setSampleRate", "(I)V", maxSampleRate);
        backyardbrains::utils::JniHelper::invokeVoid(vm, sampleSourceObj, "setChannelCount", "(I)V", channelCount);
    };

    void onExpansionBoardTypeDetection(int expansionBoardType) override {
        backyardbrains::utils::JniHelper::invokeVoid(vm, sampleSourceObj, "setExpansionBoardType", "(I)V",
                                                     expansionBoardType);
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

    eventListener = new EventListener();
    amModulationProcessor = new AmModulationProcessor();
    sampleStreamProcessor = new SampleStreamProcessor(eventListener);
    thresholdProcessor = new ThresholdProcessor(new HeartbeatListener());
    fftProcessor = new FftProcessor();
    spikeAnalysis = new SpikeAnalysis();
    autocorrelationAnalysis = new AutocorrelationAnalysis();
    isiAnalysis = new IsiAnalysis();
    averageSpikeAnalysis = new AverageSpikeAnalysis();
    crossCorrelationAnalysis = new CrossCorrelationAnalysis();

    // let's cache fields of the SignalData java object
    jclass cls = env->FindClass("com/backyardbrains/dsp/SignalData");
    sdChannelCountFid = env->GetFieldID(cls, "channelCount", "I");
    sdSamplesFid = env->GetFieldID(cls, "samples", "[[S");
    sdSampleCountsFid = env->GetFieldID(cls, "sampleCounts", "[I");
    sdEventIndicesFid = env->GetFieldID(cls, "eventIndices", "[I");
    sdEventNamesFid = env->GetFieldID(cls, "eventNames", "[Ljava/lang/String;");
    sdEventCountFid = env->GetFieldID(cls, "eventCount", "I");
    sdLastSampleIndexFid = env->GetFieldID(cls, "lastSampleIndex", "J");

    // let's cache fields of the SignalDrawData java object
    cls = env->FindClass("com/backyardbrains/drawing/SignalDrawData");
    sddSamplesFid = env->GetFieldID(cls, "samples", "[[F");
    sddSampleCountsFid = env->GetFieldID(cls, "sampleCounts", "[I");

    // let's cache fields of the EventsDrawData java object
    cls = env->FindClass("com/backyardbrains/drawing/EventsDrawData");
    eddEventIndicesFid = env->GetFieldID(cls, "eventIndices", "[F");
    eddEventNamesFid = env->GetFieldID(cls, "eventNames", "[Ljava/lang/String;");
    eddEventCountFid = env->GetFieldID(cls, "eventCount", "I");

    // let's cache fields of the FftData java object
    cls = env->FindClass("com/backyardbrains/dsp/FftData");
    fdMaxWindowCountFid = env->GetFieldID(cls, "maxWindowCount", "I");
    fdMaxWindowSizeFid = env->GetFieldID(cls, "maxWindowSize", "I");
    fdFftFid = env->GetFieldID(cls, "fft", "[[F");
    fdWindowCountFid = env->GetFieldID(cls, "windowCount", "I");;
    fdWindowSizeFid = env->GetFieldID(cls, "windowSize", "I");

    // let's cache fields of the FftDrawData java object
    cls = env->FindClass("com/backyardbrains/drawing/FftDrawData");
    fddVerticesFid = env->GetFieldID(cls, "vertices", "[F");
    fddIndicesFid = env->GetFieldID(cls, "indices", "[S");
    fddColorsFid = env->GetFieldID(cls, "colors", "[F");
    fddVertexCountFid = env->GetFieldID(cls, "vertexCount", "I");
    fddIndexCountFid = env->GetFieldID(cls, "indexCount", "I");
    fddColorCountFid = env->GetFieldID(cls, "colorCount", "I");
    fddScaleXFid = env->GetFieldID(cls, "scaleX", "F");
    fddScaleYFid = env->GetFieldID(cls, "scaleY", "F");

    // let's cache fields of the SpikeIndexValue java object
    cls = env->FindClass("com/backyardbrains/vo/SpikeIndexValue");
    sivValueFid = env->GetFieldID(cls, "value", "F");
    sivIndexFid = env->GetFieldID(cls, "index", "I");

    // let's cache fields of the SpikesDrawData java object
    cls = env->FindClass("com/backyardbrains/drawing/SpikesDrawData");
    spddVerticesFid = env->GetFieldID(cls, "vertices", "[F");
    spddColorsFid = env->GetFieldID(cls, "colors", "[F");
    spddVertexCountFid = env->GetFieldID(cls, "vertexCount", "I");
    spddColorCountFid = env->GetFieldID(cls, "colorCount", "I");

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
    jint channelCount = env->GetIntField(in, sdChannelCountFid);
    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(in, sdSamplesFid));
    auto inSampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(in, sdSampleCountsFid));

    jint *inSampleCountsPtr = new jint[channelCount];
    env->GetIntArrayRegion(inSampleCounts, 0, channelCount, inSampleCountsPtr);

    // check if we didn't get any samples... in that case we can return
    jint frameCount = INT_MAX;
    for (int i = 0; i < channelCount; i++) {
        if (inSampleCountsPtr[i] == 0) return 0;
        if (inSampleCountsPtr[i] < frameCount) frameCount = inSampleCountsPtr[i];
    }
    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        inSamplesPtr[i] = new jshort[frameCount]{0};
        env->GetShortArrayRegion(reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samples, i)), 0,
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
    jshort *outSamplesPtr = backyardbrains::utils::SignalUtils::interleaveSignal(inSamplesPtr, frameCount,
                                                                                 channelCount);

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

extern "C" JNIEXPORT jfloat JNICALL
Java_com_backyardbrains_utils_JniUtils_rms(JNIEnv *env, jclass type, jshortArray in, jint length) {
    auto *samplesPtr = new jshort[length];
    env->GetShortArrayRegion(in, 0, length, samplesPtr);

    // exception check
    if (exception_check(env)) {
        delete[] samplesPtr;
    }

    const float rms = backyardbrains::utils::AnalysisUtils::RMS(samplesPtr, length);

    delete[] samplesPtr;

    return rms;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSampleRate(JNIEnv *env, jclass type, jint sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SAMPLE RATE: %1d", sampleRate);

    amModulationProcessor->setSampleRate(sampleRate);
    sampleStreamProcessor->setSampleRate(sampleRate);
    thresholdProcessor->setSampleRate(sampleRate);
    fftProcessor->setSampleRate(sampleRate);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setChannelCount(JNIEnv *env, jclass type, jint channelCount) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "CHANNEL COUNT: %1d", channelCount);

    amModulationProcessor->setChannelCount(channelCount);
    sampleStreamProcessor->setChannelCount(channelCount);
    thresholdProcessor->setChannelCount(channelCount);
    fftProcessor->setChannelCount(channelCount);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setSelectedChannel(JNIEnv *env, jclass type, jint selectedChannel) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SELECTED CHANNEL: %1d", selectedChannel);

    amModulationProcessor->setSelectedChannel(selectedChannel);
    sampleStreamProcessor->setSelectedChannel(selectedChannel);
    thresholdProcessor->setSelectedChannel(selectedChannel);
    fftProcessor->setSelectedChannel(selectedChannel);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setBandFilter(JNIEnv *env, jclass type, jfloat lowCutOffFreq,
                                                     jfloat highCutOffFreq) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "BAND FILTER: %1f, %2f", lowCutOffFreq, highCutOffFreq);

    amModulationProcessor->setBandFilter(lowCutOffFreq, highCutOffFreq);
    sampleStreamProcessor->setBandFilter(lowCutOffFreq, highCutOffFreq);
    thresholdProcessor->setBandFilter(lowCutOffFreq, highCutOffFreq);
    fftProcessor->setBandFilter(lowCutOffFreq, highCutOffFreq);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_setNotchFilter(JNIEnv *env, jclass type, jfloat centerFreq) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "NOTCH FILTER: %1f", centerFreq);

    amModulationProcessor->setNotchFilter(centerFreq);
    sampleStreamProcessor->setNotchFilter(centerFreq);
    thresholdProcessor->setNotchFilter(centerFreq);
    fftProcessor->setNotchFilter(centerFreq);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processSampleStream(JNIEnv *env, jclass type, jobject out, jbyteArray inBytes,
                                                           jint length, jobject sampleSourceObject) {
    jint channelCount = env->GetIntField(out, sdChannelCountFid);
    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(out, sdSamplesFid));
    auto sampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(out, sdSampleCountsFid));
    auto eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, sdEventIndicesFid));
    auto eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, sdEventNamesFid));

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

    jint *channelSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        auto channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samples, i));
        env->SetShortArrayRegion(channelSamples, 0, outSampleCounts[i], outSamplesPtr[i]);
        env->SetObjectArrayElement(samples, i, channelSamples);
        channelSampleCounts[i] = outSampleCounts[i];
    }
    env->SetIntArrayRegion(sampleCounts, 0, channelCount, channelSampleCounts);
    env->SetIntArrayRegion(eventIndices, 0, outEventCount, outEventIndicesPtr);
    env->SetIntField(out, sdEventCountFid, outEventCount);
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
    jint channelCount = env->GetIntField(out, sdChannelCountFid);
    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(out, sdSamplesFid));
    auto sampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(out, sdSampleCountsFid));

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
        backyardbrains::utils::JniHelper::invokeStaticVoid(vm, "onAmDemodulationChange", "(Z)V",
                                                           isReceivingAmSignalAfter);
    }

    // exception check
    if (exception_check(env)) {
        delete[] inBytesPtr;
        for (int i = 0; i < channelCount; i++)
            delete[] outSamplesPtr[i];
        delete[] outSamplesPtr;
        return;
    }

    jint *deinterleavedSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        auto deinterleavedSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samples, i));
        env->SetShortArrayRegion(deinterleavedSamples, 0, frameCount, outSamplesPtr[i]);
        deinterleavedSampleCounts[i] = frameCount;
    }
    env->SetIntArrayRegion(sampleCounts, 0, channelCount, deinterleavedSampleCounts);

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
    jint channelCount = env->GetIntField(out, sdChannelCountFid);
    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(out, sdSamplesFid));
    auto sampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(out, sdSampleCountsFid));
    auto eventIndices = reinterpret_cast<jintArray>(env->GetObjectField(out, sdEventIndicesFid));
    auto eventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(out, sdEventNamesFid));

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
    backyardbrains::utils::SignalUtils::deinterleaveSignal(outSamplesPtr, reinterpret_cast<short *>(inBytesPtr),
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

    jint *deinterleavedSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        auto deinterleavedSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samples, i));
        env->SetShortArrayRegion(deinterleavedSamples, 0, frameCount, outSamplesPtr[i]);
        deinterleavedSampleCounts[i] = frameCount;
    }
    env->SetIntArrayRegion(sampleCounts, 0, channelCount, deinterleavedSampleCounts);
    env->SetIntArrayRegion(eventIndices, 0, eventCounter, outEventIndicesPtr);
    env->SetIntField(out, sdEventCountFid, eventCounter);
    env->SetLongField(out, sdLastSampleIndexFid, prepend + end);
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
    jint channelCount = env->GetIntField(out, sdChannelCountFid);
    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(in, sdSamplesFid));
    auto sampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(in, sdSampleCountsFid));

    jint *inSampleCountsPtr = new jint[channelCount];
    env->GetIntArrayRegion(sampleCounts, 0, channelCount, inSampleCountsPtr);

    // check if we didn't get any samples... in that case we can return
    for (int i = 0; i < channelCount; i++) {
        if (inSampleCountsPtr[i] == 0) return;
    }

    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        inSamplesPtr[i] = new jshort[inSampleCountsPtr[i]]{0};
        env->GetShortArrayRegion(reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samples, i)), 0,
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

    thresholdProcessor->setChannelCount(channelCount);
    if (!averageSamples) {
        thresholdProcessor->appendIncomingSamples(inSamplesPtr, inSampleCountsPtr);

        delete[] inSampleCountsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;

        return;
    }

    auto outSamples = reinterpret_cast<jobjectArray>(env->GetObjectField(out, sdSamplesFid));
    auto outSampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(out, sdSampleCountsFid));
    auto inEventIndices = reinterpret_cast<jintArray>(env->GetObjectField(in, sdEventIndicesFid));
    auto inEventNames = reinterpret_cast<jobjectArray>(env->GetObjectField(in, sdEventNamesFid));
    jint inEventCount = env->GetIntField(in, sdEventCountFid);

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
    for (int i = 0; i < channelCount; i++) {
        auto channelSamples = reinterpret_cast<jshortArray>(env->GetObjectArrayElement(outSamples, i));
        env->SetShortArrayRegion(channelSamples, 0, averagedSampleCount[i], outSamplesPtr[i]);
        env->SetObjectArrayElement(outSamples, i, channelSamples);
        env->DeleteLocalRef(channelSamples);

        channelSampleCounts[i] = averagedSampleCount[i];
    }
    env->SetIntArrayRegion(outSampleCounts, 0, channelCount, channelSampleCounts);

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

extern "C"
JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_resetFft(JNIEnv *env, jclass type) {
    fftProcessor->resetFft();
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_processFft(JNIEnv *env, jclass type, jobject out, jobject in,
                                                  jfloat fftSampleRate, jint downsamplingFactor) {
    jint channelCount = env->GetIntField(in, sdChannelCountFid);
    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(in, sdSamplesFid));
    auto sampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(in, sdSampleCountsFid));

    jint *inSampleCountsPtr = new jint[channelCount];
    env->GetIntArrayRegion(sampleCounts, 0, channelCount, inSampleCountsPtr);

    // check if we didn't get any samples... in that case we can return
    for (int i = 0; i < channelCount; i++) {
        if (inSampleCountsPtr[i] == 0) return;
    }

    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        inSamplesPtr[i] = new jshort[inSampleCountsPtr[i]]{0};
        env->GetShortArrayRegion(reinterpret_cast<jshortArray>(env->GetObjectArrayElement(samples, i)), 0,
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

    auto maxWindowCount = env->GetIntField(out, fdMaxWindowCountFid);
    auto maxWindowSize = env->GetIntField(out, fdMaxWindowSizeFid);
    auto outFft = reinterpret_cast<jobjectArray>(env->GetObjectField(out, fdFftFid));

    auto **outFftPtr = new jfloat *[maxWindowCount];
    for (int i = 0; i < maxWindowCount; i++) outFftPtr[i] = new jfloat[maxWindowSize]{0};
    uint32_t windowCount = 0, windowSize = 0;
    fftProcessor->process(outFftPtr, windowCount, windowSize, channelCount, inSamplesPtr,
                          reinterpret_cast<uint32_t *>(inSampleCountsPtr), fftSampleRate, downsamplingFactor);

    // exception check
    if (exception_check(env)) {
        delete[] inSampleCountsPtr;
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        for (int i = 0; i < maxWindowCount; i++) {
            delete[] outFftPtr[i];
        }
        delete[] outFftPtr;
        return;
    }

    for (int i = 0; i < windowCount; i++) {
        auto fft = reinterpret_cast<jfloatArray >(env->GetObjectArrayElement(outFft, i));
        env->SetFloatArrayRegion(fft, 0, windowSize, outFftPtr[i]);
        env->SetObjectArrayElement(outFft, i, fft);
        env->DeleteLocalRef(fft);
    }
    env->SetIntField(out, fdWindowCountFid, windowCount);
    env->SetIntField(out, fdWindowSizeFid, windowSize);

    delete[] inSampleCountsPtr;
    for (int i = 0; i < channelCount; i++) {
        delete[] inSamplesPtr[i];
    }
    delete[] inSamplesPtr;
    for (int i = 0; i < maxWindowCount; i++) {
        delete[] outFftPtr[i];
    }
    delete[] outFftPtr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForSignalDrawing(JNIEnv *env, jclass type, jobject outSignal,
                                                               jobject outEvents, jobjectArray inSignal,
                                                               jint inFrameCount, jintArray inEventIndices,
                                                               jint inEventCount, jint drawStartIndex,
                                                               jint drawEndIndex, jint drawSurfaceWidth) {
    jint channelCount = env->GetArrayLength(inSignal);
    auto **inSamplesPtr = new jshort *[channelCount];
    for (int i = 0; i < channelCount; ++i) {
        auto tmpSamples = (jshortArray) env->GetObjectArrayElement(inSignal, i);
        inSamplesPtr[i] = new jshort[inFrameCount];
        env->GetShortArrayRegion(tmpSamples, 0, inFrameCount, inSamplesPtr[i]);
        env->DeleteLocalRef(tmpSamples);
    }

    jint *inEventIndicesPtr = new jint[inEventCount];
    env->GetIntArrayRegion(inEventIndices, 0, inEventCount, inEventIndicesPtr);

    // exception check
    if (exception_check(env)) {
        for (int i = 0; i < channelCount; i++) {
            delete[] inSamplesPtr[i];
        }
        delete[] inSamplesPtr;
        delete[] inEventIndicesPtr;
        return;
    }

    jint maxSampleCount = drawSurfaceWidth * 5; // can't be more than x5 when enveloping (deducted from testing)
    jint maxEventCount = 100;
    auto **outSamplesPtr = new jfloat *[channelCount];
    for (int i = 0; i < channelCount; i++)
        outSamplesPtr[i] = new jfloat[maxSampleCount]{0};
    auto *outSampleCountsPtr = new jint[channelCount]{0};
    auto *outEventIndicesPtr = new jfloat[maxEventCount];
    jint outEventCount = 0;
    backyardbrains::utils::DrawingUtils::prepareSignalForDrawing(outSamplesPtr, outSampleCountsPtr, outEventIndicesPtr,
                                                                 outEventCount, inSamplesPtr, channelCount,
                                                                 inEventIndicesPtr, inEventCount, drawStartIndex,
                                                                 drawEndIndex, drawSurfaceWidth);

    auto samples = reinterpret_cast<jobjectArray>(env->GetObjectField(outSignal, sddSamplesFid));
    auto sampleCounts = reinterpret_cast<jintArray>(env->GetObjectField(outSignal, sddSampleCountsFid));
    auto eventIndices = reinterpret_cast<jfloatArray>(env->GetObjectField(outEvents, eddEventIndicesFid));

    jint *channelSampleCounts = new jint[channelCount];
    for (int i = 0; i < channelCount; i++) {
        auto channelSamples = reinterpret_cast<jfloatArray >(env->GetObjectArrayElement(samples, i));
        env->SetFloatArrayRegion(channelSamples, 0, outSampleCountsPtr[i], outSamplesPtr[i]);
        env->SetObjectArrayElement(samples, i, channelSamples);
        env->DeleteLocalRef(channelSamples);

        channelSampleCounts[i] = outSampleCountsPtr[i];
    }
    env->SetIntArrayRegion(sampleCounts, 0, channelCount, channelSampleCounts);
    env->SetFloatArrayRegion(eventIndices, 0, outEventCount, outEventIndicesPtr);
    env->SetIntField(outEvents, eddEventCountFid, outEventCount);

    for (int i = 0; i < channelCount; i++) {
        delete[] inSamplesPtr[i];
    }
    delete[] inSamplesPtr;
    delete[] inEventIndicesPtr;
    for (int i = 0; i < channelCount; i++) {
        delete[] outSamplesPtr[i];
    }
    delete[] outSamplesPtr;
    delete[] outSampleCountsPtr;
    delete[] outEventIndicesPtr;
    delete[] channelSampleCounts;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForThresholdDrawing(JNIEnv *env, jclass type, jobject outSignal,
                                                                  jobject outEvents, jobjectArray inSignal,
                                                                  jint inFrameCount, jintArray inEventIndices,
                                                                  jint inEventCount, jint drawStartIndex,
                                                                  jint rawEndIndex, jint drawSurfaceWidth) {
    int drawSamplesCount = rawEndIndex - drawStartIndex;
    int from = (int) ((inFrameCount - drawSamplesCount) * .5);
    int to = (int) ((inFrameCount + drawSamplesCount) * .5);

    Java_com_backyardbrains_utils_JniUtils_prepareForSignalDrawing(env, type, outSignal, outEvents, inSignal,
                                                                   inFrameCount, inEventIndices, inEventCount, from, to,
                                                                   drawSurfaceWidth);
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForFftDrawing(JNIEnv *env, jclass type, jobject out, jobjectArray in,
                                                            jint drawStartIndex, jint drawEndIndex,
                                                            jint drawSurfaceWidth, jint drawSurfaceHeight,
                                                            jfloat fftScaleFactor) {
    jint windowSize = -1;
    jint maxWindowCount = env->GetArrayLength(in);
    jint windowCount = ((drawEndIndex - drawStartIndex) * maxWindowCount) / drawEndIndex;

    // if there is no fft windows just return
    if (windowCount == 0) return;

    auto **inFftPtr = new jfloat *[windowCount];
    jint index;
    for (int i = 0; i < windowCount; ++i) {
        index = maxWindowCount - windowCount + i;
        auto tmpSamples = (jfloatArray) env->GetObjectArrayElement(in, index);
        windowSize = /*static_cast<jint>(*/env->GetArrayLength(tmpSamples)/* / fftScaleFactor)*/;
        inFftPtr[i] = new jfloat[windowSize];
        env->GetFloatArrayRegion(tmpSamples, 0, windowSize, inFftPtr[i]);
        env->DeleteLocalRef(tmpSamples);
    }

    // exception check
    if (exception_check(env)) {
        for (int i = 0; i < windowCount; i++) {
            delete[] inFftPtr[i];
        }
        delete[] inFftPtr;
        return;
    }

    // if fft windows are empty just return
    if (windowSize < 0) {
        for (int i = 0; i < windowCount; i++) {
            delete[] inFftPtr[i];
        }
        delete[] inFftPtr;
        return;
    }

    auto outVertices = reinterpret_cast<jfloatArray>(env->GetObjectField(out, fddVerticesFid));
    auto outIndices = reinterpret_cast<jshortArray>(env->GetObjectField(out, fddIndicesFid));
    auto outColors = reinterpret_cast<jfloatArray>(env->GetObjectField(out, fddColorsFid));
    auto *outVerticesPtr = new jfloat[env->GetArrayLength(outVertices)];
    auto *outIndicesPtr = new jshort[env->GetArrayLength(outIndices)];
    auto *outColorsPtr = new jfloat[env->GetArrayLength(outColors)];
    jint outVertexCount = 0, outIndexCount = 0, outColorCount = 0;
    backyardbrains::utils::DrawingUtils::prepareFftForDrawing(outVerticesPtr, outIndicesPtr, outColorsPtr,
                                                              outVertexCount, outIndexCount, outColorCount,
                                                              inFftPtr, windowCount, windowSize, drawSurfaceWidth,
                                                              drawSurfaceHeight);

    env->SetFloatArrayRegion(outVertices, 0, outVertexCount, outVerticesPtr);
    env->SetShortArrayRegion(outIndices, 0, outIndexCount, outIndicesPtr);
    env->SetFloatArrayRegion(outColors, 0, outColorCount, outColorsPtr);
    env->SetIntField(out, fddVertexCountFid, outVertexCount);
    env->SetIntField(out, fddIndexCountFid, outIndexCount);
    env->SetIntField(out, fddColorCountFid, outColorCount);
    env->SetFloatField(out, fddScaleXFid, (jfloat) maxWindowCount / windowCount);
    env->SetFloatField(out, fddScaleYFid, fftScaleFactor);

    for (int i = 0; i < windowCount; i++) {
        delete[] inFftPtr[i];
    }
    delete[] inFftPtr;
    delete[] outVerticesPtr;
    delete[] outIndicesPtr;
    delete[] outColorsPtr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_backyardbrains_utils_JniUtils_prepareForSpikesDrawing(JNIEnv *env, jclass type, jobject out, jobjectArray in,
                                                               jfloatArray colorInRange, jfloatArray colorOutOfRange,
                                                               jint rangeStart, jint rangeEnd, jint sampleStartIndex,
                                                               jint sampleEndIndex, jint drawStartIndex,
                                                               jint drawEndIndex, jint sampleCount,
                                                               jint drawSurfaceWidth) {
    jint spikeCount = env->GetArrayLength(in);

    // if there is no spikes just return
    if (spikeCount == 0) {
        env->SetIntField(out, spddVertexCountFid, 0);
        env->SetIntField(out, spddColorCountFid, 0);
        return;
    }

    auto *inSpikeValuesPtr = new jfloat[spikeCount];
    auto *inSpikeIndicesPtr = new jint[spikeCount];
    for (int i = 0; i < spikeCount; ++i) {
        auto tmpSpike = (jobject) env->GetObjectArrayElement(in, i);
        inSpikeValuesPtr[i] = env->GetFloatField(tmpSpike, sivValueFid);
        inSpikeIndicesPtr[i] = env->GetIntField(tmpSpike, sivIndexFid);
        env->DeleteLocalRef(tmpSpike);
    }
    auto *colorInPtr = new jfloat[4]; // (rgba)
    env->GetFloatArrayRegion(colorInRange, 0, 4, colorInPtr);
    auto *colorOutPtr = new jfloat[4]; // (rgba)
    env->GetFloatArrayRegion(colorOutOfRange, 0, 4, colorOutPtr);

    // exception check
    if (exception_check(env)) {
        delete[] inSpikeValuesPtr;
        delete[] inSpikeIndicesPtr;
        delete[] colorInPtr;
        delete[] colorOutPtr;
        return;
    }

    auto outVertices = reinterpret_cast<jfloatArray>(env->GetObjectField(out, spddVerticesFid));
    auto outColors = reinterpret_cast<jfloatArray>(env->GetObjectField(out, spddColorsFid));
    auto *outSpikeVerticesPtr = new jfloat[env->GetArrayLength(outVertices)];
    auto *outSpikeColorsPtr = new jfloat[env->GetArrayLength(outColors)];
    jint outVertexCount = 0, outColorCount = 0;
    backyardbrains::utils::DrawingUtils::prepareSpikesForDrawing(outSpikeVerticesPtr, outSpikeColorsPtr, outVertexCount,
                                                                 outColorCount, inSpikeValuesPtr, inSpikeIndicesPtr,
                                                                 spikeCount, colorInPtr, colorOutPtr, rangeStart,
                                                                 rangeEnd, sampleStartIndex, sampleEndIndex,
                                                                 drawStartIndex, drawEndIndex, sampleCount,
                                                                 drawSurfaceWidth);

    env->SetFloatArrayRegion(outVertices, 0, outVertexCount, outSpikeVerticesPtr);
    env->SetFloatArrayRegion(outColors, 0, outColorCount, outSpikeColorsPtr);
    env->SetIntField(out, spddVertexCountFid, outVertexCount);
    env->SetIntField(out, spddColorCountFid, outColorCount);

    delete[] inSpikeValuesPtr;
    delete[] inSpikeIndicesPtr;
    delete[] colorInPtr;
    delete[] colorOutPtr;
    delete[] outSpikeVerticesPtr;
    delete[] outSpikeColorsPtr;
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

    autocorrelationAnalysis->process(spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, analysisPtr,
                                     analysisBinCount);

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
                                                   jint spikeTrainCount, jintArray spikeCounts,
                                                   jobjectArray analysis,
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

    crossCorrelationAnalysis->process(spikeTrainsPtr, spikeTrainCount, spikeCountsPtr, analysisPtr,
                                      analysisBinCount);

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
                                                            jobjectArray normAverageSpike,
                                                            jobjectArray normTopStdLine,
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