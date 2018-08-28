package com.backyardbrains.events;

import com.backyardbrains.analysis.AnalysisType;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioAnalysisDoneEvent {

    private final boolean success;
    private final @AnalysisType int type;

    public AudioAnalysisDoneEvent(boolean success, @AnalysisType int type) {
        this.success = success;
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public @AnalysisType int getType() {
        return type;
    }
}
