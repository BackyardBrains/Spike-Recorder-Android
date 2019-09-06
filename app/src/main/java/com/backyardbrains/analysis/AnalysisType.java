package com.backyardbrains.analysis;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    AnalysisType.NONE, AnalysisType.FIND_SPIKES, AnalysisType.AUTOCORRELATION, AnalysisType.ISI,
    AnalysisType.CROSS_CORRELATION, AnalysisType.AVERAGE_SPIKE, AnalysisType.EVENT_TRIGGERED_AVERAGE
}) public @interface AnalysisType {
    /**
     * Invalid analysis type.
     */
    int NONE = -1;

    /**
     * Find spikes analysis.
     */
    int FIND_SPIKES = 0;

    /**
     * Autocorrelation analysis.
     */
    int AUTOCORRELATION = 1;

    /**
     * Inter Spike Interval analysis.
     */
    int ISI = 2;

    /**
     * Cross-Correlation analysis.
     */
    int CROSS_CORRELATION = 3;

    /**
     * Average Spike analysis.
     */
    int AVERAGE_SPIKE = 4;

    /**
     * Event Triggered Average analysis.
     */
    int EVENT_TRIGGERED_AVERAGE = 5;
}
