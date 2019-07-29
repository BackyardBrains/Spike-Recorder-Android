package com.backyardbrains.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.db.entity.SpikeAnalysis;
import com.backyardbrains.db.entity.Train;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.vo.SpikeIndexValue;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface AnalysisDataSource {

    interface GetAnalysisCallback<T> {
        void onAnalysisLoaded(@NonNull T result);

        void onDataNotAvailable();
    }

    //=================================================
    //  SPIKE ANALYSIS
    //=================================================

    interface SpikeAnalysisCheckCallback {
        void onSpikeAnalysisExistsResult(@Nullable SpikeAnalysis analysis, int trainCount);
    }

    void spikeAnalysisExists(@NonNull String filePath, boolean countNonEmptyTrains,
        @Nullable SpikeAnalysisCheckCallback callback);

    long getSpikeAnalysisId(@NonNull String filePath);

    void saveSpikeAnalysis(@NonNull String filePath, @NonNull Spike[] spikesAnalysis);

    void updateSpikeAnalysisFilePath(String oldFilePath, String newFilePath);

    void deleteSpikeAnalysis(String filePath);

    SpikeIndexValue[] getSpikeAnalysisForIndexRange(long analysisId, int channel, int startIndex, int endIndex);

    SpikeIndexValue[] getSpikeAnalysisByTrainForIndexRange(long trainId, int channel, int startIndex, int endIndex);

    void getSpikeAnalysisTimesByTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<float[][]> callback);

    void getSpikeAnalysisIndicesByTrains(@NonNull String filePath, @Nullable GetAnalysisCallback<int[][]> callback);

    //=================================================
    //  SPIKE TRAINS
    //=================================================

    interface AddSpikeAnalysisTrainCallback {
        void onSpikeAnalysisTrainAdded(int order);
    }

    interface RemoveSpikeAnalysisTrainCallback {
        void onSpikeAnalysisTrainRemoved(int newTrainCount);
    }

    void getSpikeAnalysisTrains(@NonNull String filePath, @Nullable GetAnalysisCallback<Train[]> callback);

    void getSpikeAnalysisTrainsByChannel(@NonNull String filePath, int channel,
        @Nullable GetAnalysisCallback<Train[]> callback);

    void addSpikeAnalysisTrain(@NonNull String filePath, int channelCount,
        @Nullable AddSpikeAnalysisTrainCallback callback);

    void saveSpikeAnalysisTrain(@NonNull String filePath, int channel, int order, @ThresholdOrientation int orientation,
        int value);

    void removeSpikeAnalysisTrain(@NonNull String filePath, int order,
        @Nullable RemoveSpikeAnalysisTrainCallback callback);
}
