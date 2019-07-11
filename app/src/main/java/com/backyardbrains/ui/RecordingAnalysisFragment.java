package com.backyardbrains.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.backyardbrains.R;
import com.backyardbrains.analysis.AnalysisType;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.events.AnalyzeAudioFileEvent;
import com.backyardbrains.events.AnalyzeEventTriggeredAveragesEvent;
import com.backyardbrains.events.FindSpikesEvent;
import com.backyardbrains.events.OpenRecordingOptionsEvent;
import com.backyardbrains.ui.BaseOptionsFragment.OptionsAdapter.OptionItem;
import com.backyardbrains.ui.dialogs.EventTriggeredAverageOptionsDialog;
import com.backyardbrains.ui.dialogs.EventTriggeredAverageOptionsDialog.Options;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.RecordingUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class RecordingAnalysisFragment extends BaseOptionsFragment {

    public static final String TAG = makeLogTag(RecordingAnalysisFragment.class);

    private static final String ARG_FILE_PATH = "bb_analysis_id";

    private enum Option {
        ID_EVENT_TRIGGERED_AVERAGE(0), ID_FIND_SPIKES(1), ID_AUTOCORRELATION(2), ID_ISI(3), ID_CROSS_CORRELATION(
            4), ID_AVERAGE_SPIKE(5);

        private final int id;

        Option(final int id) {
            this.id = id;
        }

        public int value() {
            return id;
        }

        public static Option find(int id) {
            for (Option v : values()) {
                if (v.id == id) return v;
            }

            return null;
        }
    }

    private final EventTriggeredAverageOptionsDialog.OnSelectOptionsListener eventSelectionListener =
        this::eventTriggeredAverage;

    private EventTriggeredAverageOptionsDialog eventTriggeredAverageOptionsDialog;
    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];
    private String filePath;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link RecordingAnalysisFragment}.
     */
    public static RecordingAnalysisFragment newInstance(@Nullable String filePath) {
        final RecordingAnalysisFragment fragment = new RecordingAnalysisFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) filePath = getArguments().getString(ARG_FILE_PATH);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        eventTriggeredAverageOptionsDialog =
            new EventTriggeredAverageOptionsDialog(container.getContext(), eventSelectionListener);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) filePath = savedInstanceState.getString(ARG_FILE_PATH);

        if (filePath != null) {
            final File eventsFile = RecordingUtils.getEventFile(new File(filePath));
            spikesAnalysisExists(filePath,
                (analysis, trainCount) -> constructOptions(eventsFile != null, analysis != null, trainCount > 0));
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_FILE_PATH, filePath);
    }

    //==============================================
    //  ABSTRACT IMPLEMENTATIONS
    //==============================================

    @Override protected String getTitle() {
        return RecordingUtils.getFileNameWithoutExtension(new File(filePath));
    }

    @Override protected View.OnClickListener getBackClickListener() {
        return v -> openRecordingOptions();
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Opens recordings list view
    private void openRecordingOptions() {
        EventBus.getDefault().post(new OpenRecordingOptionsEvent(filePath));
    }

    // Specified callback is invoked after check that spike analysis for file located at specified filePath exists or not
    private void spikesAnalysisExists(@NonNull String filePath,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        if (getAnalysisManager() != null) getAnalysisManager().spikesAnalysisExists(filePath, false, callback);
    }

    // Executes option for the specified ID
    void execOption(int id) {
        Option option = Option.find(id);
        if (option == null) return;

        final File f = new File(filePath);
        switch (option) {
            case ID_EVENT_TRIGGERED_AVERAGE:
                openEventTriggeredAverageAnalysisDialog(f);
                break;
            case ID_FIND_SPIKES:
                findSpikes(f);
                break;
            case ID_AUTOCORRELATION:
                autocorrelation(f);
                break;
            case ID_ISI:
                ISI(f);
                break;
            case ID_CROSS_CORRELATION:
                crossCorrelation(f);
                break;
            case ID_AVERAGE_SPIKE:
                averageSpike(f);
                break;
        }
    }

    // Opens a dialog with all available options for Triggered Average Analysis
    private void openEventTriggeredAverageAnalysisDialog(@NonNull File f) {
        final File eventsFile = RecordingUtils.getEventFile(f);
        if (eventsFile != null) {
            final int eventCount = JniUtils.checkEvents(eventsFile.getAbsolutePath(), eventNames);
            if (eventCount != 0) eventTriggeredAverageOptionsDialog.show(eventNames, eventCount);
        }
    }

    // Start process of event triggered average analysis for specified audio file
    private void eventTriggeredAverage(@NonNull Options options) {
        final File f = new File(filePath);
        if (f.exists()) {
            EventBus.getDefault()
                .post(new AnalyzeEventTriggeredAveragesEvent(filePath,
                    Arrays.copyOfRange(options.events, 0, options.eventCount), options.removeNoiseIntervals));
        }
    }

    // Start process of finding spikes for the specified audio file
    void findSpikes(@NonNull File f) {
        if (f.exists()) EventBus.getDefault().post(new FindSpikesEvent(f.getAbsolutePath()));
    }

    // Start process of autocorrelation analysis for specified audio file
    void autocorrelation(@NonNull File f) {
        startAnalysis(f, AnalysisType.AUTOCORRELATION);
    }

    // Start process of inter spike interval analysis for specified audio file
    void ISI(@NonNull File f) {
        startAnalysis(f, AnalysisType.ISI);
    }

    // Start process of cross-correlation analysis for specified audio file
    void crossCorrelation(@NonNull File f) {
        startAnalysis(f, AnalysisType.CROSS_CORRELATION);
    }

    // Start process of average spike analysis for specified audio file
    void averageSpike(@NonNull File f) {
        startAnalysis(f, AnalysisType.AVERAGE_SPIKE);
    }

    // Starts analysis process for specified type and specified audio file
    private void startAnalysis(@NonNull final File file, @AnalysisType final int type) {
        spikesAnalysisExists(file.getAbsolutePath(), (analysis, trainCount) -> {
            if (analysis != null) {
                if (file.exists()) {
                    EventBus.getDefault().post(new AnalyzeAudioFileEvent(file.getAbsolutePath(), type));
                }
            } else {
                BYBUtils.showAlert(getActivity(), getString(R.string.find_spikes_not_done_title),
                    getString(R.string.find_spikes_not_done_message));
            }
        });
    }

    // Creates and sets analysis options for the current file
    void constructOptions(boolean hasEvents, final boolean spikesFound, final boolean showCrossCorrelation) {
        final String[] optionLabels = getResources().getStringArray(R.array.options_recording_analysis);
        final List<OptionItem> options = new ArrayList<>();
        if (hasEvents) options.add(new OptionItem(Option.ID_EVENT_TRIGGERED_AVERAGE.value(), optionLabels[0], true));
        options.add(new OptionItem(Option.ID_FIND_SPIKES.value(), optionLabels[1], true));
        if (spikesFound) {
            options.add(new OptionItem(Option.ID_AUTOCORRELATION.value(), optionLabels[2], true));
            options.add(new OptionItem(Option.ID_ISI.value(), optionLabels[3], true));
            if (showCrossCorrelation) {
                options.add(new OptionItem(Option.ID_CROSS_CORRELATION.value(), optionLabels[4], true));
            }
            options.add(new OptionItem(Option.ID_AVERAGE_SPIKE.value(), optionLabels[5], true));
        }
        setOptions(options, (id, name) -> execOption(id));
    }
}
