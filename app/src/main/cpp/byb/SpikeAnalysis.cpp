//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SpikeAnalysis.h"

const char *SpikeAnalysis::TAG = "SpikeAnalysis";

SpikeAnalysis::SpikeAnalysis() = default;

SpikeAnalysis::~SpikeAnalysis() = default;

long long SpikeAnalysis::currentTimeInMilliseconds() {
    struct timeval tv{};
    gettimeofday(&tv, nullptr);
    return ((tv.tv_sec * 1000) + (tv.tv_usec / 1000));
}

void SpikeAnalysis::findSpikes(const char *filePath, short **outValuesPos, int **outIndicesPos, float **outTimesPos,
                               short **outValuesNeg, int **outIndicesNeg, float **outTimesNeg, int channelCount,
                               int *outPosCounts, int *outNegCounts) {
    long long start = currentTimeInMilliseconds();

    // open audio file we need to analyze
    drwav *wavPtr = drwav_open_file(filePath);
    if (wavPtr == nullptr) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", filePath);
        return;
    }

    // check whether file is long enough for processing
    drwav_uint64 totalSamples = wavPtr->totalSampleCount;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file sample count is: %ld", static_cast<long>(totalSamples));
    if (totalSamples < wavPtr->sampleRate * channelCount * MIN_VALID_FILE_LENGTH_IN_SECS) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "File to short! Don't process!");
        return;
    }

    // determine buffer size
    auto bufferSize = static_cast<drwav_uint64>(ceil(totalSamples / BIN_COUNT));
    auto maxBufferSize = static_cast<drwav_uint64>(ceil(
            (wavPtr->sampleRate * BUFFER_SIZE_IN_SECS) / channelCount));
    if (bufferSize > maxBufferSize) bufferSize = maxBufferSize;

    // create buffers
    int deviationsCount = static_cast<int>(ceil(static_cast<float>(totalSamples) / bufferSize));
    auto *samples = new drwav_int16[bufferSize];
    auto **deinterleavedSamples = new drwav_int16 *[channelCount];
    auto **standardDeviations = new float *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        deinterleavedSamples[i] = new drwav_int16[bufferSize]{0};
        standardDeviations[i] = new float[deviationsCount];
    }

    // 1. FIRST LET'S FIND STANDARD DEVIATIONS FOR EVERY CHUNK and
    int *deviationCounters = new int[channelCount]{0};

    drwav_uint64 read;
    int sampleCount = static_cast<int>(bufferSize);
    int frameCount = sampleCount / channelCount;
    while ((read = drwav_read_s16(wavPtr, bufferSize, samples)) > 0) {
        if (sampleCount != static_cast<int>(read)) {
            sampleCount = static_cast<int>(read);
            frameCount = sampleCount / channelCount;
        }
        SignalUtils::deinterleaveSignal(deinterleavedSamples, samples, sampleCount, channelCount);
        for (int i = 0; i < channelCount; i++) {
            standardDeviations[i][deviationCounters[i]++] = AnalysisUtils::SD(deinterleavedSamples[i], frameCount);
        }
    }
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FINDING DEVIATIONS",
                        static_cast<long>(currentTimeInMilliseconds() - start));

    // 2. SORT DEVIATIONS ASCENDING
    for (int i = 0; i < channelCount; i++) {
        std::sort(standardDeviations[i], standardDeviations[i] + deviationCounters[i], std::greater<>());
    }
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER SORTING AND REVERSING DEVIATIONS",
                        static_cast<long>(currentTimeInMilliseconds() - start));


    // 3. DETERMINE ACCEPTABLE SPIKE VALUES WHICH ARE VALUES GRATER THEN 40% OF SDTs MULTIPLIED BY 2 and
    auto *sig = new short[channelCount]{0};
    auto *negSig = new short[channelCount]{0};
    for (int i = 0; i < channelCount; i++) {
        float tmpSig = 2 * standardDeviations[i][(int) ceil(deviationCounters[i] * 0.4f)];
        sig[i] = static_cast<short>(tmpSig > SHRT_MAX ? SHRT_MAX : tmpSig);
        float tmpNegSig = -1 * sig[i]; // we need it for negative values as well
        negSig[i] = static_cast<short>(tmpNegSig < SHRT_MIN ? SHRT_MIN : tmpNegSig);
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "SIG: %d, NEG_SIG: %d", sig[i], negSig[i]);
    }

    delete[] samples;
    for (int i = 0; i < channelCount; i++) {
        delete[] deinterleavedSamples[i];
        delete[] standardDeviations[i];
    }
    delete[] deinterleavedSamples;
    delete[] standardDeviations;
    delete[] deviationCounters;

    // 4. FIND THE SPIKES IMPLEMENTING SCHMITT TRIGGER
    float sampleRateDivider = (float) 1 / wavPtr->sampleRate;
    short sample;

    int *schmittPosState = new int[channelCount];
    int *schmittNegState = new int[channelCount];
    auto *maxPeakValue = new short[channelCount];
    auto *minPeakValue = new short[channelCount];
    int *maxPeakIndex = new int[channelCount]{0};
    int *minPeakIndex = new int[channelCount]{0};
    auto *maxPeakTime = new float[channelCount]{0.0f};
    auto *minPeakTime = new float[channelCount]{0.0f};
    auto *currentTime = new float[channelCount]{0.0f};
    int *currentIndex = new int[channelCount]{0};
    int *spikeCounter = new int[channelCount]{0};
    int *spikeNegCounter = new int[channelCount]{0};

    bufferSize = maxBufferSize; // let's use max buffer size

    samples = new short[bufferSize];
    deinterleavedSamples = new drwav_int16 *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        deinterleavedSamples[i] = new drwav_int16[bufferSize]{0};
        schmittPosState[i] = SCHMITT_OFF;
        schmittNegState[i] = SCHMITT_OFF;
        maxPeakValue[i] = SHRT_MIN;
        minPeakValue[i] = SHRT_MAX;
    }

    // go to beginning of the file cause we need to run through the file again to find spikes
    drwav_seek_to_sample(wavPtr, 0);

    sampleCount = static_cast<int>(bufferSize);
    frameCount = sampleCount / channelCount;
    while ((read = drwav_read_s16(wavPtr, bufferSize, samples)) > 0) {
        if (sampleCount != static_cast<int>(read)) {
            sampleCount = static_cast<int>(read);
            frameCount = sampleCount / channelCount;
        }
        SignalUtils::deinterleaveSignal(deinterleavedSamples, samples, sampleCount, channelCount);
        for (int channel = 0; channel < channelCount; channel++) {
            // find peaks
            for (int i = 0; i < frameCount; i++) {
                sample = deinterleavedSamples[channel][i];
                // determine state of positive schmitt trigger
                if (schmittPosState[channel] == SCHMITT_OFF) {
                    if (sample > sig[channel]) {
                        schmittPosState[channel] = SCHMITT_ON;
                        maxPeakValue[channel] = SHRT_MIN;
                    }
                } else {
                    if (sample < 0) {
                        schmittPosState[channel] = SCHMITT_OFF;
                        outValuesPos[channel][spikeCounter[channel]] = maxPeakValue[channel];
                        outIndicesPos[channel][spikeCounter[channel]] = maxPeakIndex[channel];
                        outTimesPos[channel][spikeCounter[channel]] = maxPeakTime[channel];
                        spikeCounter[channel]++;
                    } else if (sample > maxPeakValue[channel]) {
                        maxPeakValue[channel] = sample;
                        maxPeakIndex[channel] = currentIndex[channel];
                        maxPeakTime[channel] = currentTime[channel];
                    }
                }

                // determine state of negative schmitt trigger
                if (schmittNegState[channel] == SCHMITT_OFF) {
                    if (sample < negSig[channel]) {
                        schmittNegState[channel] = SCHMITT_ON;
                        minPeakValue[channel] = SHRT_MAX;
                    }
                } else {
                    if (sample > 0) {
                        schmittNegState[channel] = SCHMITT_OFF;
                        outValuesNeg[channel][spikeNegCounter[channel]] = minPeakValue[channel];
                        outIndicesNeg[channel][spikeNegCounter[channel]] = minPeakIndex[channel];
                        outTimesNeg[channel][spikeNegCounter[channel]] = minPeakTime[channel];
                        spikeNegCounter[channel]++;
                    } else if (sample < minPeakValue[channel]) {
                        minPeakValue[channel] = sample;
                        minPeakIndex[channel] = currentIndex[channel];
                        minPeakTime[channel] = currentTime[channel];
                    }
                }

                currentIndex[channel]++;
                currentTime[channel] += sampleRateDivider;
            }
        }
    }
    delete[] samples;
    for (int i = 0; i < channelCount; i++) {
        delete[] deinterleavedSamples[i];
    }
    delete[] deinterleavedSamples;
    delete[] sig;
    delete[] negSig;
    delete[] schmittPosState;
    delete[] schmittNegState;
    delete[] maxPeakValue;
    delete[] minPeakValue;
    delete[] maxPeakIndex;
    delete[] minPeakIndex;
    delete[] maxPeakTime;
    delete[] minPeakTime;
    delete[] currentTime;
    delete[] currentIndex;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FINDING SPIKES",
                        static_cast<long>(currentTimeInMilliseconds() - start));
    for (int channel = 0; channel < channelCount; channel++) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND POSITIVE: %d", spikeCounter[channel]);
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND NEGATIVE: %d", spikeNegCounter[channel]);
    }

    // close audio file
    drwav_close(wavPtr);

    // 5. FINALLY WE SHOULD FILTER FOUND SPIKES BY APPLYING KILL INTERVAL OF 5ms
    int i;
    int len;
    int *removedCounter = new int[channelCount]{0};
    int *removedNegCounter = new int[channelCount]{0};
    for (int channel = 0; channel < channelCount; channel++) {
        len = spikeCounter[channel];
        if (len > 0) { // Filter positive spikes using kill interval
            for (i = 0; i < len - 1; i++) { // look on the right
                if (outValuesPos[channel][i] < outValuesPos[channel][i + 1]) {
                    if ((outTimesPos[channel][i + 1] - outTimesPos[channel][i]) < KILL_INTERVAL) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            std::move(outValuesPos[channel] + i + 1, outValuesPos[channel] + i + numMoved,
                                      outValuesPos[channel] + i);
                            std::move(outIndicesPos[channel] + i + 1, outIndicesPos[channel] + i + numMoved,
                                      outIndicesPos[channel] + i);
                            std::move(outTimesPos[channel] + i + 1, outTimesPos[channel] + i + numMoved,
                                      outTimesPos[channel] + i);
                        }
                        len--;
                        removedCounter[channel]++;
                        i--;
                    }
                }
            }
            len = i;
            for (i = 1; i < len; i++) { // look on the left neighbor
                if (outValuesPos[channel][i] < outValuesPos[channel][i - 1]) {
                    if ((outTimesPos[channel][i] - outTimesPos[channel][i - 1]) < KILL_INTERVAL) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            std::move(outValuesPos[channel] + i + 1, outValuesPos[channel] + i + numMoved,
                                      outValuesPos[channel] + i);
                            std::move(outIndicesPos[channel] + i + 1, outIndicesPos[channel] + i + numMoved,
                                      outIndicesPos[channel] + i);
                            std::move(outTimesPos[channel] + i + 1, outTimesPos[channel] + i + numMoved,
                                      outTimesPos[channel] + i);
                        }
                        len--;
                        removedCounter[channel]++;
                        i--;
                    }
                }
            }
        }
        len = spikeNegCounter[channel];
        if (len > 0) { // Filter negative spikes using kill interval
            for (i = 0; i < len - 1; i++) { // look on the right
                if (outValuesNeg[channel][i] > outValuesNeg[channel][i + 1]) {
                    if ((outTimesNeg[channel][i + 1] - outTimesNeg[channel][i]) < KILL_INTERVAL) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            std::move(outValuesNeg[channel] + i + 1, outValuesNeg[channel] + i + numMoved,
                                      outValuesNeg[channel] + i);
                            std::move(outIndicesNeg[channel] + i + 1, outIndicesNeg[channel] + i + numMoved,
                                      outIndicesNeg[channel] + i);
                            std::move(outTimesNeg[channel] + i + 1, outTimesNeg[channel] + i + numMoved,
                                      outTimesNeg[channel] + i);
                        }
                        len--;
                        removedNegCounter[channel]++;
                        i--;
                    }
                }
            }
            len = i;
            for (i = 1; i < len; i++) { // look on the left neighbor
                if (outValuesNeg[channel][i] > outValuesNeg[channel][i - 1]) {
                    if ((outTimesNeg[channel][i] - outTimesNeg[channel][i - 1]) < KILL_INTERVAL) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            std::move(outValuesNeg[channel] + i + 1, outValuesNeg[channel] + i + numMoved,
                                      outValuesNeg[channel] + i);
                            std::move(outIndicesNeg[channel] + i + 1, outIndicesNeg[channel] + i + numMoved,
                                      outIndicesNeg[channel] + i);
                            std::move(outTimesNeg[channel] + i + 1, outTimesNeg[channel] + i + numMoved,
                                      outTimesNeg[channel] + i);
                        }
                        len--;
                        removedNegCounter[channel]++;
                        i--;
                    }
                }
            }
        }
        outPosCounts[channel] = spikeCounter[channel] - removedCounter[channel];
        outNegCounts[channel] = spikeNegCounter[channel] - removedNegCounter[channel];
    }
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FILTERING SPIKES",
                        static_cast<long>(currentTimeInMilliseconds() - start));
    for (int channel = 0; channel < channelCount; channel++) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND POSITIVE: %d",
                            (spikeCounter[channel] - removedCounter[channel]));
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "FOUND NEGATIVE: %d",
                            (spikeNegCounter[channel] - removedNegCounter[channel]));
    }

    delete[] spikeCounter;
    delete[] spikeNegCounter;
    delete[] removedCounter;
    delete[] removedNegCounter;
}