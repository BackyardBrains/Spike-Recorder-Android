package com.backyardbrains.analysis;

import android.os.Parcel;
import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAveragesConfig extends AnalysisConfig {

    public static final Creator<EventTriggeredAveragesConfig> CREATOR =
        new Creator<EventTriggeredAveragesConfig>() {
            @Override public EventTriggeredAveragesConfig createFromParcel(Parcel in) {
                return new EventTriggeredAveragesConfig(in);
            }

            @Override public EventTriggeredAveragesConfig[] newArray(int size) {
                return new EventTriggeredAveragesConfig[size];
            }
        };

    private String[] events;
    private boolean removeNoiseIntervals;

    public EventTriggeredAveragesConfig(@NonNull String filePath, @AnalysisType int analysisType,
        @NonNull String[] events, boolean removeNoiseIntervals) {
        super(filePath, analysisType);
        this.events = events;
        this.removeNoiseIntervals = removeNoiseIntervals;
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
}
