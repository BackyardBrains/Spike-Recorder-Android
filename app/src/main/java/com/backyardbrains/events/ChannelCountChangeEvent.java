package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ChannelCountChangeEvent {

    private final int channelCount;

    public ChannelCountChangeEvent(int channelCount) {
        this.channelCount = channelCount;
    }

    public int getChannelCount() {
        return channelCount;
    }
}
