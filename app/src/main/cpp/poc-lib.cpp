/**
 * Created by Tihomir Leka <ticapeca at gmail.com>.
 */

#include <jni.h>
#include <functional>
#include <math.h>
#include <climits>
//#include <android/log.h>

#define HELLO "Hello from C++"

static void rethrow_cpp_exception_as_java_exception(JNIEnv *env) {
    try {
        throw; // This allows to determine the type of the exception
    } catch (const std::exception &e) {
        /* unknown exception (may derive from std::exception) */
        jclass jc = env->FindClass("java/lang/Error");
        if (jc) env->ThrowNew(jc, e.what());
    } catch (...) {
        /* Oops I missed identifying this exception! */
        jclass jc = env->FindClass("java/lang/Error");
        if (jc)
            env->ThrowNew(jc, "Unidentified exception => "
                    "Improve rethrow_cpp_exception_as_java_exception()");
    }
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

extern "C"
JNIEXPORT jstring
JNICALL
Java_com_backyardbrains_utils_NativePOC_helloTest(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(HELLO);
}

extern "C"
JNIEXPORT jshortArray
JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForThresholdDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                   jint start, jint end) {
//    jshortArray processedSamples =
//    int size = returnCount * 2;
//    if (waveformVertices == null || waveformVertices.length != size) waveformVertices = new short[size];
//    int j = 0; // index of arr
//    try {
//        int start = (int) ((samples.length - returnCount) * .5);
//        int end = (int) ((samples.length + returnCount) * .5);
//        //for (int i = start; i < end && i < samples.length; i++) {
//        //    waveformVertices[j++] = (short) (i - start);
//        //    waveformVertices[j++] = samples[i];
//        //}
//        return NativePOC.prepareForThresholdDrawing(samples, start, end);
//    } catch (ArrayIndexOutOfBoundsException e) {
//        LOGE(TAG, e.getMessage());
//        Crashlytics.logException(e);
//    }
}

extern "C"
JNIEXPORT jshortArray
JNICALL
Java_com_backyardbrains_utils_NativePOC_prepareForWaveformDrawing(JNIEnv *env, jobject thiz, jshortArray samples,
                                                                  jint start, jint end, jint returnCount) {
    int drawSamplesCount = end - start;
    if (drawSamplesCount % 2 != 0) drawSamplesCount -= 1;
    if (drawSamplesCount < returnCount) returnCount = drawSamplesCount;
    int resultLength = returnCount * 2;

    jshort *ptrSamples = new jshort[drawSamplesCount];
    env->GetShortArrayRegion(samples, start, drawSamplesCount, ptrSamples);

    // exception check
    if (exception_check(env)) {
        delete[] ptrSamples;
        return env->NewShortArray(0);
    }

    jshort *ptrResult = new jshort[resultLength];

    jshort sample;
    jshort min = SHRT_MAX, max = SHRT_MIN;
    jshort x = 0;
    int samplesPerPixel = drawSamplesCount / returnCount;
    int samplesPerPixelRest = drawSamplesCount % returnCount;
    int samplesPerEnvelopeLow = samplesPerPixel * 2; // multiply by 2 because we save min and max
    int samplesPerEnvelopeHigh = samplesPerEnvelopeLow + 2;
    int envelopeCounter = 0, index = 0;

//    const char *format = "ENVELOPE: %d, SAMPLE: %d\n";

    for (int i = 0; i < drawSamplesCount; i++) {
        sample = ptrSamples[i];

        if (samplesPerPixel == 1 && samplesPerPixelRest == 0) {
            ptrResult[index++] = x++;
            ptrResult[index++] = sample;
        } else {
            if (sample > max) max = sample;
            if (sample < min) min = sample;

            if (x < samplesPerPixelRest) {
                if (envelopeCounter == samplesPerEnvelopeHigh) {
                    ptrResult[index++] = x++;
                    ptrResult[index++] = max;
                    ptrResult[index++] = x++;
                    ptrResult[index++] = min;

                    envelopeCounter = 0;
                    min = SHRT_MAX;
                    max = SHRT_MIN;
                }
            } else {
                if (envelopeCounter == samplesPerEnvelopeLow) {
                    ptrResult[index++] = x++;
                    ptrResult[index++] = max;
                    ptrResult[index++] = x++;
                    ptrResult[index++] = min;

                    envelopeCounter = 0;
                    min = SHRT_MAX;
                    max = SHRT_MIN;
                }
            }

            envelopeCounter++;
        }
    }

//    __android_log_print(ANDROID_LOG_DEBUG, "poc-lib", "====================================");
//    __android_log_print(ANDROID_LOG_DEBUG, "poc-lib", format, envelopeCounter, x);

    jshortArray result = env->NewShortArray(x * 2);
    if (result == NULL) {
        delete[] ptrSamples;
        delete[] ptrResult;
        return env->NewShortArray(0);
    }

    env->SetShortArrayRegion(result, 0, x * 2, ptrResult);
    delete[] ptrSamples;
    delete[] ptrResult;

    // exception check
    if (exception_check(env)) return env->NewShortArray(0);

    return result;

}