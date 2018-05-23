/**
 * Created by Tihomir Leka <ticapeca at gmail.com>.
 */

#include <jni.h>
#include <functional>
#include <math.h>
#include <climits>

#define HELLO "Hello from C++"

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

static jshortArray envelope(JNIEnv *env, jshortArray samples, jint start, jint end, jint returnCount) {
    int drawSamplesCount = end - start;
    if (drawSamplesCount % 2 != 0) drawSamplesCount -= 1;
    if (drawSamplesCount < returnCount) returnCount = drawSamplesCount;

    jshort *ptrSamples = new jshort[drawSamplesCount];
    env->GetShortArrayRegion(samples, start, drawSamplesCount, ptrSamples);

    // exception check
    if (exception_check(env)) {
        delete[] ptrSamples;
        return NULL;
    }

    jshort *ptrResult = new jshort[drawSamplesCount];

    jshort sample;
    jshort min = SHRT_MAX, max = SHRT_MIN;
    int samplesPerPixel = drawSamplesCount / returnCount;
    int samplesPerPixelRest = drawSamplesCount % returnCount;
    int samplesPerEnvelopeLow = samplesPerPixel * 2; // multiply by 2 because we save min and max
    int samplesPerEnvelopeHigh = samplesPerEnvelopeLow + 2;
    int envelopeCounter = 0, index = 0;

    for (int i = 0; i < drawSamplesCount; i++) {
        sample = ptrSamples[i];

        if (samplesPerPixel == 1 && samplesPerPixelRest == 0) {
            ptrResult[index++] = sample;
        } else {
            if (sample > max) max = sample;
            if (sample < min) min = sample;

            if (index < samplesPerPixelRest) {
                if (envelopeCounter == samplesPerEnvelopeHigh) {
                    ptrResult[index++] = max;
                    ptrResult[index++] = min;

                    envelopeCounter = 0;
                    min = SHRT_MAX;
                    max = SHRT_MIN;
                }
            } else {
                if (envelopeCounter == samplesPerEnvelopeLow) {
                    ptrResult[index++] = max;
                    ptrResult[index++] = min;

                    envelopeCounter = 0;
                    min = SHRT_MAX;
                    max = SHRT_MIN;
                }
            }

            envelopeCounter++;
        }
    }

    jshortArray result = env->NewShortArray(index);
    if (result == NULL) {
        delete[] ptrSamples;
        delete[] ptrResult;
        return NULL;
    }

    env->SetShortArrayRegion(result, 0, index, ptrResult);
    delete[] ptrSamples;
    delete[] ptrResult;

    // exception check
    if (exception_check(env)) return NULL;

    return result;
}

extern "C"
JNIEXPORT jstring
JNICALL
Java_com_backyardbrains_utils_NativePOC_helloTest(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(HELLO);
}

extern "C"
JNIEXPORT jshortArray
JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                  jint start, jint end, jint returnCount) {
    jshortArray envelopedSamples = envelope(env, samples, start, end, returnCount);
    if (envelopedSamples == NULL) return env->NewShortArray(0);

    int len = env->GetArrayLength(envelopedSamples);
    jshort *ptrSamples = new jshort[len];
    env->GetShortArrayRegion(envelopedSamples, 0, len, ptrSamples);

    // exception check
    if (exception_check(env)) {
        delete[] ptrSamples;
        return env->NewShortArray(0);
    }

    int resultLength = len * 2;
    jshort *ptrResult = new jshort[resultLength];

    int index = 0;
    jshort x = 0;
    for (int i = 0; i < len; i++) {
        ptrResult[index++] = x++;
        ptrResult[index++] = ptrSamples[i];
    }

    jshortArray result = env->NewShortArray(resultLength);
    if (result == NULL) {
        delete[] ptrSamples;
        delete[] ptrResult;
        return env->NewShortArray(0);
    }

    env->SetShortArrayRegion(result, 0, resultLength, ptrResult);
    delete[] ptrSamples;
    delete[] ptrResult;

    // exception check
    if (exception_check(env)) return env->NewShortArray(0);

    return result;
}
extern "C"
JNIEXPORT jshortArray
JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForThresholdDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                   jint start, jint end, jint returnCount) {
    int drawSamplesCount = end - start;
    int samplesCount = env->GetArrayLength(samples);
    int from = (int) ((samplesCount - drawSamplesCount) * .5);
    int to = (int) ((samplesCount + drawSamplesCount) * .5);

    return Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(env, thiz, samples, from, to, returnCount);
}
