package com.backyardbrains.data.persistance;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.data.persistance.source.AnalysisLocalDataSource;
import com.backyardbrains.drawing.ThresholdOrientation;
import com.backyardbrains.utils.AppExecutors;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class AnalysisRepository {

    private static AnalysisRepository INSTANCE = null;

    private final AnalysisDataSource analysisDataSource;

    // Prevent direct instantiation.
    private AnalysisRepository(@NonNull SpikeRecorderDatabase db) {
        analysisDataSource =
            AnalysisLocalDataSource.get(db.spikeAnalysisDao(), db.spikeDao(), db.trainDao(), db.spikeTrainDao(),
                new AppExecutors());
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
     * Checks whether spike analysis for the file with specified {@code filePath} exists. The result is passed to
     * specified {@code callback}.
     *
     * @param filePath Path to the file for which existence of the analysis is checked.
     * @param callback Callback that's invoked when check is preformed.
     */
    public void spikeAnalysisExists(@NonNull String filePath,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        analysisDataSource.spikeAnalysisExists(filePath, callback);
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
     * Returns collection of spikes for audio file located at specified {@code filePath} by invoking specified {@code
     * callback} and passing in the results.
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spikes.
     * @param callback Callback that's invoked when spikes are retrieved from the database.
     */
    public void getSpikeAnalysis(@NonNull String filePath,
        @Nullable AnalysisDataSource.GetAnalysisCallback<Spike[]> callback) {
        analysisDataSource.getSpikeAnalysis(filePath, callback);
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
     * Returns collection of threshold ranges for spike analysis with specified {@code spikeAnalysisId} that filter
     * collection of spikes for one of existing analysis by invoking specified {@code callback} and passing in the
     * results.
     *
     * @param filePath Absolute path of the audio file for which trains should be retrieved.
     * @param callback Callback that's invoked when trains are retrieved from database.
     */
    public void getSpikeAnalysisTrains(@NonNull String filePath,
        @Nullable AnalysisDataSource.GetAnalysisCallback<Train[]> callback) {
        analysisDataSource.getSpikeAnalysisTrains(filePath, callback);
    }

    /**
     * Adds new spike train for the spike analysis of file at specified {@code filePath}.
     *
     * @param filePath Absolute path of the audio file for which we want to add new spike train.
     * @param callback Callback that's invoked when spike trains is added to database.
     */
    public void addSpikeAnalysisTrain(@NonNull String filePath,
        @Nullable AnalysisDataSource.AddSpikeAnalysisTrainCallback callback) {
        analysisDataSource.addSpikeAnalysisTrain(filePath, callback);
    }

    /**
     * Updates spike train's threshold defined by specified {@code orientation} for the file at specified {@code
     * filePath} with specified {@code value}.
     *
     * @param filePath Absolute path of the audio file for which we want to update spike train.
     * @param orientation {@link ThresholdOrientation} of the threshold that was updated.
     * @param value New threshold value.
     * @param order Order of the spike train that needs to be updated.
     */
    public void saveSpikeAnalysisTrain(@NonNull String filePath, @ThresholdOrientation int orientation, int value,
        int order) {
        analysisDataSource.saveSpikeAnalysisTrain(filePath, orientation, value, order);
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
