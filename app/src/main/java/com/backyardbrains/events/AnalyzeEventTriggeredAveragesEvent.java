package com.backyardbrains.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AnalyzeEventTriggeredAveragesEvent {

    private final String filePath;
    private final String[] events;
    private final boolean removeNoiseIntervals;
    private final String confidenceIntervalsEvent;

    public AnalyzeEventTriggeredAveragesEvent(@NonNull String filePath, @NonNull String[] events,
        boolean removeNoiseIntervals, @Nullable String confidenceIntervalsEvent) {
        this.filePath = filePath;
        this.events = events;
        this.removeNoiseIntervals = removeNoiseIntervals;
        this.confidenceIntervalsEvent = confidenceIntervalsEvent;
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

    @Nullable public String getConfidenceIntervalsEvent() {
        return confidenceIntervalsEvent;
    }
}
