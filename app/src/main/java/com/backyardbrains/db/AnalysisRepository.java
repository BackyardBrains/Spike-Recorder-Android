package com.backyardbrains.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.db.entity.SpikeAnalysis;
import com.backyardbrains.db.entity.Train;
import com.backyardbrains.db.source.AnalysisLocalDataSource;
import com.backyardbrains.utils.AppExecutors;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.vo.SpikeIndexValue;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AnalysisRepository {

    private static AnalysisRepository INSTANCE = null;

    private final AnalysisDataSource analysisDataSource;

    // Prevent direct instantiation.
    private AnalysisRepository(@NonNull SpikeRecorderDatabase db) {
        analysisDataSource =
            AnalysisLocalDataSource.get(db.spikeAnalysisDao(), db.spikeDao(), db.trainDao(), new AppExecutors());
    }

    /**
     * Returns the single instance of this class, creating it if necessary.
     *
     * @param db Application database
     * @return Singleton instance of the {@link AnalysisRepository}
     */
    public static AnalysisRepository get(@NonNull SpikeRecorderDatabase db) {
        if (INSTANCE == null) {
            synchronized (AnalysisRepository.class) {
                if (INSTANCE == null) INSTANCE = new AnalysisRepository(db);
            }
        }
        return INSTANCE;
    }

    /**
     * Used to force {@link #get(SpikeRecorderDatabase)}  to create a new instance next time it's called.
     */
    @SuppressWarnings("unused") public static void destroy() {
        INSTANCE = null;
    }

    //=================================================
    //  SPIKE ANALYSIS
    //=================================================

    /**
     * Checks whether spike analysis for the file with specified {@code filePath} exists and whether it has spike
     * trains. The result and the train count is passed to specified {@code callback}.
     *
     * @param filePath Path to the file for which existence of the analysis is checked.
     * @param countNonEmptyTrains Whether non empty spike trains should counted in.
     * @param callback Callback that's invoked when check is preformed.
     */
    public void spikeAnalysisExists(@NonNull String filePath, boolean countNonEmptyTrains,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        analysisDataSource.spikeAnalysisExists(filePath, countNonEmptyTrains, callback);
    }

    /**
     * Saves spike analysis for the file with specified {@code filePath} and all associated spikes and spike trains.
     *
     * @param filePath Path to the file for which analysis is being saved.
     * @param spikesAnalysis {@link Spike} objects that make the analysis.
     */
    public void saveSpikeAnalysis(@NonNull String filePath, @NonNull Spike[] spikesAnalysis) {
        analysisDataSource.saveSpikeAnalysis(filePath, spikesAnalysis);
    }

    /**
     * Returns id of the {@link SpikeAnalysis} for audio file located at specified {@code filePath}.
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spike analysis id.
     */
    public long getSpikeAnalysisId(@NonNull String filePath) {
        return analysisDataSource.getSpikeAnalysisId(filePath);
    }

    /**
     * Updates spike analysis file path for the audio file located at specified {@code oldFilePath} with specified
     * {@code newFilePath}.
     *
     * @param oldFilePath Absolute path of the audio file for which we want to update the file path.
     * @param newFilePath Absolute path of the new audio file.
     */
    public void updateSpikeAnalysisFilePath(@NonNull String oldFilePath, @NonNull String newFilePath) {
        analysisDataSource.updateSpikeAnalysisFilePath(oldFilePath, newFilePath);
    }

    /**
     * Deletes spike analysis, all spike trains and spikes related to audio file located at specified {@code filePath}.
     */
    public void deleteSpikeAnalysis(@NonNull String filePath) {
        analysisDataSource.deleteSpikeAnalysis(filePath);
    }

    /**
     * Returns collection of {@link SpikeIndexValue} objects which contain values and indices for spikes belonging
     * to {@link SpikeAnalysis} with specified {@code analysisId} and are positioned between {@code startIndex} and
     * {@code endIndex}.
     *
     * @param analysisId Id of the spike analysis returned spike values and indices belong to.
     * @param channel Channel for which values and indices of spikes should be returned.
     * @param startIndex Start index from which values and indices of spikes should be returned.
     * @param endIndex End index till which values and indices of spikes should be returned.
     */
    @NonNull public SpikeIndexValue[] getSpikeAnalysisValuesAndIndicesForRange(long analysisId, int channel,
        int startIndex, int endIndex) {
        return analysisDataSource.getSpikeAnalysisForIndexRange(analysisId, channel, startIndex, endIndex);
    }

    /**
     * Returns collection of {@link SpikeIndexValue} objects which contain values and indices for spikes belonging
     * to {@link Train} with specified {@code trainId} and are positioned between {@code startIndex} and
     * {@code endIndex}.
     *
     * @param trainId Id of the train returned spike values and indexes belong to.
     * @param channel Channel that the {@link Train} with specified {@code trainId} belongs to.
     * @param startIndex Start index from which values and indexes of spikes should be returned.
     * @param endIndex End index till which values and indexes of spikes should be returned.
     */
    @NonNull public SpikeIndexValue[] getSpikesByTrainForRange(long trainId, int channel, int startIndex,
        int endIndex) {
        return analysisDataSource.getSpikeAnalysisByTrainForIndexRange(trainId, channel, startIndex, endIndex);
    }

    /**
     * Returns collection of spike times collections sorted by the spike analysis train they belong to. Result is
     * returned by invoking specified {@code callback} and passing it in.
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spike trains.
     * @param callback Callback that's invoked when spike train times are retrieved from the database.
     */
    public void getSpikeAnalysisTimesByTrains(@NonNull String filePath,
        @Nullable AnalysisDataSource.GetAnalysisCallback<float[][]> callback) {
        analysisDataSource.getSpikeAnalysisTimesByTrains(filePath, callback);
    }

    /**
     * Returns collection of spike indices collections sorted by the spike analysis train they belong to. Result is
     * returned by invoking specified {@code callback} and passing it in.
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spike trains.
     * @param callback Callback that's invoked when spike trains indices are retrieved from the database.
     */
    public void getSpikeAnalysisIndicesByTrains(String filePath,
        AnalysisDataSource.GetAnalysisCallback<int[][]> callback) {
        analysisDataSource.getSpikeAnalysisIndicesByTrains(filePath, callback);
    }

    //=================================================
    //  SPIKE TRAINS
    //=================================================

    /**
     * Returns collection of threshold ranges for audio file with specified {@code filePath} that filter collection of
     * spikes for one of existing analysis by invoking specified {@code callback} and passing in the results.
     *
     * @param filePath Absolute path of the audio file for which trains should be retrieved.
     * @param callback Callback that's invoked when trains are retrieved from database.
     */
    public void getSpikeAnalysisTrains(@NonNull String filePath,
        @Nullable AnalysisDataSource.GetAnalysisCallback<Train[]> callback) {
        analysisDataSource.getSpikeAnalysisTrains(filePath, callback);
    }

    /**
     * Returns collection of threshold ranges for audio file with specified {@code filePath} that filter collection of
     * spikes for one of existing analysis by invoking specified {@code callback} and passing in the results.
     *
     * @param filePath Absolute path of the audio file for which trains should be retrieved.
     * @param channel Channel for which trains should be retrieved.
     * @param callback Callback that's invoked when trains are retrieved from database.
     */
    public void getSpikeAnalysisTrainsByChannel(@NonNull String filePath, int channel,
        @Nullable AnalysisDataSource.GetAnalysisCallback<Train[]> callback) {
        analysisDataSource.getSpikeAnalysisTrainsByChannel(filePath, channel, callback);
    }

    /**
     * Adds new spike train for the spike analysis of file at specified {@code filePath}.
     *
     * @param filePath Absolute path of the audio file for which we want to add new spike train.
     * @param channelCount Number of channels we need to create the new spike train for.
     * @param callback Callback that's invoked when spike trains is added to database.
     */
    public void addSpikeAnalysisTrain(@NonNull String filePath, int channelCount,
        @Nullable AnalysisDataSource.AddSpikeAnalysisTrainCallback callback) {
        analysisDataSource.addSpikeAnalysisTrain(filePath, channelCount, callback);
    }

    /**
     * Updates spike train's threshold defined by specified {@code orientation} for the file at specified {@code
     * filePath} with specified {@code value}.
     *
     * @param filePath Absolute path of the audio file for which we want to update spike train.
     * @param channel Channel for which spikes belonging to the saved train should be saved.
     * @param order Order of the spike train that needs to be updated.
     * @param orientation {@link ThresholdOrientation} of the threshold that was updated.
     * @param value New threshold value.
     */
    public void saveSpikeAnalysisTrain(@NonNull String filePath, int channel, int order,
        @ThresholdOrientation int orientation, int value) {
        analysisDataSource.saveSpikeAnalysisTrain(filePath, channel, order, orientation, value);
    }

    /**
     * Removes spike train at specified {@code order} for the file at specified {@code filePath}. New number of spike
     * trains is returned to caller by invoking specified {@code callback} and passing it as parameter.
     *
     * @param filePath Absolute path of the audio file for which we want to remove spike train.
     * @param trainOrder Order of the spike train that needs to be removed.
     * @param callback Callback that's invoked when spike trains is removed from database.
     */
    public void removeSpikeAnalysisTrain(@NonNull String filePath, int trainOrder,
        @Nullable AnalysisDataSource.RemoveSpikeAnalysisTrainCallback callback) {
        analysisDataSource.removeSpikeAnalysisTrain(filePath, trainOrder, callback);
    }
}
