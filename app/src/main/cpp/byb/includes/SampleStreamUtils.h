//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SAMPLESTREAMUTILS_H
#define SPIKE_RECORDER_ANDROID_SAMPLESTREAMUTILS_H

#include <string>

namespace util {
    class SampleStreamUtils;

    class SampleStreamListener;
}

class OnEventListenerListener {
public:
    /**
     * Triggered when SpikerBox sends hardware type message as a result of inquiry.
     */
    virtual void onSpikerBoxHardwareTypeDetected(int hardwareType) = 0;

    /**
     * Triggered when SpikerBox sends max sample rate and number of channels message as a result of inquiry.
     */
    virtual void onMaxSampleRateAndNumOfChannelsReply(int maxSampleRate, int channelCount) = 0;
};

class SampleStreamUtils {
public:
    /**
     * Whether specified message sent from SpikerBox is a hardware type message.
     */
    static bool isHardwareTypeMsg(std::string message);

    /**
     * Parses specified SpikerBox {@code message} and returns SpikerBox hardware type.
     */
    static int getBoardType(std::string message);

    /**
     * Whether specified {@code message} sent by SpikerBox is message that contains max sample rate and number of
     * channels.
     */
    static bool isSampleRateAndNumOfChannelsMsg(std::string message);

    /**
     * Parses the specified SpikerBox {@code message} and returns max sample rate.
     */
    static int getMaxSampleRate(std::string message);

    /**
     * Parses the specified SpikerBox {@code message} and returns max sample rate.
     */
    static int getChannelCount(std::string message);

    /**
     * Whether specified message sent by SpikerBox is an event message.
     */
    static bool isEventMsg(std::string message);

    /**
     * Parses the event message and returns number representation of the event.
     */
    static std::string getEventNumber(std::string message);

private:
    // Hardware type SpikerBox reply message prefix.
    static const std::string HARDWARE_TYPE_PREFIX;
    // Plant SpikerBox reply message for hardware type inquiry.
    static const std::string HARDWARE_TYPE_PLANT;
    // Muscle SpikerBox reply message for hardware type inquiry.
    static const std::string HARDWARE_TYPE_MUSCLE;
    // Brain & Heart SpikerBox reply message for hardware type inquiry (old 1 channel, new 6 channels).
    static const std::string HARDWARE_TYPE_HEART_AND_BRAIN_6CH;
    // Heart & Brain SpikerBox reply message for hardware type inquiry.
    static const std::string HARDWARE_TYPE_HEART_AND_BRAIN;
    // Neuron PRO SpikerBox reply message for hardware type inquiry.
    static const std::string HARDWARE_TYPE_NEURON_PRO;
    // Muscle PRO SpikerBox reply message for hardware type inquiry.
    static const std::string HARDWARE_TYPE_MUSCLE_PRO;
    // Sample rate SpikerBox reply message prefix
    static const std::string SAMPLE_RATE_PREFIX;
    // Number of channels SpikerBox reply message prefix
    static const std::string NUM_OF_CHANNELS_PREFIX;
    // Event message prefix
    static const std::string EVENT_PREFIX;

    // Unknown board type.
    static const int UNKNOWN = -1;
    // SpikerBox Plant board type.
    static const int PLANT = 0;
    // SpikerBox Muscle board type.
    static const int MUSCLE = 1;
    // SpikerBox Brain & Heart board type.
    static const int HEART = 2;
    // SpikerBox Muscle PRO board type.
    static const int MUSCLE_PRO = 3;
    // SpikerBox Neuron PRO board type.
    static const int NEURON_PRO = 4;

    // Sample rate used throughout the app.
    static const int SAMPLE_RATE = 10000;
};


#endif //SPIKE_RECORDER_ANDROID_SAMPLESTREAMUTILS_H
