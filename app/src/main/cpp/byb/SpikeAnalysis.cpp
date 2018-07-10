//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SpikeAnalysis.h"

const char *SpikeAnalysis::TAG = "SpikeAnalysis";

SpikeAnalysis::SpikeAnalysis() {
}

SpikeAnalysis::~SpikeAnalysis() {
}

long long SpikeAnalysis::currentTimeInMilliseconds() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return ((tv.tv_sec * 1000) + (tv.tv_usec / 1000));
}

int *SpikeAnalysis::findSpikes(const char *filePath, short *outValuesPos, int *outIndicesPos, float *outTimesPos,
                               short *outValuesNeg, int *outIndicesNeg, float *outTimesNeg) {
    long long start = currentTimeInMilliseconds();

    // open audio file we need to analyze
    drwav *wavPtr = drwav_open_file(filePath);
    if (wavPtr == NULL) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", filePath);
        return new int[2]{0};
    }

    // check whether file is long enough for processing
    drwav_uint64 totalSamples = wavPtr->totalSampleCount;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file sample count is: %ld", static_cast<long>(totalSamples));
    if (totalSamples < wavPtr->sampleRate * MIN_VALID_FILE_LENGTH_IN_SECS) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "File to short! Don't process!");
        return new int[2]{0};
    }

    // determine buffer size
    drwav_uint64 bufferSize = static_cast<drwav_uint64>(ceil(totalSamples / BIN_COUNT));
    drwav_uint64 maxBufferSize = static_cast<drwav_uint64>(ceil(
            (wavPtr->sampleRate * BUFFER_SIZE_IN_SECS) / wavPtr->channels));
    if (bufferSize > maxBufferSize) bufferSize = maxBufferSize;
    drwav_int16 *samples = new drwav_int16[bufferSize];

    // 1. FIRST LET'S FIND STANDARD DEVIATIONS FOR EVERY CHUNK and
    float *standardDeviationsArr = new float[static_cast<drwav_uint64>(ceil(totalSamples / bufferSize))];
    int deviationCounter = 0;

    drwav_uint64 read;
    while ((read = drwav_read_s16(wavPtr, bufferSize, samples)) > 0) {
        standardDeviationsArr[deviationCounter++] = AnalysisUtils::SD(samples, static_cast<int>(read));
    }
    delete[] samples;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FINDING DEVIATIONS",
                        static_cast<long>(currentTimeInMilliseconds() - start));

    // 2. SORT DEVIATIONS ASCENDING
    std::sort(standardDeviationsArr, standardDeviationsArr + deviationCounter, std::greater<float>());
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER SORTING AND REVERSING DEVIATIONS",
                        static_cast<long>(currentTimeInMilliseconds() - start));


    // 3. DETERMINE ACCEPTABLE SPIKE VALUES WHICH ARE VALUES GRATER THEN 40% OF SDTs MULTIPLIED BY 2 and
    float tmpSig = 2 * standardDeviationsArr[(int) ceil(deviationCounter * 0.4f)];
    short sig = static_cast<short>(tmpSig > SHRT_MAX ? SHRT_MAX : tmpSig);
    float tmpNegSig = -1 * sig; // we need it for negative values as well
    short negSig = static_cast<short>(tmpNegSig < SHRT_MIN ? SHRT_MIN : tmpNegSig);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SIG: %d, NEG_SIG: %d", sig, negSig);

    delete[] standardDeviationsArr;

    // 4. FIND THE SPIKES IMPLEMENTING SCHMITT TRIGGER
    int schmittPosState = SCHMITT_OFF;
    int schmittNegState = SCHMITT_OFF;
    short maxPeakValue = SHRT_MIN;
    int maxPeakIndex = 0;
    short minPeakValue = SHRT_MAX;
    int minPeakIndex = 0;
    float maxPeakTime = 0.0f;
    float minPeakTime = 0.0f;

    float sampleRateDivider = (float) 1 / wavPtr->sampleRate;
    float currentTime = 0.0f;
    int index = 0;
    short sample;

    bufferSize = maxBufferSize; // let's use max buffer size
    samples = new short[bufferSize];

    // go to beginning of the file cause we need to run through the file again to find spikes
    drwav_seek_to_sample(wavPtr, 0);

    int spikeCounter = 0, spikeNegCounter = 0;
    while ((read = drwav_read_s16(wavPtr, bufferSize, samples)) > 0) {
        // find peaks
        for (int i = 0; i < read; i++) {
            sample = samples[i];
            // determine state of positive schmitt trigger
            if (schmittPosState == SCHMITT_OFF) {
                if (sample > sig) {
                    schmittPosState = SCHMITT_ON;
                    maxPeakValue = SHRT_MIN;
                }
            } else {
                if (sample < 0) {
                    schmittPosState = SCHMITT_OFF;
                    outValuesPos[spikeCounter] = maxPeakValue;
                    outIndicesPos[spikeCounter] = maxPeakIndex;
                    outTimesPos[spikeCounter] = maxPeakTime;
                    spikeCounter++;
                } else if (sample > maxPeakValue) {
                    maxPeakValue = sample;
                    maxPeakIndex = index;
                    maxPeakTime = currentTime;
                }
            }

            // determine state of negative schmitt trigger
            if (schmittNegState == SCHMITT_OFF) {
                if (sample < negSig) {
                    schmittNegState = SCHMITT_ON;
                    minPeakValue = SHRT_MAX;
                }
            } else {
                if (sample > 0) {
                    schmittNegState = SCHMITT_OFF;
                    outValuesNeg[spikeNegCounter] = minPeakValue;
                    outIndicesNeg[spikeNegCounter] = minPeakIndex;
                    outTimesNeg[spikeNegCounter] = minPeakTime;
                    spikeNegCounter++;
                } else if (sample < minPeakValue) {
                    minPeakValue = sample;
                    minPeakIndex = index;
                    minPeakTime = currentTime;
                }
            }

            index++;
            currentTime += sampleRateDivider;
        }
    }
    delete[] samples;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FINDING SPIKES",
                        static_cast<long>(currentTimeInMilliseconds() - start));
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND POSITIVE: %d", spikeCounter);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND NEGATIVE: %d", spikeNegCounter);

    // close audio file
    drwav_close(wavPtr);

    // 5. FINALLY WE SHOULD FILTER FOUND SPIKES BY APPLYING KILL INTERVAL OF 5ms
    int i;
    int len = spikeCounter;
    int removedCounter = 0;
    if (len > 0) { // Filter positive spikes using kill interval
        for (i = 0; i < len - 1; i++) { // look on the right
            if (outValuesPos[i] < outValuesPos[i + 1]) {
                if ((outTimesPos[i + 1] - outTimesPos[i]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesPos + i + 1, outValuesPos + i + numMoved, outValuesPos + i);
                        std::move(outIndicesPos + i + 1, outIndicesPos + i + numMoved, outIndicesPos + i);
                        std::move(outTimesPos + i + 1, outTimesPos + i + numMoved, outTimesPos + i);
                    }
                    len--;
                    removedCounter++;
                    i--;
                }
            }
        }
        len = i;
        for (i = 1; i < len; i++) { // look on the left neighbor
            if (outValuesPos[i] < outValuesPos[i - 1]) {
                if ((outTimesPos[i] - outTimesPos[i - 1]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesPos + i + 1, outValuesPos + i + numMoved, outValuesPos + i);
                        std::move(outIndicesPos + i + 1, outIndicesPos + i + numMoved, outIndicesPos + i);
                        std::move(outTimesPos + i + 1, outTimesPos + i + numMoved, outTimesPos + i);
                    }
                    len--;
                    removedCounter++;
                    i--;
                }
            }
        }
    }
    i;
    len = spikeNegCounter;
    int removedNegCounter = 0;
    if (len > 0) { // Filter negative spikes using kill interval
        for (i = 0; i < len - 1; i++) { // look on the right
            if (outValuesNeg[i] > outValuesNeg[i + 1]) {
                if ((outTimesNeg[i + 1] - outTimesNeg[i]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesNeg + i + 1, outValuesNeg + i + numMoved, outValuesNeg + i);
                        std::move(outIndicesNeg + i + 1, outIndicesNeg + i + numMoved, outIndicesNeg + i);
                        std::move(outTimesNeg + i + 1, outTimesNeg + i + numMoved, outTimesNeg + i);
                    }
                    len--;
                    removedNegCounter++;
                    i--;
                }
            }
        }
        len = i;
        for (i = 1; i < len; i++) { // look on the left neighbor
            if (outValuesNeg[i] > outValuesNeg[i - 1]) {
                if ((outTimesNeg[i] - outTimesNeg[i - 1]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesNeg + i + 1, outValuesNeg + i + numMoved, outValuesNeg + i);
                        std::move(outIndicesNeg + i + 1, outIndicesNeg + i + numMoved, outIndicesNeg + i);
                        std::move(outTimesNeg + i + 1, outTimesNeg + i + numMoved, outTimesNeg + i);
                    }
                    len--;
                    removedNegCounter++;
                    i--;
                }
            }
        }
    }
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FILTERING SPIKES",
                        static_cast<long>(currentTimeInMilliseconds() - start));
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND POSITIVE: %d", (spikeCounter - removedCounter));
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND NEGATIVE: %d", (spikeNegCounter - removedNegCounter));

    return new int[2]{spikeCounter - removedCounter, spikeNegCounter - removedNegCounter};
}