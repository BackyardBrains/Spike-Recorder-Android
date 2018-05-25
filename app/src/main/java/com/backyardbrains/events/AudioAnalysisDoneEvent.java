package com.backyardbrains.events;

import com.backyardbrains.analysis.BYBAnalysisType;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioAnalysisDoneEvent {

    private final boolean success;
    private final @BYBAnalysisType int type;

    public AudioAnalysisDoneEvent(boolean success, @BYBAnalysisType int type) {
        this.success = success;
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public @BYBAnalysisType int getType() {
        return type;
    }
}
