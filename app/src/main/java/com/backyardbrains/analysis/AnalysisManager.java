package com.backyardbrains.analysis;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.db.AnalysisRepository;
import com.backyardbrains.db.SpikeRecorderDatabase;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.db.entity.Train;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.dsp.audio.WavAudioFile;
import com.backyardbrains.events.AnalysisDoneEvent;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.vo.AverageSpike;
import com.backyardbrains.vo.EventTriggeredAverages;
import com.backyardbrains.vo.SpikeIndexValue;
import com.backyardbrains.vo.Threshold;
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

    @SuppressWarnings("WeakerAccess") EventTriggeredAverages[] eventTriggeredAverages;
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
    private AnalysisDataSource.SpikeAnalysisCheckCallback spikeAnalysisCheckCallback = (analysis, trainCount) -> {
        // post event that audio file analysis was successfully finished
        EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.FIND_SPIKES));
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
                    // FIXME: 10-Oct-18 FOR NOW JUST BROADCAST THAT ANALYSIS FAILED BUT IN THE FUTURE MORE SPECIFIC ERROR SHOULD BE BROADCAST
                    // post event that audio file analysis failed
                    EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.FIND_SPIKES));

                    Crashlytics.logException(new Throwable("Error while loading file during Find Spikes analysis"));
                }
            } else {
                spikesAnalysisExists(filePath, false, spikeAnalysisCheckCallback);
            }
        } else {
            if (load(filePath)) {
                findSpikes();
            } else {
                // FIXME: 10-Oct-18 FOR NOW JUST BROADCAST THAT ANALYSIS FAILED BUT IN THE FUTURE MORE SPECIFIC ERROR SHOULD BE BROADCAST
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.FIND_SPIKES));

                Crashlytics.logException(new Throwable("Error while loading file during Find Spikes analysis"));
            }
        }
    }

    /**
     * Whether process of analysing spikes is finished or not for the audio file located at specified {@code filePath}.
     */
    public void spikesAnalysisExists(@NonNull String filePath, boolean countNonEmptyTrains,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        analysisRepository.spikeAnalysisExists(filePath, countNonEmptyTrains, callback);
    }

    /**
     * Returns spike analysis id for audio file located at specified {@code filePath}.
     */
    public long getSpikeAnalysisId(@NonNull String filePath) {
        return analysisRepository.getSpikeAnalysisId(filePath);
    }

    /**
     * Updates spike analysis file path with specified {@code newFilePath} for audio file located at specified {@code
     * oldFilePath}.
     */
    public void updateSpikeAnalysisFilePath(@NonNull String oldFilePath, @NonNull String newFilePath) {
        analysisRepository.updateSpikeAnalysisFilePath(oldFilePath, newFilePath);
    }

    /**
     * Deletes spike analysis, all trains for that spike analysis and all the spikes related to that spike analysis for
     * audio file located at specified {@code filePath}.
     * otherwise.
     */
    public void deleteSpikeAnalysis(@NonNull String filePath) {
        analysisRepository.deleteSpikeAnalysis(filePath);
    }

    /**
     * Returns array of spike values and indexes belonging to spike analysis with specified {@code analysisId} for the specified range.
     */
    public SpikeIndexValue[] getSpikesForRange(long analysisId, int channel, int startIndex, int endIndex) {
        return analysisRepository.getSpikeAnalysisValuesAndIndicesForRange(analysisId, channel, startIndex, endIndex);
    }

    /**
     * Returns array of spike values and indexes belonging to train with specified {@code trainId} for specified {@code
     * channel} for the specified range.
     */
    public SpikeIndexValue[] getSpikesByTrainForRange(long trainId, int channel, int startIndex, int endIndex) {
        return analysisRepository.getSpikesByTrainForRange(trainId, channel, startIndex, endIndex);
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
        new FindSpikesAnalysis(audioFile, new BaseAnalysis.AnalysisListener<Spike[]>() {
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
     * Listens for DB query response when retrieving existing spike trains.
     */
    public interface GetThresholdsCallback {
        /**
         * Triggered when spike trains are retrieved from database.
         */
        void onThresholdsLoaded(@NonNull List<Threshold> thresholds);
    }

    /**
     * Returns thresholds for all the existing spike trains for the specified {@code channel} of the audio file with the
     * specified {@code filePath}.
     */
    public void getSpikeTrainThresholdsByChannel(@NonNull String filePath, int channel,
        @Nullable final GetThresholdsCallback callback) {
        analysisRepository.getSpikeAnalysisTrainsByChannel(filePath, channel,
            new AnalysisDataSource.GetAnalysisCallback<Train[]>() {
                @Override public void onAnalysisLoaded(@NonNull Train[] result) {
                    final List<Threshold> thresholds = new ArrayList<>();
                    for (Train train : result) {
                        int leftThreshold = train.isLowerLeft() ? train.getLowerThreshold() : train.getUpperThreshold();
                        int rightThreshold =
                            train.isLowerLeft() ? train.getUpperThreshold() : train.getLowerThreshold();
                        thresholds.add(new Threshold(leftThreshold, rightThreshold));
                    }
                    if (callback != null) callback.onThresholdsLoaded(thresholds);
                }

                @Override public void onDataNotAvailable() {
                    if (callback != null) callback.onThresholdsLoaded(new ArrayList<>());
                }
            });
    }

    /**
     * Returns existing spike trains for the specified {@code channel} of the audio file with specified {@code filePath}.
     */
    public void getSpikeTrains(@NonNull String filePath,
        @Nullable AnalysisDataSource.GetAnalysisCallback<Train[]> callback) {
        analysisRepository.getSpikeAnalysisTrains(filePath, callback);
    }

    /**
     * Adds new spike train for the audio file with specified {@code filePath}. Threshold values are initally set to
     * {@code 0}.
     */
    public void addSpikeTrain(@NonNull String filePath, int channelCount,
        @Nullable final AnalysisDataSource.AddSpikeAnalysisTrainCallback callback) {
        analysisRepository.addSpikeAnalysisTrain(filePath, channelCount, order -> {
            if (callback != null) callback.onSpikeAnalysisTrainAdded(order);
        });
    }

    /**
     * Removes spike train at the specified {@code index} for the audio file with specified {@code filePath}.
     */
    public void removeSpikeTrain(int index, @NonNull String filePath,
        @Nullable final AnalysisDataSource.RemoveSpikeAnalysisTrainCallback callback) {
        analysisRepository.removeSpikeAnalysisTrain(filePath, index, newTrainCount -> {
            if (callback != null) callback.onSpikeAnalysisTrainRemoved(newTrainCount);
        });
    }

    /**
     * Sets specified {@code value} as threshold  with specified {@code orientation} for the spike train at specified
     * {@code index} for the specified {@code channel} of audio file with specified {@code filePath}.
     */
    public void setThreshold(@NonNull String filePath, int channel, int index, @ThresholdOrientation int orientation,
        int value) {
        analysisRepository.saveSpikeAnalysisTrain(filePath, channel, index, orientation, value);
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

        @SuppressLint("SwitchIntDef") @Override public void onDataNotAvailable() {
            switch (analysisType) {
                case AnalysisType.AUTOCORRELATION:
                    autocorrelation = null;
                    break;
                case AnalysisType.ISI:
                    isi = null;
                    break;
                case AnalysisType.CROSS_CORRELATION:
                    crossCorrelation = null;
                    break;
            }

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
                        averageSpikes = null;
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
    @SuppressLint("SwitchIntDef") public void startAnalysis(@NonNull final AnalysisConfig analysisConfig) {
        final String filePath = analysisConfig.getFilePath();
        final @AnalysisType int type = analysisConfig.getAnalysisType();
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
            case AnalysisType.EVENT_TRIGGERED_AVERAGE:
                final EventTriggeredAveragesConfig etaConfig = (EventTriggeredAveragesConfig) analysisConfig;
                eventTriggeredAverageAnalysis(filePath, etaConfig);
                break;
        }
    }

    //=================================================
    //  EVENT TRIGGERED AVERAGES
    //=================================================

    /**
     * Returns results for the Event Triggered Average analysis
     */
    @Nullable public EventTriggeredAverages[] getEventTriggeredAverages() {
        return eventTriggeredAverages;
    }

    // Loads file with specified filePath if not already loaded and starts Event Triggered Average analysis.
    private void eventTriggeredAverageAnalysis(@NonNull String filePath, @Nullable EventTriggeredAveragesConfig etaConfig) {
        if (audioFile != null) {
            if (!ObjectUtils.equals(filePath, audioFile.getAbsolutePath())) {
                if (load(filePath)) {
                    eventTriggeredAverageAnalysis(etaConfig);
                } else {
                    // TODO: 09-Feb-18 BROADCAST EVENT THAT LOADING OF THE FILE FAILED
                }
            } else {
                eventTriggeredAverageAnalysis(etaConfig);
            }
        } else {
            if (load(filePath)) {
                eventTriggeredAverageAnalysis(etaConfig);
            } else {
                // TODO: 09-Feb-18 BROADCAST EVENT THAT LOADING OF THE FILE FAILED
            }
        }
    }

    // Starts Event Triggered Average analysis depending on the set flags.
    private void eventTriggeredAverageAnalysis(@Nullable EventTriggeredAveragesConfig etaConfig) {
        LOGD(TAG, "eventTriggeredAverageAnalysis()");
        if (audioFile != null && etaConfig != null) {
            new EventTriggeredAverageAnalysis(audioFile, new BaseAnalysis.AnalysisListener<EventTriggeredAverages[]>() {
                @Override
                public void onAnalysisDone(@NonNull String filePath, @Nullable EventTriggeredAverages[] result) {
                    eventTriggeredAverages = result;
                    // post event that audio file analysis successfully finished
                    EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.EVENT_TRIGGERED_AVERAGE));
                }

                @Override public void onAnalysisFailed(@NonNull String filePath) {
                    // post event that audio file analysis failed
                    EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.EVENT_TRIGGERED_AVERAGE));
                }
            }).startAnalysis(etaConfig);
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
        new AutocorrelationAnalysis(filePath, new BaseAnalysis.AnalysisListener<int[][]>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable int[][] result) {
                autocorrelation = result;
                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.AUTOCORRELATION));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.AUTOCORRELATION));
            }
        }).startAnalysis(spikeAnalysisByTrains);
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
        new IsiAnalysis(filePath, new BaseAnalysis.AnalysisListener<int[][]>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable int[][] result) {
                isi = result;
                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.ISI));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.ISI));
            }
        }).startAnalysis(spikeAnalysisByTrains);
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
        new CrossCorrelationAnalysis(filePath, new BaseAnalysis.AnalysisListener<int[][]>() {
            @Override public void onAnalysisDone(@NonNull String filePath, @Nullable int[][] result) {
                crossCorrelation = result;
                // post event that audio file analysis successfully finished
                EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.CROSS_CORRELATION));
            }

            @Override public void onAnalysisFailed(@NonNull String filePath) {
                // post event that audio file analysis failed
                EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.CROSS_CORRELATION));
            }
        }).startAnalysis(spikeAnalysisByTrains);
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
            new AverageSpikeAnalysis(audioFile, new BaseAnalysis.AnalysisListener<AverageSpike[]>() {
                @Override public void onAnalysisDone(@NonNull String filePath, @Nullable AverageSpike[] result) {
                    averageSpikes = result;
                    // post event that audio file analysis is successfully finished
                    EventBus.getDefault().post(new AnalysisDoneEvent(true, AnalysisType.AVERAGE_SPIKE));
                }

                @Override public void onAnalysisFailed(@NonNull String filePath) {
                    // post event that audio file analysis is successfully finished
                    EventBus.getDefault().post(new AnalysisDoneEvent(false, AnalysisType.AVERAGE_SPIKE));
                }
            }).startAnalysis(spikeAnalysisByTrains);
        }
    }
}
