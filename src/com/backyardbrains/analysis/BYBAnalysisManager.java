package com.backyardbrains.analysis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.audio.WavAudioFile;
import com.backyardbrains.drawing.ThresholdOrientation;
import com.backyardbrains.events.AudioAnalysisDoneEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class BYBAnalysisManager {

    private static final String TAG = makeLogTag(BYBAnalysisManager.class);

    private static final int MAX_THRESHOLDS = 3;

    private Context context;
    private BYBAudioFile audioFile;

    private BYBFindSpikesAnalysis spikesAnalysis;
    private boolean bSpikesDone;
    private BYBSpike[] spikes;

    private int selectedThreshold;
    private ArrayList<int[]> thresholds;
    private boolean bThresholdsChanged;

    private List<List<BYBSpike>> spikeTrains;
    private boolean bSpikeTrainsDone;

    private BYBIsiAnalysis isiAnalysis;
    private boolean bProcessISI;
    private boolean bISIDone;

    private BYBAutocorrelationAnalysis autocorrelationAnalysis;
    private boolean bProcessAutocorrelation = false;
    private boolean bAutocorrelationDone = false;

    private BYBCrossCorrelationAnalysis crossCorrelationAnalysis;
    private boolean bProcessCrossCorrelation = false;
    private boolean bCrossCorrelationDone = false;

    private BYBAverageSpikeAnalysis averageSpikeAnalysis;
    private boolean bProcessAverageSpike = false;
    private boolean bAverageSpikeDone = false;

    public BYBAnalysisManager(Context context) {
        this.context = context.getApplicationContext();

        // init thresholds, we have one pair of thresholds by default
        thresholds = new ArrayList<>();
        thresholds.add(new int[2]);
    }

    /**
     * Whether specified {@code filePath} is path to currently processed audio file.
     */
    public boolean isCurrentFile(@NonNull String filePath) {
        return audioFile != null && audioFile.getAbsolutePath().equals(filePath);
    }

    //=================================================
    //  FIND SPIKES
    //=================================================

    /**
     * Loads file with specified {@code filePath} if not already loaded and starts the process of finding spikes.
     */
    public void findSpikes(@NonNull String filePath) {
        if (audioFile != null) {
            if (!filePath.equals(audioFile.getAbsolutePath())) {
                load(filePath);
            } else {
                process();
            }
        } else {
            load(filePath);
        }
    }

    /**
     * Returns array of spikes found during the spike analysis.
     */
    public BYBSpike[] getSpikes() {
        if (spikesFound()) {
            return spikes;
        } else {
            return new BYBSpike[0];
        }
    }

    /**
     * Whether process of analysing spikes is finished or not.
     */
    public boolean spikesFound() {
        return (getThresholdsSize() > 0 && spikes != null && spikes.length > 0 && bSpikesDone);
    }

    // Loads file with specified file path into WavAudioFile for further processing
    private boolean load(@NonNull String filePath) {
        try {
            return load(new File(filePath));
        } catch (IOException e) {
            LOGE(TAG, "Error while loading " + filePath);
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

        findSpikes();

        return true;
    }

    // Clears current spike analysis and triggers the new one
    private void findSpikes() {
        LOGD(TAG, "findSpikes begin");

        spikesAnalysis = null;
        spikesAnalysis = new BYBFindSpikesAnalysis(audioFile, new BYBBaseAnalysis.AnalysisListener() {

            @Override public void onAnalysisDone() {
                if (spikesAnalysis != null) {
                    spikes = spikesAnalysis.getSpikes();

                    spikesAnalysis = null;
                    bSpikesDone = true;

                    process();

                    // post event that audio file analysis failed
                    EventBus.getDefault().post(new AudioAnalysisDoneEvent(true, BYBAnalysisType.FIND_SPIKES));
                }
            }

            @Override public void onAnalysisCanceled() {
                bSpikesDone = false;

                // post event that audio file analysis failed
                EventBus.getDefault().post(new AudioAnalysisDoneEvent(false, BYBAnalysisType.FIND_SPIKES));
            }
        });
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
            } finally {
                audioFile = null;
            }
        }

        spikes = null;
        bSpikesDone = false;

        resetAnalysisFlags();

        bThresholdsChanged = false;
        clearThresholds();
    }

    // Resets all the flags that influence analysis process
    private void resetAnalysisFlags() {
        clearSpikeTrains();
        bSpikeTrainsDone = false;

        isiAnalysis = null;
        bProcessISI = false;
        bISIDone = false;

        autocorrelationAnalysis = null;
        bProcessAutocorrelation = false;
        bAutocorrelationDone = false;

        crossCorrelationAnalysis = null;
        bProcessCrossCorrelation = false;
        bCrossCorrelationDone = false;

        averageSpikeAnalysis = null;
        bProcessAverageSpike = false;
        bAverageSpikeDone = false;
    }

    //=================================================
    //  SPIKE TRAINS
    //=================================================

    private List<List<BYBSpike>> processSpikeTrains() {
        if (!bSpikeTrainsDone || bThresholdsChanged) {
            clearSpikeTrains();
            spikeTrains = new ArrayList<>();
            for (int j = 0; j < thresholds.size(); j++) {
                int min = Math.min(thresholds.get(j)[0], thresholds.get(j)[1]);
                int max = Math.max(thresholds.get(j)[0], thresholds.get(j)[1]);
                ArrayList<BYBSpike> temp = new ArrayList<>();
                for (BYBSpike spike : spikes) {
                    if (spike.value >= min && spike.value <= max) temp.add(spike);
                }
                spikeTrains.add(temp);
            }
            bSpikeTrainsDone = true;
        }
        return spikeTrains;
    }

    private void clearSpikeTrains() {
        if (spikeTrains != null) {
            for (int i = 0; i < spikeTrains.size(); i++) {
                if (spikeTrains.get(i) != null) {
                    spikeTrains.get(i).clear();
                }
            }
            spikeTrains.clear();
            spikeTrains = null;
        }
    }

    //=================================================
    //  ANALYZE FILE
    //=================================================

    /**
     * Initializes the process of analyzing currently loaded audio file. If the file hasn't yet been loaded it's loaded
     * and then analyzed. If the file has already been analyzed {@code false} is returned, {@code true} otherwise.
     */
    @SuppressLint("SwitchIntDef") public boolean analyzeFile(@NonNull String filePath, @BYBAnalysisType int type) {
        boolean alreadyAnalyzed = false;
        switch (type) {
            case BYBAnalysisType.AUTOCORRELATION:
                if (!bAutocorrelationDone || bThresholdsChanged) {
                    bProcessAutocorrelation = true;
                    bSpikeTrainsDone = false;
                } else {
                    alreadyAnalyzed = true;
                }
                break;
            case BYBAnalysisType.AVERAGE_SPIKE:
                if (!bAverageSpikeDone || bThresholdsChanged) {
                    bProcessAverageSpike = true;
                    bSpikeTrainsDone = false;
                } else {
                    alreadyAnalyzed = true;
                }
                break;
            case BYBAnalysisType.CROSS_CORRELATION:
                if (!bCrossCorrelationDone || bThresholdsChanged) {
                    bProcessCrossCorrelation = true;
                    bSpikeTrainsDone = false;
                } else {
                    alreadyAnalyzed = true;
                }
                break;
            case BYBAnalysisType.ISI:
                if (!bISIDone || bThresholdsChanged) {
                    bProcessISI = true;
                    bSpikeTrainsDone = false;
                } else {
                    alreadyAnalyzed = true;
                }
                break;
        }

        if (alreadyAnalyzed) EventBus.getDefault().post(new AudioAnalysisDoneEvent(true, type));

        // in case file is not already loaded let's do it
        findSpikes(filePath);

        return !alreadyAnalyzed;
    }

    // Starts one of the analysis depending on the set flags
    private void process() {
        LOGD(TAG, "process!");
        if (spikesFound()) {
            if (bProcessISI) {
                isiAnalysis();
                bProcessISI = false;
            }
            if (bProcessAutocorrelation) {
                autocorrelationAnalysis();
                bProcessAutocorrelation = false;
            }
            if (bProcessCrossCorrelation) {
                crossCorrelationAnalysis();
                bProcessCrossCorrelation = false;
            }
            if (bProcessAverageSpike) {
                averageSpikeAnalysis();
                bProcessAverageSpike = false;
            }
        }
        bThresholdsChanged = false;
    }

    //=================================================
    //  ISI (Inter Spike Interval)
    //=================================================

    /**
     * Returns results for the Inter Spike Interval analysis
     */
    @Nullable public List<List<BYBInterSpikeInterval>> getISI() {
        return isiAnalysis != null ? isiAnalysis.getIsi() : null;
    }

    // Starts Inter Spike Interval analysis depending on the set flags.
    private void isiAnalysis() {
        LOGD(TAG, "isiAnalysis()");
        if (!bISIDone || bThresholdsChanged) {
            processSpikeTrains();

            bProcessISI = true;
            isiAnalysis = new BYBIsiAnalysis(spikeTrains, new BYBBaseAnalysis.AnalysisListener() {
                @Override public void onAnalysisDone() {
                    bISIDone = true;
                    bProcessISI = false;

                    // post event that audio file analysis is successfully finished
                    EventBus.getDefault().post(new AudioAnalysisDoneEvent(true, BYBAnalysisType.ISI));
                }

                @Override public void onAnalysisCanceled() {
                    bISIDone = false;
                    bProcessISI = false;

                    // post event that audio file analysis is successfully finished
                    EventBus.getDefault().post(new AudioAnalysisDoneEvent(false, BYBAnalysisType.ISI));
                }
            });
        }
    }

    //=================================================
    //  AUTOCORRELATION
    //=================================================

    /**
     * Returns results for the Autocorrelation analysis
     */
    @Nullable public List<List<Integer>> getAutocorrelation() {
        return autocorrelationAnalysis != null ? autocorrelationAnalysis.getAutoCorrelation() : null;
    }

    // Starts Autocorrelation analysis depending on the set flags.
    private void autocorrelationAnalysis() {
        LOGD(TAG, "autocorrelationAnalysis()");
        if (!bAutocorrelationDone || bThresholdsChanged) {
            processSpikeTrains();

            bProcessAutocorrelation = true;
            autocorrelationAnalysis =
                new BYBAutocorrelationAnalysis(spikeTrains, new BYBBaseAnalysis.AnalysisListener() {
                    @Override public void onAnalysisDone() {
                        bAutocorrelationDone = true;
                        bProcessAutocorrelation = false;

                        // post event that audio file analysis is successfully finished
                        EventBus.getDefault().post(new AudioAnalysisDoneEvent(true, BYBAnalysisType.AUTOCORRELATION));
                    }

                    @Override public void onAnalysisCanceled() {
                        bAutocorrelationDone = false;
                        bProcessAutocorrelation = false;

                        // post event that audio file analysis is successfully finished
                        EventBus.getDefault().post(new AudioAnalysisDoneEvent(false, BYBAnalysisType.AUTOCORRELATION));
                    }
                });
        }
    }

    //=================================================
    //  CROSS-CORRELATION
    //=================================================

    /**
     * Returns results for the Cross-Correlation analysis
     */
    @Nullable public List<List<Integer>> getCrossCorrelation() {
        return crossCorrelationAnalysis != null ? crossCorrelationAnalysis.getCrossCorrelation() : null;
    }

    // Starts Cross-Correlation analysis depending on the set flags.
    private void crossCorrelationAnalysis() {
        LOGD(TAG, "crossCorrelationAnalysis()");
        if (!bCrossCorrelationDone || bThresholdsChanged) {
            processSpikeTrains();

            bProcessCrossCorrelation = true;
            crossCorrelationAnalysis =
                new BYBCrossCorrelationAnalysis(spikeTrains, new BYBBaseAnalysis.AnalysisListener() {
                    @Override public void onAnalysisDone() {
                        bCrossCorrelationDone = true;
                        bProcessCrossCorrelation = false;

                        // post event that audio file analysis is successfully finished
                        EventBus.getDefault().post(new AudioAnalysisDoneEvent(true, BYBAnalysisType.CROSS_CORRELATION));
                    }

                    @Override public void onAnalysisCanceled() {
                        bCrossCorrelationDone = false;
                        bProcessCrossCorrelation = false;

                        // post event that audio file analysis is successfully finished
                        EventBus.getDefault().post(new AudioAnalysisDoneEvent(false, BYBAnalysisType.CROSS_CORRELATION));
                    }
                });
        }
    }

    //=================================================
    //  AVERAGE SPIKE
    //=================================================

    /**
     * Returns results for the Average Spike analysis
     */
    @Nullable public BYBAverageSpike[] getAverageSpike() {
        return averageSpikeAnalysis != null ? averageSpikeAnalysis.getAverageSpikes() : null;
    }

    // Starts Average Spike analysis depending on the set flags.
    private void averageSpikeAnalysis() {
        LOGD(TAG, "averageSpikeAnalysis()");
        if (!bAverageSpikeDone || bThresholdsChanged) {
            if (audioFile != null) {
                processSpikeTrains();

                bProcessAverageSpike = true;
                averageSpikeAnalysis =
                    new BYBAverageSpikeAnalysis(audioFile, spikeTrains, new BYBBaseAnalysis.AnalysisListener() {
                        @Override public void onAnalysisDone() {
                            bAverageSpikeDone = true;
                            bProcessAverageSpike = false;

                            // post event that audio file analysis is successfully finished
                            EventBus.getDefault().post(new AudioAnalysisDoneEvent(true, BYBAnalysisType.AVERAGE_SPIKE));
                        }

                        @Override public void onAnalysisCanceled() {
                            bAverageSpikeDone = false;
                            bProcessAverageSpike = false;

                            // post event that audio file analysis is successfully finished
                            EventBus.getDefault().post(new AudioAnalysisDoneEvent(false, BYBAnalysisType.AVERAGE_SPIKE));
                        }
                    });
            }
        }
    }

    //=================================================
    //  THRESHOLDS
    //=================================================

    public int getMaxThresholds() {
        return MAX_THRESHOLDS;
    }

    public int getThresholdsSize() {
        return thresholds.size();
    }

    public int getSelectedThresholdIndex() {
        return selectedThreshold;
    }

    public int[] getSelectedThresholds() {
        if (selectedThreshold >= 0 && selectedThreshold < thresholds.size()) {
            return thresholds.get(selectedThreshold);
        }
        return new int[2];
    }

    public void selectThreshold(int index) {
        if (index >= 0 && index < MAX_THRESHOLDS) {
            selectedThreshold = index;

            updateThresholdHandles();
        }
    }

    /**
     * Adds new pair of thresholds.
     */
    public void addThreshold() {
        if (thresholds.size() < MAX_THRESHOLDS) {
            thresholds.add(new int[2]);
            selectedThreshold = thresholds.size() - 1;

            updateThresholdHandles();
            resetAnalysisFlags();
        }
    }

    public void removeSelectedThreshold() {
        if (thresholds.size() > 0 && thresholds.size() > selectedThreshold) {
            thresholds.remove(selectedThreshold);
            selectedThreshold = thresholds.size() - 1;

            updateThresholdHandles();
            resetAnalysisFlags();
        }
    }

    public void setThreshold(int group, int index, int value) {
        if (thresholds.size() > 0 && thresholds.size() > group && index < 2) {
            thresholds.get(group)[index] = value;

            bThresholdsChanged = true;
            resetAnalysisFlags();
        }
    }

    public void setThreshold(@ThresholdOrientation int orientation, int value) {
        if (thresholds.size() > 0 && thresholds.size() > selectedThreshold) {
            thresholds.get(selectedThreshold)[orientation] = value;

            bThresholdsChanged = true;
            resetAnalysisFlags();
        }
    }

    private void clearThresholds() {
        thresholds.clear();
        selectedThreshold = 0;

        updateThresholdHandles();
    }

    private void updateThresholdHandles() {
        if (context != null) {
            Intent i = new Intent();
            i.setAction("BYBUpdateThresholdHandle");
            context.getApplicationContext().sendBroadcast(i);

            bThresholdsChanged = true;
        }
    }
}
