package com.backyardbrains;

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
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.SeekableWaveformRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Formats;
import com.backyardbrains.utils.Func;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.view.ThresholdHandle;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class PlaybackScopeFragment extends BaseWaveformFragment {

    private static final String TAG = makeLogTag(PlaybackScopeFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";
    private static final String INT_SAMPLE_RATE = "bb_sample_rate";
    private static final String BOOL_THRESHOLD_ON = "bb_threshold_on";
    private static final String LONG_PLAYBACK_POSITION = "bb_playback_position";

    // Default number of sample sets that should be summed when averaging
    private static final int AVERAGED_SAMPLE_COUNT = 30;

    // Runnable used for updating playback seek bar
    final protected PlaybackSeekRunnable playbackSeekRunnable = new PlaybackSeekRunnable();
    // Runnable used for updating selected samples measurements (RMS, spike count and spike frequency)
    final protected MeasurementsUpdateRunnable measurementsUpdateRunnable = new MeasurementsUpdateRunnable();
    // Runnable used for updating threshold handle position
    final SetThresholdHandlePositionRunnable setThresholdHandlePositionRunnable =
        new SetThresholdHandlePositionRunnable();
    // Runnable used for updating threshold processor threshold value
    final UpdateDataProcessorThresholdRunnable updateDataProcessorThresholdRunnable =
        new UpdateDataProcessorThresholdRunnable();

    protected ThresholdHandle thresholdHandle;
    protected ImageButton ibtnThreshold;
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
    int sampleRate = AudioUtils.SAMPLE_RATE;
    // Whether signal triggering is turned on or off
    boolean thresholdOn;
    // Holds position of the playback while in background
    int playbackPosition;

    /**
     * Runnable that is executed on the UI thread every time recording's playhead is updated.
     */
    protected class PlaybackSeekRunnable implements Runnable {

        private int progress;
        private boolean updateProgressSeekBar;
        private boolean updateProgressTimeLabel;

        @Override public void run() {
            seek(progress);
            if (updateProgressSeekBar) sbAudioProgress.setProgress(progress);
            // avoid division by zero
            if (updateProgressTimeLabel) updateProgressTime(progress, sampleRate);
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public void setUpdateProgressSeekBar(boolean updateProgressSeekBar) {
            this.updateProgressSeekBar = updateProgressSeekBar;
        }

        public void setUpdateProgressTimeLabel(boolean updateProgressTimeLabel) {
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

        public void setMeasuring(boolean measuring) {
            this.measuring = measuring;
        }

        public void setRms(float rms) {
            if (Float.isInfinite(rms) || Float.isNaN(rms)) {
                this.rms = 0f;
                return;
            }

            this.rms = rms;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        public void setFirstTrainSpikeCount(int firstTrainSpikeCount) {
            this.firstTrainSpikeCount = firstTrainSpikeCount;

            firstTrainSpikesPerSecond = (firstTrainSpikeCount * sampleRate) / (float) sampleCount;
            if (Float.isInfinite(firstTrainSpikesPerSecond) || Float.isNaN(firstTrainSpikesPerSecond)) {
                firstTrainSpikesPerSecond = 0f;
            }
        }

        public void setSecondTrainSpikeCount(int secondTrainSpikeCount) {
            this.secondTrainSpikeCount = secondTrainSpikeCount;

            secondTrainSpikesPerSecond = (secondTrainSpikeCount * sampleRate) / (float) sampleCount;
            if (Float.isInfinite(secondTrainSpikesPerSecond) || Float.isNaN(secondTrainSpikesPerSecond)) {
                secondTrainSpikesPerSecond = 0f;
            }
        }

        public void setThirdTrainSpikeCount(int thirdTrainSpikeCount) {
            this.thirdTrainSpikeCount = thirdTrainSpikeCount;

            thirdTrainSpikesPerSecond = (thirdTrainSpikeCount * sampleRate) / (float) sampleCount;
            if (Float.isInfinite(thirdTrainSpikesPerSecond) || Float.isNaN(thirdTrainSpikesPerSecond)) {
                thirdTrainSpikesPerSecond = 0f;
            }
        }
    }

    /**
     * Runnable that is executed on the UI thread every time threshold position is changed.
     */
    private class SetThresholdHandlePositionRunnable implements Runnable {

        private int position;

        @Override public void run() {
            setThresholdHandlePosition(position);
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }

    /**
     * Runnable that is executed on the UI thread every time threshold value is changed.
     */
    private class UpdateDataProcessorThresholdRunnable implements Runnable {

        private float value;

        @Override public void run() {
            updateDataProcessorThreshold(value);
        }

        public void setValue(float value) {
            this.value = value;
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

    private final View.OnClickListener startThresholdOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            startThresholdMode();
            setupThresholdView();
        }
    };

    private final View.OnClickListener stopThresholdOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            stopThresholdMode();
            setupThresholdView();
        }
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

    private final ThresholdHandle.OnThresholdChangeListener thresholdChangeListener =
        new ThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                getRenderer().adjustThreshold(y);
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
            sampleRate = savedInstanceState.getInt(INT_SAMPLE_RATE, AudioUtils.SAMPLE_RATE);
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
        outState.putBoolean(BOOL_THRESHOLD_ON, thresholdOn);
        outState.putInt(LONG_PLAYBACK_POSITION, playbackPosition);
    }

    //=================================================
    //  ABSTRACT METHODS IMPLEMENTATIONS AND OVERRIDES
    //=================================================

    @Override protected final View createView(LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_playback_scope, container, false);
        thresholdHandle = view.findViewById(R.id.threshold_handle);
        ibtnThreshold = view.findViewById(R.id.ibtn_threshold);
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
        renderer.setOnDrawListener(new BaseWaveformRenderer.OnDrawListener() {

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                if (getActivity() != null) {
                    viewableTimeSpanUpdateRunnable.setSampleRate(sampleRate);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceHeight(drawSurfaceHeight);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable);
                }
            }
        });
        renderer.setOnScrollListener(new BaseWaveformRenderer.OnScrollListener() {

            @Override public void onScrollStart() {
                startSeek();
            }

            @Override public void onScroll(float dx) {
                if (getActivity() != null) {
                    int progress = (int) (sbAudioProgress.getProgress() - dx);
                    if (progress < 0) progress = 0;
                    if (progress > sbAudioProgress.getMax()) progress = sbAudioProgress.getMax();
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
        renderer.setOnThresholdChangeListener(new WaveformRenderer.OnThresholdChangeListener() {

            @Override public void onThresholdPositionChange(final int position) {
                if (getActivity() != null) {
                    setThresholdHandlePositionRunnable.setPosition(position);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(setThresholdHandlePositionRunnable);
                }
            }

            @Override public void onThresholdValueChange(final float value) {
                if (getActivity() != null) {
                    updateDataProcessorThresholdRunnable.setValue(value);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(updateDataProcessorThresholdRunnable);
                }
            }
        });
        renderer.setSignalAveraging(thresholdOn);
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
     * Returns length of the played audio file in samples.
     */
    protected int getLength() {
        if (getAudioService() != null) return (int) getAudioService().getPlaybackLength();

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
     * Tells audio service to seek to specified {@code position}.
     */
    protected void seek(int position) {
        // let's pause the threshold while seeking
        JniUtils.pauseThreshold();

        if (getAudioService() != null) getAudioService().seekPlayback(position);
    }

    /**
     * Tells audio service that seek should stop.
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
        if (event.getLength() > 0) { // we are starting playback, not resuming
            sbAudioProgress.setMax((int) event.getLength());
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

        sbAudioProgress.setProgress((int) event.getProgress());
        updateProgressTime((int) event.getProgress(), event.getSampleRate());
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
            ViewUtils.playAfterNextLayout(ibtnThreshold, new Func<View, Void>() {
                @Nullable @Override public Void apply(@Nullable View source) {
                    thresholdHandle.setTopOffset(ibtnThreshold.getHeight());
                    return null;
                }
            });
        } else {
            ibtnThreshold.setVisibility(View.INVISIBLE);
        }
        // play/pause button
        setupPlayPauseButton();
        ibtnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toggle(!isPlaying());
            }
        });
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
                    playbackSeekRunnable.setProgress(progress);
                    playbackSeekRunnable.setUpdateProgressSeekBar(false);
                    playbackSeekRunnable.setUpdateProgressTimeLabel(true);
                    seekBar.post(playbackSeekRunnable);
                }
            }
        });
        sbAudioProgress.setProgress(0);
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
            // setup threshold handle
            thresholdHandle.setVisibility(View.VISIBLE);
            thresholdHandle.setOnHandlePositionChangeListener(thresholdChangeListener);
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
            // setup threshold handle
            thresholdHandle.setVisibility(View.INVISIBLE);
            thresholdHandle.setOnHandlePositionChangeListener(null);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.INVISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(null);
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.INVISIBLE);
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

    // Sets the specified value for the threshold.
    void setThresholdHandlePosition(int value) {
        // can be null if callback is called after activity has finished
        if (thresholdHandle != null) thresholdHandle.setPosition(value);
    }

    // Updates data processor with the newly set threshold.
    void updateDataProcessorThreshold(float value) {
        JniUtils.setThreshold((int) value);
        // in case we are pausing just reset the renderer buffers
        getRenderer().resetAveragedSignal();
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
    void updateProgressTime(int progress, int sampleRate) {
        playbackPosition = progress;
        tvProgressTime.setText(WavUtils.formatWavProgress(progress, sampleRate));
    }
}
