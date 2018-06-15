package com.backyardbrains.data.persistance.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.SpikeValueAndIndex;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.dao.SpikeAnalysisDao;
import com.backyardbrains.data.persistance.dao.SpikeDao;
import com.backyardbrains.data.persistance.dao.SpikeTrainDao;
import com.backyardbrains.data.persistance.dao.TrainDao;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.data.persistance.entity.SpikeAnalysis;
import com.backyardbrains.data.persistance.entity.SpikeTrain;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.utils.AppExecutors;
import com.backyardbrains.utils.ThresholdOrientation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class AnalysisLocalDataSource implements AnalysisDataSource {

    private static AnalysisLocalDataSource INSTANCE;

    @SuppressWarnings("WeakerAccess") SpikeAnalysisDao spikeAnalysisDao;
    @SuppressWarnings("WeakerAccess") SpikeDao spikeDao;
    @SuppressWarnings("WeakerAccess") TrainDao trainDao;
    @SuppressWarnings("WeakerAccess") SpikeTrainDao spikeTrainDao;
    @SuppressWarnings("WeakerAccess") AppExecutors appExecutors;

    // Private constructor through which we create singleton instance
    private AnalysisLocalDataSource(@NonNull SpikeAnalysisDao spikeAnalysisDao, @NonNull SpikeDao spikeDao,
        @NonNull TrainDao trainDao, @NonNull SpikeTrainDao spikeTrainDao, @NonNull AppExecutors appExecutors) {
        this.spikeAnalysisDao = spikeAnalysisDao;
        this.spikeDao = spikeDao;
        this.trainDao = trainDao;
        this.spikeTrainDao = spikeTrainDao;
        this.appExecutors = appExecutors;
    }

    /**
     * Returns singleton instance of {@link AnalysisLocalDataSource} with default configuration.
     */
    public static AnalysisLocalDataSource get(@NonNull SpikeAnalysisDao spikeAnalysisDao, @NonNull SpikeDao spikeDao,
        @NonNull TrainDao trainDao, @NonNull SpikeTrainDao spikeTrainDao, @NonNull AppExecutors appExecutors) {
        if (INSTANCE == null) {
            synchronized (AnalysisLocalDataSource.class) {
                if (INSTANCE == null) {
                    INSTANCE =
                        new AnalysisLocalDataSource(spikeAnalysisDao, spikeDao, trainDao, spikeTrainDao, appExecutors);
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
     * @param callback Callback that's invoked when check is preformed.
     */
    @Override public void spikeAnalysisExists(@NonNull final String filePath,
        @Nullable final SpikeAnalysisCheckCallback callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final int trainCount = trainDao.loadTrainCount(analysis.getId());
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onSpikeAnalysisExistsResult(trainCount > 0, trainCount);
                        }
                    });
                } else {
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onSpikeAnalysisExistsResult(false, 0);
                        }
                    });
                }
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
        final Runnable runnable = new Runnable() {
            @Override public void run() {
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
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spike analysis id.
     */
    public long getSpikeAnalysisId(@NonNull String filePath) {
        return spikeAnalysisDao.loadSpikeAnalysisId(filePath);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve the spikes.
     * @param callback Callback that's invoked when spikes are retrieved from the database.
     */
    @Override public void getSpikeAnalysisSpikes(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<Spike[]> callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final Spike[] spikes = spikeDao.loadSpikes(analysis.getId());
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onAnalysisLoaded(spikes);
                        }
                    });
                } else {
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onDataNotAvailable();
                        }
                    });
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param analysisId Id of the {@link SpikeAnalysis} for which spike values and indices should be queried.
     * @param startIndex Index of the first sample in the range for which spikes should be retrieved.
     * @param endIndex Index of the last sample in the range for which spikes should be retrieved.
     * @return Array of spike values and indices located between specified {@code startIndex} and {@code endIndex}.
     */
    public SpikeValueAndIndex[] getSpikeAnalysisValuesAndIndicesForRange(long analysisId, int startIndex,
        int endIndex) {
        return spikeDao.loadSpikeValuesAndIndicesForRange(analysisId, startIndex, endIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @param trainId Id of the {@link Train} for which spike values and indices should be queried.
     * @param startIndex Index of the first sample in the range for which spikes should be retrieved.
     * @param endIndex Index of the last sample in the range for which spikes should be retrieved.
     * @return Array of spike values and indices located between specified {@code startIndex} and {@code endIndex}.
     */
    @Override public SpikeValueAndIndex[] getSpikeAnalysisValuesAndIndicesByTrainForRange(long trainId, int startIndex,
        int endIndex) {
        return spikeTrainDao.loadSpikeValuesAndIndicesForRange(trainId, startIndex, endIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve spike times.
     * @param callback Callback that's invoked when spike times are retrieved from the database.
     */
    @Override public void getSpikeAnalysisTimesByTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<float[][]> callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final Train[] trains = trainDao.loadTrains(analysis.getId());
                    if (trains.length > 0) {
                        final float[][] spikeAnalysisTrains = new float[trains.length][];
                        boolean analysisExists = false;
                        for (int i = 0; i < trains.length; i++) {
                            spikeAnalysisTrains[i] = spikeTrainDao.loadSpikeTimes(trains[i].getId());
                            if (spikeAnalysisTrains[i].length > 0) analysisExists = true;
                        }

                        final boolean finalAnalysisExists = analysisExists;
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override public void run() {
                                if (callback != null) {
                                    if (finalAnalysisExists) {
                                        callback.onAnalysisLoaded(spikeAnalysisTrains);
                                    } else {
                                        callback.onDataNotAvailable();
                                    }
                                }
                            }
                        });
                    } else {
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override public void run() {
                                if (callback != null) callback.onDataNotAvailable();
                            }
                        });
                    }
                } else {
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onDataNotAvailable();
                        }
                    });
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to retrieve spike indices.
     * @param callback Callback that's invoked when spike indices are retrieved from the database.
     */
    @Override public void getSpikeAnalysisIndicesByTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<int[][]> callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final Train[] trains = trainDao.loadTrains(analysis.getId());
                    if (trains.length > 0) {
                        final int[][] spikeAnalysisTrains = new int[trains.length][];
                        boolean analysisExists = false;
                        for (int i = 0; i < trains.length; i++) {
                            spikeAnalysisTrains[i] = spikeTrainDao.loadSpikeIndices(trains[i].getId());
                            if (spikeAnalysisTrains[i].length > 0) analysisExists = true;
                        }

                        final boolean finalAnalysisExists = analysisExists;
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override public void run() {
                                if (callback != null) {
                                    if (finalAnalysisExists) {
                                        callback.onAnalysisLoaded(spikeAnalysisTrains);
                                    } else {
                                        callback.onDataNotAvailable();
                                    }
                                }
                            }
                        });
                    } else {
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override public void run() {
                                if (callback != null) callback.onDataNotAvailable();
                            }
                        });
                    }
                } else {
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onDataNotAvailable();
                        }
                    });
                }
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
        final GetAnalysisCallback<Train[]> callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final Train[] trains = trainDao.loadTrains(analysis.getId());
                    if (trains.length > 0) {
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override public void run() {
                                if (callback != null) callback.onAnalysisLoaded(trains);
                            }
                        });
                    } else {
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override public void run() {
                                if (callback != null) callback.onDataNotAvailable();
                            }
                        });
                    }
                } else {
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onDataNotAvailable();
                        }
                    });
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to add new spike train.
     * @param callback Callback that's invoked when spike trains is added to database.
     */
    public void addSpikeAnalysisTrain(@NonNull final String filePath,
        @Nullable final AddSpikeAnalysisTrainCallback callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final int trainCount = trainDao.loadTrainCount(analysis.getId());
                    final Train train = new Train(analysis.getId(), 0, 0, trainCount, true);
                    trainDao.insertTrain(train);
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onSpikeAnalysisTrainAdded(train);
                        }
                    });
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    /**
     * {@inheritDoc}
     *
     * @param filePath Absolute path of the audio file for which we want to update spike train.
     * @param orientation {@link ThresholdOrientation} of the threshold that was updated.
     * @param value New threshold value.
     * @param order Order of the spike train that needs to be updated.
     */
    @Override public void saveSpikeAnalysisTrain(@NonNull final String filePath,
        @ThresholdOrientation final int orientation, final int value, final int order) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    final Train train = trainDao.loadTrain(analysis.getId(), order);
                    if (train != null) {
                        // find new lower/upper thresholds
                        final int left = orientation == ThresholdOrientation.LEFT ? value
                            : train.isLowerLeft() ? train.getLowerThreshold() : train.getUpperThreshold();
                        final int right = orientation == ThresholdOrientation.RIGHT ? value
                            : train.isLowerLeft() ? train.getUpperThreshold() : train.getLowerThreshold();
                        final int lower = Math.min(left, right);
                        final int upper = Math.max(left, right);

                        // remove old train and all linked spike trains (cascade delete)
                        trainDao.deleteTrain(train);

                        // save newly configured train
                        trainDao.insertTrain(new Train(analysis.getId(), lower, upper, order, left < right));

                        // save new entries to spike_trains table lined to this train
                        final Spike[] spikes = spikeDao.loadSpikes(analysis.getId());
                        final Train savedTrain = trainDao.loadTrain(analysis.getId(), order);
                        final List<SpikeTrain> spikeTrains = new ArrayList<>();
                        for (Spike spike : spikes) {
                            if (spike.getValue() >= lower && spike.getValue() <= upper) {
                                spikeTrains.add(new SpikeTrain(spike.getId(), savedTrain.getId()));
                            }
                        }
                        if (spikeTrains.size() > 0) spikeTrainDao.insertSpikeTrains(spikeTrains);
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
     * @param trainOrder Order of the spike train that needs to be removed.
     * @param callback Callback that's invoked when spike trains is removed from database.
     */
    @Override public void removeSpikeAnalysisTrain(@NonNull final String filePath, final int trainOrder,
        @Nullable final RemoveSpikeAnalysisTrainCallback callback) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                final SpikeAnalysis analysis = spikeAnalysisDao.loadSpikeAnalysis(filePath);
                if (analysis != null) {
                    // remove train and all linked spike trains (cascade delete)
                    trainDao.deleteTrain(analysis.getId(), trainOrder);

                    // update order of all trains after deleted one
                    trainDao.updateTrainsAfterOrder(analysis.getId(), trainOrder);

                    // get fresh train count
                    final int trainCount = trainDao.loadTrainCount(analysis.getId());
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onSpikeAnalysisTrainRemoved(trainCount);
                        }
                    });
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }
}
