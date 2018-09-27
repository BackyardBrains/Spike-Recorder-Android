package com.backyardbrains.analysis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.AudioFile;
import com.backyardbrains.audio.WavAudioFile;
import com.backyardbrains.data.AverageSpike;
import com.backyardbrains.data.SpikeValueAndIndex;
import com.backyardbrains.data.Threshold;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.AnalysisRepository;
import com.backyardbrains.data.persistance.SpikeRecorderDatabase;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.events.AnalysisDoneEvent;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.ThresholdOrientation;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AnalysisManager {

    private static final String TAG = makeLogTag(AnalysisManager.class);

    private AudioFile audioFile;

    // Reference to the data manager that stores and processes the data
    @SuppressWarnings("WeakerAccess") final AnalysisRepository analysisRepository;

    @SuppressWarnings("WeakerAccess") int[][] autocorrelation;
    @SuppressWarnings("WeakerAccess") int[][] crossCorrelation;
    @SuppressWarnings("WeakerAccess") int[][] isi;
    @SuppressWarnings("WeakerAccess") AverageSpike[] averageSpikes;

    public AnalysisManager(@NonNull Context context) {
        analysisRepository = AnalysisRepository.get(SpikeRecorderDatabase.get(context));
    }

    //=================================================
    //  FIND SPIKES
    //=================================================

    // Callback to be invoked when spikes are retrieved from the analysis repository
    private AnalysisDataSource.GetAnalysisCallback<Spike[]> getSpikesCallback =
        new AnalysisDataSource.GetAnalysisCallback<Spike[]>() {
            @Override public void onAnalysisLoaded(@NonNull Spike[] spikes) {
                // post event that audio file analysis was successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.FIND_SPIKES));
            }

            @Override public void onDataNotAvailable() {
                findSpikes();
            }
        };

    /**
     * Loads file with specified {@code filePath} if not already loaded and starts the process of finding spikes if they
     * are not already found.
     */
    public void findSpikes(@NonNull String filePath) {
        if (audioFile != null) {
            if (!ObjectUtils.equals(filePath, audioFile.getAbsolutePath())) {
                if (load(filePath)) {
                    findSpikes();
                } else {
                    // TODO: 09-Feb-18 BROADCAST EVENT THAT LOADING OF THE FILE FAILED
                }
            } else {
                analysisRepository.getSpikeAnalysisSpikes(filePath, getSpikesCallback);
            }
        } else {
            if (load(filePath)) {
                findSpikes();
            } else {
                // TODO: 09-Feb-18 BROADCAST EVENT THAT LOADING OF THE FILE FAILED
            }
        }
    }

    /**
     * Returns spike analysis id for audio file located at specified {@code filePath}.
     */
    public long getSpikeAnalysisId(@NonNull String filePath) {
        return analysisRepository.getSpikeAnalysisId(filePath);
    }

    /**
     * Returns array of spike values and indexes belonging to spike analysis with specified {@code analysisId} for the specified range.
     */
    public SpikeValueAndIndex[] getSpikesForRange(long analysisId, int startIndex, int endIndex) {
        return analysisRepository.getSpikeAnalysisValuesAndIndicesForRange(analysisId, startIndex, endIndex);
    }

    /**
     * Returns array of spike values and indexes belonging to train with specified {@code trainId} for the specified range.
     */
    public SpikeValueAndIndex[] getSpikesByTrainForRange(long trainId, int startIndex, int endIndex) {
        return analysisRepository.getSpikesByTrainForRange(trainId, startIndex, endIndex);
    }

    /**
     * Whether process of analysing spikes is finished or not.
     */
    public void spikesAnalysisExists(@NonNull String filePath,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        analysisRepository.spikeAnalysisExists(filePath, callback);
    }

    // Loads file with specified file path into WavAudioFile for further processing
    private boolean load(@NonNull String filePath) {
        try {
            return load(new File(filePath));
        } catch (IOException e) {
            LOGE(TAG, "Error while loading " + filePath);
            Crashlytics.logException(e);
            return false;
        }
    }

    // Loads specified file into WavAudioFile for further processing
    private boolean load(@NonNull File file) throws IOException {
        LOGD(TAG, "load");

        if (!file.exists()) {
            LOGE(TAG, "Cant load file " + file.getAbsolutePath() + ", it doesn't exist!!");
            return false;
        }

        reset();
        audioFile = new WavAudioFile(file);

        return true;
    }

    // Clears current spike analysis and triggers the new one
    @SuppressWarnings("WeakerAccess") void findSpikes() {
        new FindSpikesAnalysis(audioFile, new BaseAnalysis.AnalysisListener<Spike>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable Spike[] results) {
                analysisRepository.saveSpikeAnalysis(filePath, results != null ? results : new Spike[0]);

                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.FIND_SPIKES));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.FIND_SPIKES));
            }
        }).startAnalysis();
    }

    // Resets all the flags and clears all resources before loading new audio file.
    private void reset() {
        LOGD(TAG, "RESET");
        if (audioFile != null) {
            try {
                audioFile.close();
                LOGD(TAG, "RandomAccessFile closed");
            } catch (IOException e) {
                LOGE(TAG, "IOException while stopping random access file: " + e.toString());
                Crashlytics.logException(e);
            } finally {
                audioFile = null;
            }
        }

        autocorrelation = null;
        isi = null;
        crossCorrelation = null;
        averageSpikes = null;
    }

    //=================================================
    //  SPIKE TRAINS
    //=================================================

    /**
     *
     * @param filePath
     * @param callback
     */
    public void getSpikeTrains(@NonNull String filePath,
        @Nullable AnalysisDataSource.GetAnalysisCallback<Train[]> callback) {
        analysisRepository.getSpikeAnalysisTrains(filePath, callback);
    }

    // Callback to be invoked when spike analysis (by trains) is retrieved from the analysis repository
    private class GetSpikeAnalysisByTrainsCallback implements AnalysisDataSource.GetAnalysisCallback<float[][]> {

        private final String filePath;
        private final @AnalysisType int analysisType;

        GetSpikeAnalysisByTrainsCallback(@NonNull String filePath, @AnalysisType int analysisType) {
            this.filePath = filePath;
            this.analysisType = analysisType;
        }

        @SuppressLint("SwitchIntDef") @Override public void onAnalysisLoaded(@NonNull float[][] result) {
            switch (analysisType) {
                case AnalysisType.AUTOCORRELATION:
                    autocorrelationAnalysis(filePath, result);
                    break;
                case AnalysisType.ISI:
                    isiAnalysis(filePath, result);
                    break;
                case AnalysisType.CROSS_CORRELATION:
                    crossCorrelationAnalysis(filePath, result);
                    break;
            }
        }

        @Override public void onDataNotAvailable() {
            // post event that audio file analysis failed
            EventBus.getDefault().post(new AnalysisDoneEvent(true, analysisType));
        }
    }

    @SuppressWarnings("WeakerAccess") void getSpikeAnalysisByTrains(@NonNull String filePath,
        @AnalysisType int analysisType) {
        if (analysisType == AnalysisType.AVERAGE_SPIKE) {
            analysisRepository.getSpikeAnalysisIndicesByTrains(filePath,
                new AnalysisDataSource.GetAnalysisCallback<int[][]>() {
                    @Override public void onAnalysisLoaded(@NonNull int[][] result) {
                        averageSpikeAnalysis(result);
                    }

                    @Override public void onDataNotAvailable() {
                        // post event that audio file analysis failed
                        EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.AVERAGE_SPIKE));
                    }
                });
        } else {
            analysisRepository.getSpikeAnalysisTimesByTrains(filePath,
                new GetSpikeAnalysisByTrainsCallback(filePath, analysisType));
        }
    }

    //=================================================
    //  ANALYZE FILE
    //=================================================

    /**
     * Initiates a check of whether specified analysis {@code type} already exists for the file at specified {@code
     * filePath}. The check will either start the analysis process if it doesn't exist, or inform caller that it does.
     */
    @SuppressLint("SwitchIntDef") public void startAnalysis(@NonNull final String filePath, @AnalysisType int type) {
        switch (type) {
            case AnalysisType.AUTOCORRELATION:
                getSpikeAnalysisByTrains(filePath, AnalysisType.AUTOCORRELATION);
                break;
            case AnalysisType.ISI:
                getSpikeAnalysisByTrains(filePath, AnalysisType.ISI);
                break;
            case AnalysisType.CROSS_CORRELATION:
                getSpikeAnalysisByTrains(filePath, AnalysisType.CROSS_CORRELATION);
                break;
            case AnalysisType.AVERAGE_SPIKE:
                averageSpikeAnalysis(filePath);
                break;
        }
    }

    //=================================================
    //  AUTOCORRELATION
    //=================================================

    /**
     * Returns results for the Autocorrelation analysis
     */
    @Nullable public int[][] getAutocorrelation() {
        return autocorrelation;
    }

    // Starts Autocorrelation analysis depending on the set flags.
    @SuppressWarnings("WeakerAccess") void autocorrelationAnalysis(final @NonNull String filePath,
        @NonNull float[][] spikeAnalysisByTrains) {
        LOGD(TAG, "autocorrelationAnalysis()");
        new AutocorrelationAnalysis(filePath, spikeAnalysisByTrains, new BaseAnalysis.AnalysisListener<int[]>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable int[][] results) {
                autocorrelation = results;
                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.AUTOCORRELATION));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.AUTOCORRELATION));
            }
        }).startAnalysis();
    }

    //=================================================
    //  ISI (Inter Spike Interval)
    //=================================================

    /**
     * Returns results for the Inter Spike Interval analysis
     */
    @Nullable public int[][] getISI() {
        return isi;
    }

    // Starts Inter Spike Interval analysis depending on the set flags.
    @SuppressWarnings("WeakerAccess") void isiAnalysis(final @NonNull String filePath,
        @NonNull float[][] spikeAnalysisByTrains) {
        LOGD(TAG, "isiAnalysis()");
        new IsiAnalysis(filePath, spikeAnalysisByTrains, new BaseAnalysis.AnalysisListener<int[]>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable int[][] results) {
                isi = results;
                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.ISI));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.ISI));
            }
        }).startAnalysis();
    }

    //=================================================
    //  CROSS-CORRELATION
    //=================================================

    /**
     * Returns results for the Cross-Correlation analysis
     */
    @Nullable public int[][] getCrossCorrelation() {
        return crossCorrelation;
    }

    // Starts Cross-Correlation analysis depending on the set flags.
    @SuppressWarnings("WeakerAccess") void crossCorrelationAnalysis(final @NonNull String filePath,
        @NonNull float[][] spikeAnalysisByTrains) {
        LOGD(TAG, "crossCorrelationAnalysis()");
        new CrossCorrelationAnalysis(filePath, spikeAnalysisByTrains, new BaseAnalysis.AnalysisListener<int[]>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable int[][] results) {
                crossCorrelation = results;
                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.CROSS_CORRELATION));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.CROSS_CORRELATION));
            }
        }).startAnalysis();
    }

    //=================================================
    //  AVERAGE SPIKE
    //=================================================

    /**
     * Returns results for the Average Spike analysis
     */
    @Nullable public AverageSpike[] getAverageSpike() {
        return averageSpikes;
    }

    // Loads file with specified filePath if not already loaded and starts Average Spike analysis.
    private void averageSpikeAnalysis(@NonNull String filePath) {
        if (audioFile != null) {
            if (!ObjectUtils.equals(filePath, audioFile.getAbsolutePath())) {
                if (load(filePath)) {
                    getSpikeAnalysisByTrains(filePath, AnalysisType.AVERAGE_SPIKE);
                } else {
                    // TODO: 09-Feb-18 BROADCAST EVENT THAT LOADING OF THE FILE FAILED
                }
            } else {
                getSpikeAnalysisByTrains(filePath, AnalysisType.AVERAGE_SPIKE);
            }
        } else {
            if (load(filePath)) {
                getSpikeAnalysisByTrains(filePath, AnalysisType.AVERAGE_SPIKE);
            } else {
                // TODO: 09-Feb-18 BROADCAST EVENT THAT LOADING OF THE FILE FAILED
            }
        }
    }

    // Starts the actual Average Spike analysis.
    @SuppressWarnings("WeakerAccess") void averageSpikeAnalysis(@NonNull int[][] spikeAnalysisByTrains) {
        LOGD(TAG, "averageSpikeAnalysis()");
        if (audioFile != null) {
            new AverageSpikeAnalysis(audioFile, spikeAnalysisByTrains,
                new BaseAnalysis.AnalysisListener<AverageSpike>() {
                    @Override public void onAnalysisDone(@NonNull String filePath, @Nullable AverageSpike[] results) {
                        averageSpikes = results;
                        // post event that audio file analysis is successfully finished
                        EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.AVERAGE_SPIKE));
                    }

                    @Override public void onAnalysisFailed(@NonNull String filePath) {
                        // post event that audio file analysis is successfully finished
                        EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.AVERAGE_SPIKE));
                    }
                }).startAnalysis();
        }
    }

    //=================================================
    //  THRESHOLDS
    //=================================================

    /**
     * Listens for DB query response when retrieving existing spike trains.
     */
    public interface GetThresholdsCallback {
        /**
         * Triggered when spike trains are retrieved from database.
         */
        void onThresholdsLoaded(@NonNull List<Threshold> thresholds);
    }

    /**
     * Returns existing thresholds for the audio file with specified {@code filePath}.
     */
    public void getThresholds(@NonNull String filePath, @Nullable final GetThresholdsCallback callback) {
        analysisRepository.getSpikeAnalysisTrains(filePath, new AnalysisDataSource.GetAnalysisCallback<Train[]>() {
            @Override public void onAnalysisLoaded(@NonNull Train[] result) {
                final List<Threshold> thresholds = new ArrayList<>();
                for (Train train : result) {
                    int leftThreshold = train.isLowerLeft() ? train.getLowerThreshold() : train.getUpperThreshold();
                    int rightThreshold = train.isLowerLeft() ? train.getUpperThreshold() : train.getLowerThreshold();
                    thresholds.add(new Threshold(leftThreshold, rightThreshold));
                }
                if (callback != null) callback.onThresholdsLoaded(thresholds);
            }

            @Override public void onDataNotAvailable() {
                if (callback != null) callback.onThresholdsLoaded(new ArrayList<Threshold>());
            }
        });
    }

    /**
     * Adds new pair of thresholds for the audio file with specified {@code filePath}.
     */
    public void addThreshold(@NonNull String filePath,
        @Nullable final AnalysisDataSource.AddSpikeAnalysisTrainCallback callback) {
        analysisRepository.addSpikeAnalysisTrain(filePath, new AnalysisDataSource.AddSpikeAnalysisTrainCallback() {
            @Override public void onSpikeAnalysisTrainAdded(@NonNull Train train) {
                if (callback != null) callback.onSpikeAnalysisTrainAdded(train);
            }
        });
    }

    /**
     * Removes thresholds at the specified {@code index} for the audio file with specified {@code filePath}.
     */
    public void removeThreshold(int index, @NonNull String filePath,
        @Nullable final AnalysisDataSource.RemoveSpikeAnalysisTrainCallback callback) {
        analysisRepository.removeSpikeAnalysisTrain(filePath, index,
            new AnalysisDataSource.RemoveSpikeAnalysisTrainCallback() {
                @Override public void onSpikeAnalysisTrainRemoved(int newTrainCount) {
                    if (callback != null) callback.onSpikeAnalysisTrainRemoved(newTrainCount);
                }
            });
    }

    /**
     * Sets specified {@code value} for the current threshold at the specified {@code index} with specified {@code
     * orientation}.
     */
    public void setThreshold(int index, @ThresholdOrientation int orientation, int value, @NonNull String filePath) {
        analysisRepository.saveSpikeAnalysisTrain(filePath, orientation, value, index);
    }
}
