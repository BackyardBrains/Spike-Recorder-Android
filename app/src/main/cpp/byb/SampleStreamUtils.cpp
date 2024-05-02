//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SampleStreamUtils.h"

namespace backyardbrains {

    namespace utils {

        const std::string SampleStreamUtils::HARDWARE_TYPE_PREFIX = "HWT:";
        const std::string SampleStreamUtils::HARDWARE_TYPE_PLANT = SampleStreamUtils::HARDWARE_TYPE_PREFIX + "PLANTSS;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_MUSCLE =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "MUSCLESS;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_HEART_AND_BRAIN_6CH =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "HEARTSS;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_HEART_AND_BRAIN =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "HBLEOSB;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_HUMAN =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "HUMANSB;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_HHIBOX =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "HHIBOX;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_NEURON_PRO =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "NEURONSB;";
        const std::string SampleStreamUtils::HARDWARE_TYPE_MUSCLE_PRO =
                SampleStreamUtils::HARDWARE_TYPE_PREFIX + "MUSCLESB;";
        const std::string SampleStreamUtils::SAMPLE_RATE_PREFIX = "MSF:";
        const std::string SampleStreamUtils::NUM_OF_CHANNELS_PREFIX = "MNC:";
        const std::string SampleStreamUtils::EVENT_PREFIX = "EVNT:";
        const std::string SampleStreamUtils::EVENT_P300 = "p300:";
        const std::string SampleStreamUtils::EVENT_P300_AUDIO = "sound:";
        const std::string SampleStreamUtils::EXPANSION_BOARD_TYPE_PREFIX = "BRD:";

        bool SampleStreamUtils::isHardwareTypeMsg(std::string message) {
            return message.compare(0, HARDWARE_TYPE_PREFIX.length(), HARDWARE_TYPE_PREFIX) == 0;
        }

        int SampleStreamUtils::getHardwareType(std::string message) {
            if (std::strcmp(HARDWARE_TYPE_PLANT.c_str(), message.c_str()) == 0) return PLANT_HARDWARE;
            if (std::strcmp(HARDWARE_TYPE_MUSCLE.c_str(), message.c_str()) == 0) return MUSCLE_HARDWARE;
            if (std::strcmp(HARDWARE_TYPE_HEART_AND_BRAIN_6CH.c_str(), message.c_str()) == 0) return HEART_HARDWARE;
            if (std::strcmp(HARDWARE_TYPE_HEART_AND_BRAIN.c_str(), message.c_str()) == 0) return HEART_HARDWARE;
            if (std::strcmp(HARDWARE_TYPE_HUMAN.c_str(), message.c_str()) == 0) return HUMAN_HARDWARE;
            if (std::strcmp(HARDWARE_TYPE_HHIBOX.c_str(), message.c_str()) == 0) return HHIBOX_HARDWARE;
            if (message.find(HARDWARE_TYPE_NEURON_PRO) != std::string::npos) return NEURON_PRO_HARDWARE;
            if (message.find(HARDWARE_TYPE_MUSCLE_PRO) != std::string::npos) return MUSCLE_PRO_HARDWARE;
            return UNKNOWN_HARDWARE;
        }

        bool SampleStreamUtils::isSampleRateAndNumOfChannelsMsg(std::string message) {
            return message.compare(0, SAMPLE_RATE_PREFIX.length(), SAMPLE_RATE_PREFIX) == 0 &&
                   message.find(NUM_OF_CHANNELS_PREFIX) != std::string::npos;
        }

        int SampleStreamUtils::getMaxSampleRate(std::string message) {
            message = message.replace(0, SAMPLE_RATE_PREFIX.length(), "");
            size_t found = message.find(NUM_OF_CHANNELS_PREFIX);
            if (found == std::string::npos) return SAMPLE_RATE;
            message = message.substr(0, found - 1);
            return std::stoi(message);
        }

        int SampleStreamUtils::getChannelCount(std::string message) {
            size_t found = message.find(NUM_OF_CHANNELS_PREFIX);
            if (found == std::string::npos) return 1;
            message = message.substr(found + NUM_OF_CHANNELS_PREFIX.length());
            message = message.replace(message.length() - 1, 1, "");
            return std::stoi(message);
        }

        bool SampleStreamUtils::isEventMsg(std::string message) {
            return message.compare(0, EVENT_PREFIX.length(), EVENT_PREFIX) == 0;
        }

        std::string SampleStreamUtils::getEventNumber(std::string message) {
            message = message.replace(0, EVENT_PREFIX.length(), "");
            size_t found = message.find(';');
            if (found == std::string::npos) return message;
            return message.replace(found, message.length() - found, "");
        }

        bool SampleStreamUtils::isExpansionBoardTypeMsg(std::string message) {
            return message.compare(0, EXPANSION_BOARD_TYPE_PREFIX.length(), EXPANSION_BOARD_TYPE_PREFIX) == 0;
        }

        int SampleStreamUtils::getExpansionBoardType(std::string message) {
            message = message.replace(0, EXPANSION_BOARD_TYPE_PREFIX.length(), "");
            size_t found = message.find(';');
            if (found == std::string::npos) return std::stoi(message);
            message = message.replace(found, message.length() - found, "");
            return std::stoi(message);
        }
        bool SampleStreamUtils::isHumanSpikerBoxType300(std::string message) {
            return message.compare(0, EVENT_P300.length(), EVENT_P300) == 0;
        }

        int SampleStreamUtils::getHumanSpikerBoxType300(std::string message) {
            message = message.replace(0, EVENT_P300.length(), "");
            size_t found = message.find(';');
            if (found == std::string::npos) return std::stoi(message);
            message = message.replace(found, message.length() - found, "");
            return std::stoi(message);
        }
        bool SampleStreamUtils::isHumanSpikerBoxType300Audio(std::string message) {
            return message.compare(0, EVENT_P300_AUDIO.length(), EVENT_P300_AUDIO) == 0;
        }

        int SampleStreamUtils::getHumanSpikerBoxType300Audio(std::string message) {
            message = message.replace(0, EVENT_P300_AUDIO.length(), "");
            size_t found = message.find(';');
            if (found == std::string::npos) return std::stoi(message);
            message = message.replace(found, message.length() - found, "");
            return std::stoi(message);
        }
    }
}