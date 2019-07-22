package com.backyardbrains.analysis;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAveragesConfig extends AnalysisConfig {

    public static final Creator<EventTriggeredAveragesConfig> CREATOR = new Creator<EventTriggeredAveragesConfig>() {
        @Override public EventTriggeredAveragesConfig createFromParcel(Parcel in) {
            return new EventTriggeredAveragesConfig(in);
        }

        @Override public EventTriggeredAveragesConfig[] newArray(int size) {
            return new EventTriggeredAveragesConfig[size];
        }
    };

    private String[] events;
    private boolean removeNoiseIntervals;
    private String confidenceIntervalsEvent;

    public EventTriggeredAveragesConfig(@NonNull String filePath, @AnalysisType int analysisType,
        @NonNull String[] events, boolean removeNoiseIntervals, @Nullable String confidenceIntervalsEvent) {
        super(filePath, analysisType);
        this.events = events;
        this.removeNoiseIntervals = removeNoiseIntervals;
        this.confidenceIntervalsEvent = confidenceIntervalsEvent;
    }

    @SuppressWarnings("WeakerAccess") protected EventTriggeredAveragesConfig(Parcel in) {
        super(in);
        in.readStringArray(events);
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(events);
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
