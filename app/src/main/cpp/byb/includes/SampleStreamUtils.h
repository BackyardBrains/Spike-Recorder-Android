//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SAMPLESTREAMUTILS_H
#define SPIKE_RECORDER_ANDROID_SAMPLESTREAMUTILS_H

#include <string>

namespace backyardbrains {

    namespace utils {

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

            /**
             * Triggered when SpikerBox sends expansion board type message when one is connected.
             */
            virtual void onExpansionBoardTypeDetection(int expansionBoardType) = 0;
        };

        class SampleStreamUtils {
        public:

            // expansion board not present
            static const int NONE_BOARD_DETACHED = 0;
            // Expansion board with additional inputs
            static const int ADDITIONAL_INPUTS_EXPANSION_BOARD = 1;
            // Hammer expansion board
            static const int HAMMER_EXPANSION_BOARD = 4;
            // Joystick expansion board
            static const int JOYSTICK_EXPANSION_BOARD = 5;

            /**
             * Whether specified message sent from SpikerBox is a hardware type message.
             */
            static bool isHardwareTypeMsg(std::string message);

            /**
             * Parses specified SpikerBox {@code message} and returns SpikerBox hardware type.
             */
            static int getHardwareType(std::string message);

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

            /**
             * Whether specified {@code message} sent by SpikerBox is expansion board type message.
             */
            static bool isExpansionBoardTypeMsg(std::string message);

            /**
             * Parses specified SpikerBox {@code message} and returns SpikerBox expansion board type.
             */
            static int getExpansionBoardType(std::string message);

            static int getResolution(int hardwareType);

            static int getResolutionMultiplier(int hardwareType);

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
            // Humans SpikerBox reply message for hardware type inquiry.
            static const std::string HARDWARE_TYPE_HUMANS;
            // Human -Human Interface
            static const std::string HARDWARE_TYPE_HHIBOX;
            // Sample rate SpikerBox reply message prefix
            static const std::string SAMPLE_RATE_PREFIX;
            // Number of channels SpikerBox reply message prefix
            static const std::string NUM_OF_CHANNELS_PREFIX;
            // Event message prefix
            static const std::string EVENT_PREFIX;
            // Expansion board connection message prefix
            static const std::string EXPANSION_BOARD_TYPE_PREFIX;

            // Unknown hardware type.
            static const int UNKNOWN_HARDWARE = -1;
            // SpikerBox Plant hardware type.
            static const int PLANT_HARDWARE = 0;
            // SpikerBox Muscle hardware type.
            static const int MUSCLE_HARDWARE = 1;
            // SpikerBox Brain & Heart hardware type.
            static const int HEART_HARDWARE = 2;
            // SpikerBox Muscle PRO hardware type.
            static const int MUSCLE_PRO_HARDWARE = 3;
            // SpikerBox Neuron PRO hardware type.
            static const int NEURON_PRO_HARDWARE = 4;
            // SpikerBox Humans hardware type.
            static const int HUMANS_HARDWARE = 5;
            // SpikerBox Humans hardware type.
            static const int HHI_HARDWARE = 6;

            // Sample rate used throughout the app.
            static const int SAMPLE_RATE = 10000;
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_SAMPLESTREAMUTILS_H
