package com.backyardbrains.db.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.db.dao.SpikeAnalysisDao;
import com.backyardbrains.db.dao.SpikeDao;
import com.backyardbrains.db.dao.TrainDao;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.db.entity.SpikeAnalysis;
import com.backyardbrains.db.entity.Train;
import com.backyardbrains.utils.AppExecutors;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.vo.SpikeIndexValue;
import com.backyardbrains.vo.SpikeIndexValueTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AnalysisLocalDataSource implements AnalysisDataSource {

    private static AnalysisLocalDataSource INSTANCE;

    @SuppressWarnings("WeakerAccess") SpikeAnalysisDao spikeAnalysisDao;
    @SuppressWarnings("WeakerAccess") SpikeDao spikeDao;
    @SuppressWarnings("WeakerAccess") TrainDao trainDao;
    @SuppressWarnings("WeakerAccess") AppExecutors appExecutors;

    // Private constructor through which we create singleton instance
    private AnalysisLocalDataSource(@NonNull SpikeAnalysisDao spikeAnalysisDao, @NonNull SpikeDao spikeDao,
        @NonNull TrainDao trainDao, @NonNull AppExecutors appExecutors) {
        this.spikeAnalysisDao = spikeAnalysisDao;
        this.spikeDao = spikeDao;
        this.trainDao = trainDao;
        this.appExecutors = appExecutors;
    }

    /**
     * Returns singleton instance of {@link AnalysisLocalDataSource} with default configuration.
     */
    public static AnalysisLocalDataSource get(@NonNull SpikeAnalysisDao spikeAnalysisDao, @NonNull SpikeDao spikeDao,
        @NonNull TrainDao trainDao, @NonNull AppExecutors appExecutors) {
        if (INSTANCE == null) {
            synchronized (AnalysisLocalDataSource.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AnalysisLocalDataSource(spikeAnalysisDao, spikeDao, trainDao, appExecutors);
                }
            }
        }
        return INSTANCE;
    }

    //=================================================
    //  SPIKE ANALYSIS
    //=================================================

    /**
     * {@inheritDoc}
     *
     * @param filePath Path to the file for which existence of the analysis is checked.
     * @param countNonEmptyTrains Whether non empty spike trains should counted in.
     * @param callback Callback that's invoked when check is preformed.
     */
    @Override public void spikeAnalysisExists(@NonNull final String filePath, boolean countNonEmptyTrains,
        @Nullable final SpikeAnalysisCheckCallback callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final int trainCount;
                if (countNonEmptyTrains) {
                    trainCount = trainDao.loadTrainCount(analysis.getId());
                } else {
                    trainCount = trainDao.loadNonEmptyTrainCount(analysis.getId());
                }
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onSpikeAnalysisExistsResult(analysis, trainCount);
                });
            } else {
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onSpikeAnalysisExistsResult(null, 0);
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Path to the file for which analysis is being saved.
     * @param spikesAnalysis {@link Spike} objects that make the analysis.
     */
    @Override public void saveSpikeAnalysis(@NonNull final String filePath, @NonNull final Spike[] spikesAnalysis) {
        final Runnable runnable = () -> {
            if (spikesAnalysis.length > 0) {
                // save spike analysis
                final SpikeAnalysis analysis = new SpikeAnalysis(filePath);
                final long analysisId = spikeAnalysisDao.insertSpikeAnalysis(analysis);

                if (analysisId > 0) {
                    // save spikes
                    for (Spike spike : spikesAnalysis) {
                        spike.setAnalysisId(analysisId);
                    }
                    spikeDao.insertSpikes(spikesAnalysis);
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param oldFilePath Path to the file for which we want to change the file path.
     * @param newFilePath New path that we want to save.
     */
    @Override public void updateSpikeAnalysisFilePath(@NonNull String oldFilePath, @NonNull String newFilePath) {
        final Runnable runnable = () -> {
            // get old spike analysis
            spikeAnalysisDao.updateSpikeAnalysisFilePath(oldFilePath, newFilePath);
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Path to the file for which analysis, all trains and spikes should be deleted.
     */
    @Override public void deleteSpikeAnalysis(String filePath) {
        final Runnable runnable = () -> spikeAnalysisDao.deleteSpikeAnalysis(filePath);

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spike analysis id.
     */
    @Override public long getSpikeAnalysisId(@NonNull String filePath) {
        final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
        return analysis != null ? analysis.getId() : 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param analysisId Id of the {@link SpikeAnalysis} for which spike values and indices should be queried.
     * @param channel Channel for which values and indices of spikes should be returned.
     * @param startIndex Index of the first sample in the range for which spikes should be retrieved.
     * @param endIndex Index of the last sample in the range for which spikes should be retrieved.
     * @return Array of spike values and indices located between specified {@code startIndex} and {@code endIndex}.
     */
    @Override public SpikeIndexValue[] getSpikeAnalysisForIndexRange(long analysisId, int channel, int startIndex,
        int endIndex) {
        return spikeDao.loadSpikesForIndexRange(analysisId, channel, startIndex, endIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @param trainId Id of the {@link Train} for which spike values and indices should be queried.
     * @param channel Channel that the {@link Train} with specified {@code trainId} belongs to.
     * @param startIndex Index of the first sample in the range for which spikes should be retrieved.
     * @param endIndex Index of the last sample in the range for which spikes should be retrieved.
     * @return Array of spike values and indices located between specified {@code startIndex} and {@code endIndex}.
     */
    @Override public SpikeIndexValue[] getSpikeAnalysisByTrainForIndexRange(long trainId, int channel, int startIndex,
        int endIndex) {
        return spikeDao.loadSpikesByTrainForIndexRange(trainId, channel, startIndex, endIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for spikes times should be retrieved.
     * @param callback Callback that's invoked when spike times are retrieved from the database.
     */
    @Override public void getSpikeAnalysisTimesByTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<float[][]> callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final Train[] trains = trainDao.loadTrains(analysis.getId());
                final int trainCount = trains.length;
                if (trainCount > 0) {
                    final float[][] spikeAnalysisTrains = new float[trainCount][];
                    for (int i = 0; i < trainCount; i++) {
                        spikeAnalysisTrains[i] = spikeDao.loadSpikeTimes(trains[i].getId());
                    }
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) {
                            callback.onAnalysisLoaded(spikeAnalysisTrains);
                        }
                    });
                } else {
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onDataNotAvailable();
                    });
                }
            } else {
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onDataNotAvailable();
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which spike indices should be retrieved.
     * @param callback Callback that's invoked when spike indices are retrieved from the database.
     */
    @Override public void getSpikeAnalysisIndicesByTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<int[][]> callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final Train[] trains = trainDao.loadTrains(analysis.getId());
                final int trainCount = trains.length;
                if (trainCount > 0) {
                    final int[][] spikeAnalysisTrains = new int[trainCount][];
                    for (int i = 0; i < trainCount; i++) {
                        spikeAnalysisTrains[i] = spikeDao.loadSpikeIndices(trains[i].getId());
                    }
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) {
                            callback.onAnalysisLoaded(spikeAnalysisTrains);
                        }
                    });
                } else {
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onDataNotAvailable();
                    });
                }
            } else {
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onDataNotAvailable();
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    //=================================================
    //  SPIKE TRAINS
    //=================================================

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which trains should be retrieved.
     * @param callback Callback that's invoked when trains are retrieved from database.
     */
    @Override public void getSpikeAnalysisTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<Train[]> callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final Train[] trains = trainDao.loadTrains(analysis.getId());
                if (trains.length > 0) {
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onAnalysisLoaded(trains);
                    });
                } else {
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onDataNotAvailable();
                    });
                }
            } else {
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onDataNotAvailable();
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which trains should be retrieved.
     * @param channel Channel for which trains should be retrieved.
     * @param callback Callback that's invoked when trains are retrieved from database.
     */
    @Override public void getSpikeAnalysisTrainsByChannel(@NonNull final String filePath, int channel,
        @Nullable final GetAnalysisCallback<Train[]> callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final Train[] trains = trainDao.loadTrainsByChannel(analysis.getId(), channel);
                if (trains.length > 0) {
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onAnalysisLoaded(trains);
                    });
                } else {
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onDataNotAvailable();
                    });
                }
            } else {
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onDataNotAvailable();
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to add new spike train.
     * @param channelCount Number of channels we need to create the new spike train for.
     * @param callback Callback that's invoked when spike trains is added to database.
     */
    @Override public void addSpikeAnalysisTrain(@NonNull final String filePath, int channelCount,
        @Nullable final AddSpikeAnalysisTrainCallback callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                int trainCount = trainDao.loadTrainCount(analysis.getId());
                if (trainCount > 0) trainCount /= channelCount;
                final Train[] trains = new Train[channelCount];
                for (int i = 0; i < channelCount; i++) {
                    trains[i] = new Train(analysis.getId(), i, trainCount);
                }
                trainDao.insertTrains(trains);
                int finalTrainCount = trainCount;
                appExecutors.mainThread().execute(() -> {
                    if (callback != null) callback.onSpikeAnalysisTrainAdded(finalTrainCount);
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to update spike train.
     * @param channel Channel for which spikes belonging to the saved train should be saved.
     * @param order Order of the spike train that needs to be updated.
     * @param orientation {@link ThresholdOrientation} of the threshold that was updated.
     * @param value New threshold value.
     */
    @Override public void saveSpikeAnalysisTrain(@NonNull final String filePath, int channel, final int order,
        @ThresholdOrientation final int orientation, final int value) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final long analysisId = analysis.getId();
                final Train train = trainDao.loadTrain(analysisId, channel, order);
                if (train != null) {
                    // find new lower/upper thresholds
                    final int left = orientation == ThresholdOrientation.LEFT ? value
                        : train.isLowerLeft() ? train.getLowerThreshold() : train.getUpperThreshold();
                    final int right = orientation == ThresholdOrientation.RIGHT ? value
                        : train.isLowerLeft() ? train.getUpperThreshold() : train.getLowerThreshold();
                    final int lower = Math.min(left, right);
                    final int upper = Math.max(left, right);
                    final long trainId = train.getId();

                    // remove old train and all linked spike trains (cascade delete)
                    trainDao.deleteTrain(train);

                    // remove all linked spikes
                    spikeDao.deleteSpikes(trainId);

                    // save newly configured train
                    trainDao.insertTrain(new Train(analysisId, channel, order, 0, lower, upper, left < right));

                    // save new entries to spike_trains table linked to this train
                    final SpikeIndexValueTime[] spikes =
                        spikeDao.loadSpikesForValueRange(analysisId, channel, lower, upper);
                    final Train savedTrain = trainDao.loadTrain(analysisId, channel, order);
                    final List<Spike> newSpikes = new ArrayList<>();
                    for (SpikeIndexValueTime spike : spikes) {
                        newSpikes.add(
                            new Spike(savedTrain.getAnalysisId(), savedTrain.getId(), channel, spike.getValue(),
                                spike.getIndex(), spike.getTime()));
                    }
                    final int len = newSpikes.size();
                    if (len > 0) {
                        spikeDao.insertSpikes(newSpikes);
                        trainDao.updateTrainSpikeCount(savedTrain.getId(), len);
                    }
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to remove spike train.
     * @param order Order of the spike train that needs to be removed.
     * @param callback Callback that's invoked when spike trains is removed from database.
     */
    @Override public void removeSpikeAnalysisTrain(@NonNull final String filePath, final int order,
        @Nullable final RemoveSpikeAnalysisTrainCallback callback) {
        final Runnable runnable = () -> {
            final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
            if (analysis != null) {
                final long analysisId = analysis.getId();
                final Train[] trains = trainDao.loadTrains(analysisId, order);
                /*final Train train = trainDao.loadTrain(analysisId, , order); */
                if (trains != null && trains.length > 0) {
                    for (Train train : trains) {
                        final long trainId = train.getId();

                        // remove train
                        trainDao.deleteTrain(train);

                        // remove all linked spikes
                        spikeDao.deleteSpikes(trainId);
                    }

                    // update order of all trains after deleted one
                    trainDao.updateTrainsAfterOrder(analysisId, order);

                    // get fresh train count
                    int trainCount = trainDao.loadTrainCount(analysisId);
                    final int finalTrainCount = trainCount > 0 ? trainCount / trains.length : trainCount;
                    appExecutors.mainThread().execute(() -> {
                        if (callback != null) callback.onSpikeAnalysisTrainRemoved(finalTrainCount);
                    });
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }
}
