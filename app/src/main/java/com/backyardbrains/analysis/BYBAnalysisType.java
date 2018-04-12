package com.backyardbrains.analysis;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    BYBAnalysisType.NONE, BYBAnalysisType.FIND_SPIKES, BYBAnalysisType.AUTOCORRELATION, BYBAnalysisType.ISI,
    BYBAnalysisType.CROSS_CORRELATION, BYBAnalysisType.AVERAGE_SPIKE
}) public @interface BYBAnalysisType {
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
}
