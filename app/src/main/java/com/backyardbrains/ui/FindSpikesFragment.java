package com.backyardbrains.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.events.AnalysisDoneEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesFragment extends PlaybackScopeFragment {

    private static final String TAG = makeLogTag(FindSpikesFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";

    private static int[] HANDLE_COLORS = { Colors.RED_HEX, Colors.YELLOW_HEX, Colors.GREEN_HEX };

    // Max number of thresholds
    private static final int MAX_THRESHOLDS = 3;
    // Holds names of all the available channels
    private static final List<String> CHANNEL_NAMES = new ArrayList<>();

    @BindView(R.id.ll_finding_spikes_progress) ViewGroup llFindingSpikesProgress;
    @BindView(R.id.tv_select_channel) TextView tvSelectChannel;
    @BindView(R.id.ibtn_remove_threshold) ImageButton ibtnRemoveThreshold;
    @BindViews({ R.id.threshold0, R.id.threshold1, R.id.threshold2 }) List<ImageButton> thresholdButtons;
    @BindView(R.id.ibtn_add_threshold) ImageButton ibtnAddThreshold;

    private Unbinder unbinder;

    // Index of the currently selected channel
    int selectedChannel;
    // Index of the currently selected threshold
    int selectedThreshold;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link FindSpikesFragment}.
     */
    public static FindSpikesFragment newInstance(@Nullable String filePath) {
        final FindSpikesFragment fragment = new FindSpikesFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onResume() {
        super.onResume();
        updateThresholdActions();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //=================================================
    //  ABSTRACT METHODS IMPLEMENTATIONS AND OVERRIDES
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override protected FindSpikesRenderer createRenderer() {
        final FindSpikesRenderer renderer = new FindSpikesRenderer(this, filePath);
        renderer.setOnDrawListener((drawSurfaceWidth) -> setMilliseconds(sampleRate, drawSurfaceWidth));
        renderer.setOnScrollListener(new BaseWaveformRenderer.OnScrollListener() {

            @Override public void onScrollStart() {
                startSeek();
            }

            @Override public void onScroll(float dx) {
                seek(getProgress(dx));
            }

            @Override public void onScrollEnd() {
                stopSeek();
            }
        });
        return renderer;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected FindSpikesRenderer getRenderer() {
        return (FindSpikesRenderer) super.getRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected View createPlaybackView(LayoutInflater inflater, @NonNull ViewGroup container) {
        final View view = inflater.inflate(R.layout.fragment_spikes, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected boolean showThresholdView() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void startPlaying(boolean autoPlay) {
        if (getAnalysisManager() != null) getAnalysisManager().findSpikes(filePath);
        super.startPlaying(false);
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN) @Override
    public void onAudioPlaybackStartedEvent(AudioPlaybackStartedEvent event) {
        super.onAudioPlaybackStartedEvent(event);

        // button for selecting channels should only be visible with multichannel recordings
        tvSelectChannel.setVisibility(channelCount > 1 ? View.VISIBLE : View.GONE);
        // populate channel names collection
        CHANNEL_NAMES.clear();
        for (int i = 0; i < channelCount; i++) {
            CHANNEL_NAMES.add(String.format(getString(R.string.template_channel_name), i + 1));
        }

        // let's set the playhead so that the begining of the waveform is at the middle of the screen
        int playhead = (int) (getRenderer().getGlWindowWidth() * channelCount * .5f);
        playhead -= playhead % channelCount;
        seek(playhead);
        sbAudioProgress.setProgress(toFrames(playhead));
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnalysisDoneEvent(AnalysisDoneEvent event) {
        LOGD(TAG, "Analysis of audio file finished. Success - " + event.isSuccess());
        if (event.isSuccess() && getAnalysisManager() != null) {
            getAnalysisManager().spikesAnalysisExists(filePath, true, (analysis, trainCount) -> {
                if (trainCount <= 0) addThreshold();
                updateThresholdActions();
            });
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().spikesAnalysisExists(filePath, false, (analysis, trainCount) -> {
                // if Find Spike analysis doesn't exist show loader until analysis is finished
                if (analysis == null) llFindingSpikesProgress.setVisibility(View.VISIBLE);
            });
        }

        tvSelectChannel.setOnClickListener(v -> openChannelsDialog());
        // set initial selected channel
        if (getAudioService() != null) getAudioService().setSelectedChannel(selectedChannel);

        setupThresholdActions();

        ibtnPlayPause.setVisibility(View.GONE);
        tvProgressTime.setVisibility(View.GONE);
    }

    // Opens a dialog for channel selection
    void openChannelsDialog() {
        if (getContext() != null) {
            MaterialDialog channelsDialog = new MaterialDialog.Builder(getContext()).items(CHANNEL_NAMES)
                .itemsCallbackSingleChoice(selectedChannel, (dialog, itemView, which, text) -> {
                    selectedChannel = which;
                    if (getAudioService() != null) getAudioService().setSelectedChannel(which);
                    return true;
                })
                .alwaysCallSingleChoiceCallback()
                .itemsGravity(GravityEnum.CENTER)
                .build();
            channelsDialog.show();
        }
    }

    // Initializes threshold actions
    private void setupThresholdActions() {
        // threshold selection buttons
        for (int i = 0; i < thresholdButtons.size(); i++) {
            thresholdButtons.get(i).setColorFilter(HANDLE_COLORS[i]);
            final int index = i;
            thresholdButtons.get(i).setOnClickListener(v -> selectThreshold(index));
        }
        // add threshold
        ibtnAddThreshold.setOnClickListener(v -> addThreshold());
        // remove threshold
        ibtnRemoveThreshold.setOnClickListener(v -> removeSelectedThreshold());
        // select channel
    }

    // Selects the threshold at specified index and updates UI.
    void selectThreshold(int index) {
        if (index >= 0 && index < MAX_THRESHOLDS) {
            selectedThreshold = index;

            updateThresholdActions();
        }
    }

    // Adds a new threshold to analysis manager and updates UI.
    void addThreshold() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().addSpikeTrain(filePath, channelCount, order -> {
                selectedThreshold = order;
                updateThresholdActions();
            });
        }
    }

    // Removes currently selected threshold and updates UI.
    void removeSelectedThreshold() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().removeSpikeTrain(selectedThreshold, filePath, newTrainCount -> {
                selectedThreshold = newTrainCount - 1;
                updateThresholdActions();
            });
        }
    }

    // Updates threshold actions
    void updateThresholdActions() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().getSpikeTrainThresholdsByChannel(filePath, selectedChannel, thresholds -> {
                final int thresholdsSize = thresholds.size();
                if (getRenderer() != null && thresholdsSize > 0 && selectedThreshold >= 0
                    && selectedThreshold < MAX_THRESHOLDS) {
                    llFindingSpikesProgress.setVisibility(View.GONE);

                    getRenderer().setSelectedSpikeTrain(selectedThreshold);

                    for (int i = 0; i < MAX_THRESHOLDS; i++) {
                        if (i < thresholdsSize) {
                            thresholdButtons.get(i).setVisibility(View.VISIBLE);
                        } else {
                            thresholdButtons.get(i).setVisibility(View.GONE);
                        }
                    }
                    ibtnAddThreshold.setVisibility(thresholdsSize < MAX_THRESHOLDS ? View.VISIBLE : View.GONE);
                    ibtnRemoveThreshold.setVisibility(thresholdsSize > 1 ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}