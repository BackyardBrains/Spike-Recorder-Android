//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AnalysisUtils.h"

float AnalysisUtils::SD(short *data, int length) {
    float mean = 0.0f;
    float squares = 0.0f;
    float number;
    for (int i = 0; i < length; i++) {
        number = data[i];
        mean += number;
        squares += number * number;
    }
    mean /= length;
    squares /= length;
    return sqrt(squares - mean * mean);
}

float AnalysisUtils::RMS(short *data, int length) {
    float squares = 0.0f;
    float number;
    for (int i = 0; i < length; i++) {
        number = data[i];
        squares += number * number;
    }
    return sqrt(squares / length);
}
