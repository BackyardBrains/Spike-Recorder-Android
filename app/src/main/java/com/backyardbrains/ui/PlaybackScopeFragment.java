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
    private static final String INT_AVERAGING_TRIGGER_TYPE = "bb_averaging_trigger_type";
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
    // Holds the type of triggering that is used when averaging
    private @SignalAveragingTriggerType int triggerType = SignalAveragingTriggerType.THRESHOLD;

    /**
     * Runnable that is executed on the UI thread every time recording's playhead is updated.
     */
    protected class PlaybackSeekRunnable implements Runnable {

        private int progress;
        private boolean updateProgressSeekBar;
        private boolean updateProgressTimeLabel;

        @Override public void run() {
            seek(progress);
            if (updateProgressSeekBar) sbAudioProgress.setProgress(toFrames(progress));
            // avoid division by zero
            if (updateProgressTimeLabel) updateProgressTime(progress, sampleRate, channelCount);
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        void setUpdateProgressSeekBar(boolean updateProgressSeekBar) {
            this.updateProgressSeekBar = updateProgressSeekBar;
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
        private float rms;
        private int firstTrainSpikeCount;
        private float firstTrainSpikesPerSecond;
        private int secondTrainSpikeCount;
        private float secondTrainSpikesPerSecond;
        private int thirdTrainSpikeCount;
        private float thirdTrainSpikesPerSecond;
        private int sampleCount;

        @Override public void run() {
            if (getContext() != null && measuring) {
                tvRms.setText(String.format(getString(R.string.template_rms), rms));
                // avoid division by zero
                tvRmsTime.setText(Formats.formatTime_s_msec(sampleCount / (float) sampleRate * 1000));
                tvSpikeCount0.setVisibility(firstTrainSpikeCount >= 0 ? View.VISIBLE : View.INVISIBLE);
                tvSpikeCount0.setText(String.format(getString(R.string.template_spike_count), firstTrainSpikeCount,
                    firstTrainSpikesPerSecond));
                tvSpikeCount1.setVisibility(secondTrainSpikeCount >= 0 ? View.VISIBLE : View.INVISIBLE);
                tvSpikeCount1.setText(String.format(getString(R.string.template_spike_count), secondTrainSpikeCount,
                    secondTrainSpikesPerSecond));
                tvSpikeCount2.setVisibility(thirdTrainSpikeCount >= 0 ? View.VISIBLE : View.INVISIBLE);
                tvSpikeCount2.setText(String.format(getString(R.string.template_spike_count), thirdTrainSpikeCount,
                    thirdTrainSpikesPerSecond));
            }
        }

        void setMeasuring(boolean measuring) {
            this.measuring = measuring;
        }

        void setRms(float rms) {
            if (Float.isInfinite(rms) || Float.isNaN(rms)) {
                this.rms = 0f;
                return;
            }

            this.rms = rms;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        void setFirstTrainSpikeCount(int firstTrainSpikeCount) {
            this.firstTrainSpikeCount = firstTrainSpikeCount;

            firstTrainSpikesPerSecond = (firstTrainSpikeCount * sampleRate) / (float) sampleCount;
            if (Float.isInfinite(firstTrainSpikesPerSecond) || Float.isNaN(firstTrainSpikesPerSecond)) {
                firstTrainSpikesPerSecond = 0f;
            }
        }

        void setSecondTrainSpikeCount(int secondTrainSpikeCount) {
            this.secondTrainSpikeCount = secondTrainSpikeCount;

            secondTrainSpikesPerSecond = (secondTrainSpikeCount * sampleRate) / (float) sampleCount;
            if (Float.isInfinite(secondTrainSpikesPerSecond) || Float.isNaN(secondTrainSpikesPerSecond)) {
                secondTrainSpikesPerSecond = 0f;
            }
        }

        void setThirdTrainSpikeCount(int thirdTrainSpikeCount) {
            this.thirdTrainSpikeCount = thirdTrainSpikeCount;

            thirdTrainSpikesPerSecond = (thirdTrainSpikeCount * sampleRate) / (float) sampleCount;
            if (Float.isInfinite(thirdTrainSpikesPerSecond) || Float.isNaN(thirdTrainSpikesPerSecond)) {
                thirdTrainSpikesPerSecond = 0f;
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
            triggerType = savedInstanceState.getInt(INT_AVERAGING_TRIGGER_TYPE, SignalAveragingTriggerType.THRESHOLD);
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
        outState.putInt(INT_AVERAGING_TRIGGER_TYPE, triggerType);
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
        renderer.setOnDrawListener((drawSurfaceWidth) -> {
            if (getActivity() != null) {
                viewableTimeSpanUpdateRunnable.setSampleRate(sampleRate);
                viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                // we need to call it on UI thread because renderer is drawing on background thread
                getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable);
            }
        });
        renderer.setOnScrollListener(new BaseWaveformRenderer.OnScrollListener() {

            @Override public void onScrollStart() {
                startSeek();
            }

            @Override public void onScroll(float dx) {
                if (getActivity() != null) {
                    int max = toSamples(sbAudioProgress.getMax());
                    int progress = (int) (toSamples(sbAudioProgress.getProgress()) - dx);
                    progress -= progress % channelCount;
                    if (progress < 0) progress = 0;
                    if (progress > max) progress = max;
                    playbackSeekRunnable.setProgress(progress);
                    playbackSeekRunnable.setUpdateProgressSeekBar(true);
                    playbackSeekRunnable.setUpdateProgressTimeLabel(true);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(playbackSeekRunnable);
                }
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

            @Override public void onMeasure(float rms, int firstTrainSpikeCount, int secondTrainSpikeCount,
                int thirdTrainSpikeCount, int sampleCount) {
                if (getActivity() != null) {
                    measurementsUpdateRunnable.setRms(rms);
                    measurementsUpdateRunnable.setSampleCount(sampleCount);
                    measurementsUpdateRunnable.setFirstTrainSpikeCount(firstTrainSpikeCount);
                    measurementsUpdateRunnable.setSecondTrainSpikeCount(secondTrainSpikeCount);
                    measurementsUpdateRunnable.setThirdTrainSpikeCount(thirdTrainSpikeCount);
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
        renderer.setSignalAveraging(thresholdOn);
        // if app is opened for the first time averaging trigger type will be THRESHOLD,
        // otherwise it will be the last set value (we retrieve it from C++ code
        renderer.setAveragingTriggerType(triggerType = JniUtils.getAveragingTriggerType());
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
            getAudioService().setSignalAveraging(thresholdOn);
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
    protected void seek(int position) {
        // let's pause the threshold while seeking
        JniUtils.pauseThreshold();

        if (getAudioService() != null) getAudioService().seekPlayback(position);
    }

    /**
     * Tells audio service that seek should stop.f
     */
    protected void stopSeek() {
        if (getAudioService() != null) getAudioService().stopPlaybackSeek();
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
        sbAudioProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                startSeek();
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                stopSeek();
            }

            @Override public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (fromUser) {
                    playbackSeekRunnable.setProgress(toSamples(progress));
                    playbackSeekRunnable.setUpdateProgressSeekBar(false);
                    playbackSeekRunnable.setUpdateProgressTimeLabel(true);
                    seekBar.post(playbackSeekRunnable);
                }
            }
        });
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
            ibtnThreshold.setImageResource(R.drawable.ic_threshold_off);
            ibtnThreshold.setOnClickListener(stopThresholdOnClickListener);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.VISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(averagedSampleCountChangeListener);
            sbAvgSamplesCount.setProgress(JniUtils.getAveragedSampleCount());
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.VISIBLE);
        } else {
            // setup threshold button
            ibtnThreshold.setImageResource(R.drawable.ic_threshold);
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

        getRenderer().onSaveSettings(ibtnThreshold.getContext());
        getRenderer().setSignalAveraging(thresholdOn);
        getRenderer().onLoadSettings(ibtnThreshold.getContext());
    }

    // Stops the threshold mode
    void stopThresholdMode() {
        thresholdOn = false;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);

        getRenderer().onSaveSettings(ibtnThreshold.getContext());
        getRenderer().setSignalAveraging(thresholdOn);
        getRenderer().onLoadSettings(ibtnThreshold.getContext());

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
        this.triggerType = triggerType;

        setupThresholdHandleAndAveragingTriggerTypeButtons();

        JniUtils.setAveragingTriggerType(triggerType);
        getRenderer().setAveragingTriggerType(triggerType);
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
