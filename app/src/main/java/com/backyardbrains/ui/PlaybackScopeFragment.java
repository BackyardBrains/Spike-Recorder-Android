package com.backyardbrains.ui;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.SeekableWaveformRenderer;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Formats;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class PlaybackScopeFragment extends BaseWaveformFragment {

    private static final String TAG = makeLogTag(PlaybackScopeFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";
    private static final String INT_SAMPLE_RATE = "bb_sample_rate";
    private static final String INT_CHANNEL_COUNT = "bb_channel_count";
    private static final String BOOL_THRESHOLD_ON = "bb_threshold_on";
    private static final String LONG_PLAYBACK_POSITION = "bb_playback_position";

    // Default number of sample sets that should be summed when averaging
    private static final int AVERAGED_SAMPLE_COUNT = 30;

    // Runnable used for updating playback seek bar
    final protected PlaybackSeekRunnable playbackSeekRunnable = new PlaybackSeekRunnable();
    // Runnable used for updating selected samples measurements (RMS, spike count and spike frequency)
    final protected MeasurementsUpdateRunnable measurementsUpdateRunnable = new MeasurementsUpdateRunnable();

    protected ImageButton ibtnThreshold;
    protected ImageButton ibtnAvgTriggerType;
    protected SeekBar sbAvgSamplesCount;
    protected TextView tvAvgSamplesCount;
    protected TextView tvRms;
    protected TextView tvSpikeCount0;
    protected TextView tvSpikeCount1;
    protected TextView tvSpikeCount2;
    protected ImageView ibtnPlayPause;
    protected SeekBar sbAudioProgress;
    protected TextView tvProgressTime;
    protected TextView tvRmsTime;

    protected String filePath;

    // Sample rate that should be used for audio playback (by default 44100)
    int sampleRate = AudioUtils.DEFAULT_SAMPLE_RATE;
    // Channel count that should be used for audio playback (by default 1)
    int channelCount = AudioUtils.DEFAULT_CHANNEL_COUNT;
    // Whether signal triggering is turned on or off
    boolean thresholdOn;
    // Holds position of the playback while in background
    int playbackPosition;

    /**
     * Runnable that is executed on the UI thread every time recording's playhead is updated.
     */
    protected class PlaybackSeekRunnable implements Runnable {

        private int progress;
        private boolean updateProgressTimeLabel;

        @Override public void run() {
            sbAudioProgress.setProgress(toFrames(progress));
            // avoid division by zero
            if (updateProgressTimeLabel) updateProgressTime(progress, sampleRate, channelCount);
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        void setUpdateProgressTimeLabel(@SuppressWarnings("SameParameterValue") boolean updateProgressTimeLabel) {
            this.updateProgressTimeLabel = updateProgressTimeLabel;
        }
    }

    /**
     * Runnable that is executed on the UI thread every time RMS of the selected samples is updated.
     */
    protected class MeasurementsUpdateRunnable implements Runnable {

        private boolean measuring;
        private float[] rms;
        private int[] firstTrainSpikeCounts;
        private float[] firstTrainSpikesPerSecond;
        private int[] secondTrainSpikeCounts;
        private float[] secondTrainSpikesPerSecond;
        private int[] thirdTrainSpikeCounts;
        private float[] thirdTrainSpikesPerSecond;
        private int sampleCount;
        private int selectedChannel;

        @Override public void run() {
            if (getContext() != null && measuring) {
                tvRms.setText(String.format(getString(R.string.template_rms), rms[selectedChannel]));
                // avoid division by zero
                tvRmsTime.setText(Formats.formatTime_s_msec(sampleCount / (float) sampleRate * 1000));
                if (firstTrainSpikeCounts != null && firstTrainSpikeCounts[selectedChannel] >= 0) {
                    tvSpikeCount0.setVisibility(View.VISIBLE);
                    tvSpikeCount0.setText(
                        String.format(getString(R.string.template_spike_count), firstTrainSpikeCounts[selectedChannel],
                            firstTrainSpikesPerSecond[selectedChannel]));
                } else {
                    tvSpikeCount0.setVisibility(View.INVISIBLE);
                }
                if (secondTrainSpikeCounts != null && secondTrainSpikeCounts[selectedChannel] >= 0) {
                    tvSpikeCount1.setVisibility(View.VISIBLE);
                    tvSpikeCount1.setText(
                        String.format(getString(R.string.template_spike_count), secondTrainSpikeCounts[selectedChannel],
                            secondTrainSpikesPerSecond[selectedChannel]));
                } else {
                    tvSpikeCount1.setVisibility(View.INVISIBLE);
                }
                if (thirdTrainSpikeCounts != null && thirdTrainSpikeCounts[selectedChannel] >= 0) {
                    tvSpikeCount2.setVisibility(View.VISIBLE);
                    tvSpikeCount2.setText(
                        String.format(getString(R.string.template_spike_count), thirdTrainSpikeCounts[selectedChannel],
                            thirdTrainSpikesPerSecond[selectedChannel]));
                } else {
                    tvSpikeCount2.setVisibility(View.INVISIBLE);
                }
            }
        }

        void setMeasuring(boolean measuring) {
            this.measuring = measuring;
        }

        void setRms(@NonNull float[] rms) {
            this.rms = rms;
            for (int i = 0; i < rms.length; i++) {
                if (Float.isInfinite(rms[i]) || Float.isNaN(rms[i])) {
                    this.rms[i] = 0f;
                }
            }
        }

        public void setSelectedChannel(int selectedChannel) {
            this.selectedChannel = selectedChannel;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        void setFirstTrainSpikeCounts(@Nullable int[] firstTrainSpikeCountss) {
            this.firstTrainSpikeCounts = firstTrainSpikeCountss;

            if (firstTrainSpikeCountss != null) {
                if (this.firstTrainSpikesPerSecond == null) {
                    this.firstTrainSpikesPerSecond = new float[firstTrainSpikeCountss.length];
                }

                for (int i = 0; i < firstTrainSpikeCountss.length; i++) {
                    firstTrainSpikesPerSecond[i] = (firstTrainSpikeCountss[i] * sampleRate) / (float) sampleCount;
                    if (Float.isInfinite(firstTrainSpikesPerSecond[i]) || Float.isNaN(firstTrainSpikesPerSecond[i])) {
                        firstTrainSpikesPerSecond[i] = 0f;
                    }
                }
            }
        }

        void setSecondTrainSpikeCounts(@Nullable int[] secondTrainSpikeCounts) {
            this.secondTrainSpikeCounts = secondTrainSpikeCounts;

            if (secondTrainSpikeCounts != null) {
                if (this.secondTrainSpikesPerSecond == null) {
                    this.secondTrainSpikesPerSecond = new float[secondTrainSpikeCounts.length];
                }

                for (int i = 0; i < secondTrainSpikeCounts.length; i++) {
                    secondTrainSpikesPerSecond[i] = (secondTrainSpikeCounts[i] * sampleRate) / (float) sampleCount;
                    if (Float.isInfinite(secondTrainSpikesPerSecond[i]) || Float.isNaN(secondTrainSpikesPerSecond[i])) {
                        secondTrainSpikesPerSecond[i] = 0f;
                    }
                }
            }
        }

        void setThirdTrainSpikeCounts(@Nullable int[] thirdTrainSpikeCounts) {
            this.thirdTrainSpikeCounts = thirdTrainSpikeCounts;

            if (thirdTrainSpikeCounts != null) {
                if (this.thirdTrainSpikesPerSecond == null) {
                    this.thirdTrainSpikesPerSecond = new float[thirdTrainSpikeCounts.length];
                }

                for (int i = 0; i < thirdTrainSpikeCounts.length; i++) {
                    thirdTrainSpikesPerSecond[i] = (thirdTrainSpikeCounts[i] * sampleRate) / (float) sampleCount;
                    if (Float.isInfinite(thirdTrainSpikesPerSecond[i]) || Float.isNaN(thirdTrainSpikesPerSecond[i])) {
                        thirdTrainSpikesPerSecond[i] = 0f;
                    }
                }
            }
        }
    }

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link PlaybackScopeFragment}.
     */
    public static PlaybackScopeFragment newInstance(@Nullable String filePath) {
        final PlaybackScopeFragment fragment = new PlaybackScopeFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }
    //==============================================
    // EVENT LISTENERS
    //==============================================

    private final View.OnClickListener startThresholdOnClickListener = v -> {
        startThresholdMode();
        setupThresholdView();
    };

    private final View.OnClickListener stopThresholdOnClickListener = v -> {
        stopThresholdMode();
        setupThresholdView();
    };

    private final SeekBar.OnSeekBarChangeListener averagedSampleCountChangeListener =
        new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // average sample count has changed
                int averagedSampleCount = seekBar.getProgress() > 0 ? seekBar.getProgress() : 1;
                PrefUtils.setAveragedSampleCount(seekBar.getContext(), BaseWaveformFragment.class, averagedSampleCount);
                JniUtils.setAveragedSampleCount(averagedSampleCount);
                // in case we are pausing just reset the renderer buffers
                getRenderer().resetAveragedSignal();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // minimum sample count is 1
                if (progress <= 0) progress = 1;

                // update count label
                if (tvAvgSamplesCount != null) {
                    tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), progress));
                }
            }
        };

    private final View.OnClickListener changeAveragingTriggerTypeOnClickListener =
        v -> openAveragingTriggerTypeDialog();

    private final SeekBar.OnSeekBarChangeListener playbackSeekChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override public void onStartTrackingTouch(SeekBar seekBar) {
            startSeek();
        }

        @Override public void onStopTrackingTouch(SeekBar seekBar) {
            stopSeek();
        }

        @Override public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
            if (fromUser) seek(toSamples(progress));
        }
    };

    //=================================================
    //  LIFECYCLE IMPLEMENTATIONS
    //=================================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) filePath = getArguments().getString(ARG_FILE_PATH);
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            sampleRate = savedInstanceState.getInt(INT_SAMPLE_RATE, AudioUtils.DEFAULT_SAMPLE_RATE);
            channelCount = savedInstanceState.getInt(INT_CHANNEL_COUNT, AudioUtils.DEFAULT_CHANNEL_COUNT);
            thresholdOn = savedInstanceState.getBoolean(BOOL_THRESHOLD_ON);
            playbackPosition = savedInstanceState.getInt(LONG_PLAYBACK_POSITION, 0);
        }
    }

    @Override public void onStart() {
        super.onStart();

        if (getContext() != null && ApacheCommonsLang3Utils.isBlank(filePath)) {
            ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
            return;
        }

        // everything good, start playback if it hasn't already been started
        startPlaying(true);
    }

    @Override public void onStop() {
        super.onStop();

        stopPlaying();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(INT_SAMPLE_RATE, sampleRate);
        outState.putInt(INT_CHANNEL_COUNT, channelCount);
        outState.putBoolean(BOOL_THRESHOLD_ON, thresholdOn);
        outState.putInt(LONG_PLAYBACK_POSITION, playbackPosition);
    }

    //=================================================
    //  ABSTRACT METHODS IMPLEMENTATIONS AND OVERRIDES
    //=================================================

    @Override protected final View createView(LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_playback_scope, container, false);
        ibtnThreshold = view.findViewById(R.id.ibtn_threshold);
        ibtnAvgTriggerType = view.findViewById(R.id.ibtn_avg_trigger_type);
        sbAvgSamplesCount = view.findViewById(R.id.sb_averaged_sample_count);
        tvAvgSamplesCount = view.findViewById(R.id.tv_averaged_sample_count);
        tvRms = view.findViewById(R.id.tv_rms);
        tvSpikeCount0 = view.findViewById(R.id.tv_spike_count0);
        tvSpikeCount1 = view.findViewById(R.id.tv_spike_count1);
        tvSpikeCount2 = view.findViewById(R.id.tv_spike_count2);
        ibtnPlayPause = view.findViewById(R.id.iv_play_pause);
        sbAudioProgress = view.findViewById(R.id.sb_audio_progress);
        tvProgressTime = view.findViewById(R.id.tv_progress_time);
        tvRmsTime = view.findViewById(R.id.tv_rms_time);

        // we should set averaged sample count before UI setup
        int averagedSampleCount = PrefUtils.getAveragedSampleCount(view.getContext(), BaseWaveformFragment.class);
        if (averagedSampleCount < 0) averagedSampleCount = AVERAGED_SAMPLE_COUNT;
        JniUtils.setAveragedSampleCount(averagedSampleCount);

        setupUI();

        // subclass content
        final FrameLayout flContent = view.findViewById(R.id.playback_scope_content_container);
        final View content = createPlaybackView(inflater, flContent);
        if (content != null) flContent.addView(content);

        return view;
    }

    @Override protected BaseWaveformRenderer createRenderer() {
        final SeekableWaveformRenderer renderer = new SeekableWaveformRenderer(filePath, this);
        renderer.setOnDrawListener((drawSurfaceWidth) -> setMilliseconds(sampleRate, drawSurfaceWidth));
        renderer.setOnWaveformSelectionListener(index -> {
            if (getAudioService() != null) getAudioService().setSelectedChannel(index);
        });
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
        renderer.setOnMeasureListener(new BaseWaveformRenderer.OnMeasureListener() {

            @Override public void onMeasureStart() {
                tvRms.setVisibility(View.VISIBLE);
                tvRmsTime.setVisibility(View.VISIBLE);

                measurementsUpdateRunnable.setMeasuring(true);
            }

            @Override public void onMeasure(@NonNull float[] rms, @Nullable int[] firstTrainSpikeCount,
                @Nullable int[] secondTrainSpikeCount, @Nullable int[] thirdTrainSpikeCount, int selectedChannel,
                int sampleCount) {
                if (getActivity() != null) {
                    measurementsUpdateRunnable.setRms(rms);
                    measurementsUpdateRunnable.setSelectedChannel(selectedChannel);
                    measurementsUpdateRunnable.setSampleCount(sampleCount);
                    measurementsUpdateRunnable.setFirstTrainSpikeCounts(firstTrainSpikeCount);
                    measurementsUpdateRunnable.setSecondTrainSpikeCounts(secondTrainSpikeCount);
                    measurementsUpdateRunnable.setThirdTrainSpikeCounts(thirdTrainSpikeCount);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(measurementsUpdateRunnable);
                }
            }

            @Override public void onMeasureEnd() {
                measurementsUpdateRunnable.setMeasuring(false);

                tvRms.setVisibility(View.INVISIBLE);
                tvRmsTime.setVisibility(View.INVISIBLE);
                tvSpikeCount0.setVisibility(View.INVISIBLE);
                tvSpikeCount1.setVisibility(View.INVISIBLE);
                tvSpikeCount2.setVisibility(View.INVISIBLE);
            }
        });
        return renderer;
    }

    @Override protected boolean isBackable() {
        return true;
    }

    @Override protected SeekableWaveformRenderer getRenderer() {
        return (SeekableWaveformRenderer) super.getRenderer();
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    /**
     * Subclasses should override this method if they want to provide addition UI elements that will be placed inside a
     * {@link FrameLayout} that matches parent's width and height and is places behind all the UI elements from this
     * view.
     */
    protected View createPlaybackView(LayoutInflater inflater, @NonNull ViewGroup container) {
        return null;
    }

    /**
     * Subclasses should override this method if they shouldn't show the Threshold view.
     */
    protected boolean showThresholdView() {
        return true;
    }

    /**
     * Returns length of the played audio file in frames.
     */
    protected int getLength() {
        if (getAudioService() != null) return toFrames((int) getAudioService().getPlaybackLength());

        return 0;
    }

    /**
     * Plays/pauses audio file depending on the {@code play} flag.
     */
    protected void toggle(boolean play) {
        // resume the threshold
        if (play) JniUtils.resumeThreshold();

        if (getAudioService() != null) getAudioService().togglePlayback(play);
    }

    /**
     * Whether audio file is currently being played or not.
     */
    protected boolean isPlaying() {
        return getAudioService() != null && getAudioService().isAudioPlaying();
    }

    /**
     * Starts playing audio file.
     */
    protected void startPlaying(boolean autoPlay) {
        if (getAudioService() != null) {
            // this will set signal averaging if we are coming from background
            getAudioService().setSignalAveraging(thresholdOn);
            // this will set signal averaging trigger type if we are coming from background
            getAudioService().setSignalAveragingTriggerType(JniUtils.getAveragingTriggerType());
            // this will start playback if we are coming from background
            getAudioService().startPlayback(filePath, autoPlay, playbackPosition);
        }

        // resume the threshold
        JniUtils.resumeThreshold();
    }

    /**
     * Stops playing audio file.
     */
    protected void stopPlaying() {
        if (getAudioService() != null) getAudioService().stopPlayback();
    }

    /**
     * Tells audio service that seek is about to start.
     */
    protected void startSeek() {
        if (getAudioService() != null) getAudioService().startPlaybackSeek();
    }

    /**
     * Tells audio service to seek to the sample at specified {@code position}.
     */
    protected void seek(int progress) {
        if (getAudioService() != null) getAudioService().seekPlayback(progress);

        playbackSeekRunnable.setProgress(progress);
        playbackSeekRunnable.setUpdateProgressTimeLabel(true);
        // we need to call it on UI thread because renderer is drawing on background thread
        if (getActivity() != null) getActivity().runOnUiThread(playbackSeekRunnable);
    }

    /**
     * Tells audio service that seek should stop.f
     */
    protected void stopSeek() {
        if (getAudioService() != null) getAudioService().stopPlaybackSeek();
    }

    /**
     * Calculates and returns playback seek bar progress value using specified {@code dx} value.
     */
    protected int getProgress(float dx) {
        int max = toSamples(sbAudioProgress.getMax());
        int progress = (int) (toSamples(sbAudioProgress.getProgress()) - dx);
        progress -= progress % channelCount;
        if (progress < 0) progress = 0;
        if (progress > max) progress = max;

        return progress;
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @CallSuper @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        // this will init playback if we are coming from background
        startPlaying(false);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPlaybackStartedEvent(AudioPlaybackStartedEvent event) {
        LOGD(TAG, "Start audio playback - " + event.getLength());
        sampleRate = event.getSampleRate();
        channelCount = event.getChannelCount();
        if (event.getLength() > 0) { // we are starting playback, not resuming
            sbAudioProgress.setMax(toFrames((int) event.getLength()));
            EventBus.getDefault().removeStickyEvent(AudioPlaybackStartedEvent.class);

            // threshold should be reset every time playback is started from begining
            JniUtils.resetThreshold();
        }

        setupPlayPauseButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPlaybackProgressEvent(AudioPlaybackProgressEvent event) {
        // can be 0 if AudioPlaybackStartedEvent event was sent before onStart()
        if (sbAudioProgress.getMax() == 0) sbAudioProgress.setMax(getLength());

        sbAudioProgress.setProgress((int) event.getProgress() / event.getChannelCount());
        updateProgressTime((int) event.getProgress(), event.getSampleRate(), event.getChannelCount());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPlaybackStoppedEvent(AudioPlaybackStoppedEvent event) {
        LOGD(TAG, "Stop audio playback - " + (event.isCompleted() ? "end" : "pause"));

        setupPlayPauseButton();
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI() {
        if (showThresholdView()) {
            // threshold button
            ibtnThreshold.setVisibility(View.VISIBLE);
            // threshold view
            setupThresholdView();
        } else {
            ibtnThreshold.setVisibility(View.INVISIBLE);
            ibtnAvgTriggerType.setVisibility(View.INVISIBLE);
        }
        // play/pause button
        setupPlayPauseButton();
        ibtnPlayPause.setOnClickListener(v -> toggle(!isPlaying()));
        // audio progress
        sbAudioProgress.setMax(getLength());
        sbAudioProgress.setOnSeekBarChangeListener(playbackSeekChangeListener);
        sbAudioProgress.setProgress(0);
    }

    // Converts number of specified samples to number of frames
    int toFrames(int samples) {
        return samples / channelCount;
    }

    // Converts number of specified frames to number of samples
    int toSamples(int frames) {
        return frames * channelCount;
    }

    //==============================================
    // THRESHOLD
    //==============================================

    // Sets up the threshold view (button, handle and averaged sample count
    void setupThresholdView() {
        if (thresholdOn) {
            // setup threshold button
            ibtnThreshold.setBackgroundResource(R.drawable.circle_gray_white_active);
            ibtnThreshold.setOnClickListener(stopThresholdOnClickListener);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.VISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(averagedSampleCountChangeListener);
            sbAvgSamplesCount.setProgress(JniUtils.getAveragedSampleCount());
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.VISIBLE);
        } else {
            // setup threshold button
            ibtnThreshold.setBackgroundResource(R.drawable.circle_gray_white);
            ibtnThreshold.setOnClickListener(startThresholdOnClickListener);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.INVISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(null);
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.INVISIBLE);
        }
        setupThresholdHandleAndAveragingTriggerTypeButtons();
    }

    // Sets up threshold handle and averaging trigger type button
    void setupThresholdHandleAndAveragingTriggerTypeButtons() {
        if (thresholdOn) {
            // setup averaging trigger type button
            ibtnAvgTriggerType.setVisibility(View.VISIBLE);
            final @SignalAveragingTriggerType int triggerType = JniUtils.getAveragingTriggerType();
            switch (triggerType) {
                case SignalAveragingTriggerType.ALL_EVENTS:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_all_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_1:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_1_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_2:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_2_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_3:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_3_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_4:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_4_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_5:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_5_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_6:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_6_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_7:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_7_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_8:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_8_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_9:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_9_black_24dp);
                    break;
                case SignalAveragingTriggerType.THRESHOLD:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_threshold_black_24dp);
                    break;
            }
            ibtnAvgTriggerType.setOnClickListener(changeAveragingTriggerTypeOnClickListener);
        } else {
            // setup averaging trigger type button
            ibtnAvgTriggerType.setVisibility(View.INVISIBLE);
            ibtnAvgTriggerType.setOnClickListener(null);
        }
    }

    // Starts the threshold mode
    void startThresholdMode() {
        thresholdOn = true;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);
    }

    // Stops the threshold mode
    void stopThresholdMode() {
        thresholdOn = false;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);

        // threshold should be reset every time it's enabled so let's reset every time on closing
        JniUtils.resetThreshold();
    }

    // Opens a dialog for averaging trigger type selection
    void openAveragingTriggerTypeDialog() {
        if (getContext() != null) {
            MaterialDialog averagingTriggerTypeDialog =
                new MaterialDialog.Builder(getContext()).items(R.array.options_averaging_trigger_type)
                    .itemsCallback((dialog, itemView, position, text) -> setTriggerType(
                        position == 0 ? SignalAveragingTriggerType.THRESHOLD
                            : position == 10 ? SignalAveragingTriggerType.ALL_EVENTS : position))
                    .build();
            averagingTriggerTypeDialog.show();
        }
    }

    // Sets the specified trigger type as the preferred averaging trigger type
    void setTriggerType(@SignalAveragingTriggerType int triggerType) {
        if (getAudioService() != null) getAudioService().setSignalAveragingTriggerType(triggerType);

        setupThresholdHandleAndAveragingTriggerTypeButtons();
    }

    //==============================================
    // PLAY/PAUSE/PROGRESS
    //==============================================

    // Sets appropriate image on play/pause button
    private void setupPlayPauseButton() {
        LOGD(TAG, "setupPlayPauseButton() - isPlaying=" + isPlaying());
        ibtnPlayPause.setImageResource(
            isPlaying() ? R.drawable.ic_pause_circle_filled_orange_24dp : R.drawable.ic_play_circle_filled_orange_24dp);
    }

    // Updates progress time according to progress
    void updateProgressTime(int progress, int sampleRate, int channelCount) {
        playbackPosition = progress;
        tvProgressTime.setText(WavUtils.formatWavProgress(progress, sampleRate, channelCount));
    }
}
