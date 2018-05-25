package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SampleRateChangeEvent {

    private final int sampleRate;

    public SampleRateChangeEvent(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
