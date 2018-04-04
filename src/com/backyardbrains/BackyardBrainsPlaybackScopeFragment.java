package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.SeekableWaveformRenderer;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Formats;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class BackyardBrainsPlaybackScopeFragment extends BaseWaveformFragment {

    private static final String TAG = makeLogTag(BackyardBrainsPlaybackScopeFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";
    private static final String INT_SAMPLE_RATE = "bb_sample_rate";

    // Maximum time that should be processed in any given moment (in seconds)
    private static final double MAX_PROCESSING_TIME = 6; // 6 seconds

    // Runnable used for updating viewable time span number
    final protected ViewableTimeSpanUpdateRunnable viewableTimeSpanUpdateRunnable =
        new ViewableTimeSpanUpdateRunnable();
    final protected PlaybackSeekRunnable playbackSeekRunnable = new PlaybackSeekRunnable();
    final protected RMSUpdateRunnable rmsUpdateRunnable = new RMSUpdateRunnable();

    protected TextView tvRms;
    protected TextView tvSpikeCount0;
    protected TextView tvSpikeCount1;
    protected TextView tvSpikeCount2;
    protected ImageView ibtnPlayPause;
    protected SeekBar sbAudioProgress;
    protected TextView tvProgressTime;
    protected TextView tvRmsTime;

    protected String filePath;

    // Sample rate that should be used for audio playback
    int sampleRate;

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
    protected class RMSUpdateRunnable implements Runnable {

        private float rms;
        private int sampleCount;

        @Override public void run() {
            tvRms.setText(String.format(getString(R.string.template_rms), rms));
            tvRmsTime.setText(Formats.formatTime_s_msec(sampleCount / (float) sampleRate * 1000));
        }

        public void setRms(float rms) {
            this.rms = rms;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }
    }

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link BackyardBrainsPlaybackScopeFragment}.
     */
    public static BackyardBrainsPlaybackScopeFragment newInstance(@Nullable String filePath) {
        final BackyardBrainsPlaybackScopeFragment fragment = new BackyardBrainsPlaybackScopeFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    //=================================================
    //  LIFECYCLE IMPLEMENTATIONS
    //=================================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) filePath = getArguments().getString(ARG_FILE_PATH);
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) sampleRate = savedInstanceState.getInt(INT_SAMPLE_RATE, AudioUtils.SAMPLE_RATE);
        seek(sbAudioProgress.getProgress());
    }

    @Override public void onStart() {
        super.onStart();

        if (getContext() != null && ApacheCommonsLang3Utils.isBlank(filePath)) {
            ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
            return;
        }

        // we should set max processing time to 6 seconds
        if (getAudioService() != null) getAudioService().setMaxProcessingTimeInSeconds(MAX_PROCESSING_TIME);
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
    }

    //=================================================
    //  ABSTRACT METHODS IMPLEMENTATIONS AND OVERRIDES
    //=================================================

    @Override protected final View createView(LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_playback_scope, container, false);
        tvRms = view.findViewById(R.id.tv_rms);
        tvSpikeCount0 = view.findViewById(R.id.tv_spike_count0);
        tvSpikeCount1 = view.findViewById(R.id.tv_spike_count1);
        tvSpikeCount2 = view.findViewById(R.id.tv_spike_count2);
        ibtnPlayPause = view.findViewById(R.id.iv_play_pause);
        sbAudioProgress = view.findViewById(R.id.sb_audio_progress);
        tvProgressTime = view.findViewById(R.id.tv_progress_time);
        tvRmsTime = view.findViewById(R.id.tv_rms_time);

        setupUI();

        // subclass content
        final FrameLayout flContent = view.findViewById(R.id.playback_scope_content_container);
        final View content = createPlaybackView(inflater, flContent);
        if (content != null) flContent.addView(content);

        return view;
    }

    @Override protected BYBBaseRenderer createRenderer(@NonNull float[] preparedBuffer) {
        final SeekableWaveformRenderer renderer = new SeekableWaveformRenderer(filePath, this, preparedBuffer);
        renderer.setCallback(new SeekableWaveformRenderer.Callback() {

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    viewableTimeSpanUpdateRunnable.setSampleRate(sampleRate);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceHeight(drawSurfaceHeight);
                    getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable);
                }
            }

            @Override public void onHorizontalDragStart() {
                startSeek();
            }

            @Override public void onHorizontalDrag(float dx) {
                int progress = (int) (sbAudioProgress.getProgress() - dx);
                if (progress < 0) progress = 0;
                if (progress > sbAudioProgress.getMax()) progress = sbAudioProgress.getMax();
                playbackSeekRunnable.setProgress(progress);
                playbackSeekRunnable.setUpdateProgressSeekBar(true);
                playbackSeekRunnable.setUpdateProgressTimeLabel(true);
                sbAudioProgress.post(playbackSeekRunnable);
            }

            @Override public void onHorizontalDragEnd() {
                stopSeek();
            }

            @Override public void onMeasurementStart() {
                tvRms.setVisibility(View.VISIBLE);
                tvRmsTime.setVisibility(View.VISIBLE);
            }

            @Override public void onMeasure(float rms, int rmsSampleCount) {
                if (getActivity() != null) {
                    rmsUpdateRunnable.setRms(rms);
                    rmsUpdateRunnable.setSampleCount(rmsSampleCount);
                    tvRms.post(rmsUpdateRunnable);
                }
            }

            @Override public void onMeasurementEnd() {
                tvRms.setVisibility(View.INVISIBLE);
                tvRmsTime.setVisibility(View.INVISIBLE);
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
        if (getAudioService() != null) getAudioService().startPlayback(filePath, autoPlay);
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
        // this will start playback if we are coming from background
        startPlaying(false);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPlaybackStartedEvent(AudioPlaybackStartedEvent event) {
        LOGD(TAG, "Start audio playback - " + event.getLength());
        sampleRate = event.getSampleRate();
        if (event.getLength() > 0) {
            sbAudioProgress.setMax((int) event.getLength());
            EventBus.getDefault().removeStickyEvent(AudioPlaybackStartedEvent.class);
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

        if (event.isCompleted()) sbAudioProgress.setProgress(0);
        setupPlayPauseButton();
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI() {
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

    // Sets appropriate image on play/pause button
    private void setupPlayPauseButton() {
        LOGD(TAG, "setupPlayPauseButton() - isPlaying=" + isPlaying());
        ibtnPlayPause.setImageResource(
            isPlaying() ? R.drawable.ic_pause_circle_filled_orange_24dp : R.drawable.ic_play_circle_filled_orange_24dp);
    }

    // Updates progress time according to progress
    void updateProgressTime(int progress, int sampleRate) {
        tvProgressTime.setText(WavUtils.formatWavProgress(progress, sampleRate));
    }
}
