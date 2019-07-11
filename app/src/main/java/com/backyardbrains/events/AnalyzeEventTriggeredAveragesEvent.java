package com.backyardbrains.events;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AnalyzeEventTriggeredAveragesEvent {

    private final String filePath;
    private final String[] events;
    private final boolean removeNoiseIntervals;

    public AnalyzeEventTriggeredAveragesEvent(@NonNull String filePath, @NonNull String[] events,
        boolean removeNoiseIntervals) {
        this.filePath = filePath;
        this.events = events;
        this.removeNoiseIntervals = removeNoiseIntervals;
    }

    public String getFilePath() {
        return filePath;
    }

    public String[] getEvents() {
        return events;
    }

    public boolean isRemoveNoiseIntervals() {
        return removeNoiseIntervals;
    }
}
