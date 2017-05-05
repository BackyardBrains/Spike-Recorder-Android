package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.SeekableWaveformRenderer;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.utls.WavUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsPlayLiveScopeFragment extends BaseWaveformFragment {

    private static final String TAG = makeLogTag(BackyardBrainsPlayLiveScopeFragment.class);

    @BindView(R.id.iv_play_pause) ImageView ibtnPlayPause;
    @BindView(R.id.sb_audio_progress) SeekBar sbAudioProgress;
    @BindView(R.id.tv_time) TextView tvProgressTime;

    private Unbinder unbinder;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link BackyardBrainsPlayLiveScopeFragment}.
     */
    public static BackyardBrainsPlayLiveScopeFragment newInstance() {
        return new BackyardBrainsPlayLiveScopeFragment();
    }

    //=================================================
    //  LIFECYCLE IMPLEMENTATIONS
    //=================================================

    @Override public void onStop() {
        super.onStop();
        stopPlaying();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //=================================================
    //  ABSTRACT METHODS IMPLEMENTATIONS AND OVERRIDES
    //=================================================

    @Override protected int getLayoutRes() {
        return R.layout.fragment_play_live_scope;
    }

    @Override
    protected void initView(@NonNull View view, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        unbinder = ButterKnife.bind(this, view);

        setupUI();
    }

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        final SeekableWaveformRenderer renderer = new SeekableWaveformRenderer(fragment, preparedBuffer);
        renderer.setCallback(new BYBBaseRenderer.Callback() {
            @Override public void onTimeChange(final float milliseconds) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setMilliseconds(milliseconds);
                        }
                    });
                }
            }

            @Override public void onSignalChange(final float millivolts) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setMillivolts(millivolts);
                        }
                    });
                }
            }

            @Override public void onHorizontalDragStart() {
                LOGD(TAG, "Start horizontal drag");
                startSeek();
            }

            @Override public void onHorizontalDrag(float dx) {
                int progress = (int) (sbAudioProgress.getProgress() - dx);
                if (progress < 0) progress = 0;
                if (progress > sbAudioProgress.getMax()) progress = sbAudioProgress.getMax();
                seek(progress);
                sbAudioProgress.setProgress(progress);
                updateProgressTime(progress);
                LOGD(TAG, "Seeking: " + progress);
            }

            @Override public void onHorizontalDragEnd() {
                LOGD(TAG, "End horizontal drag");
                stopSeek();
            }
        });
        return renderer;
    }

    @Override protected SeekableWaveformRenderer getRenderer() {
        return (SeekableWaveformRenderer) super.getRenderer();
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

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

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPlaybackStartedEvent(AudioPlaybackStartedEvent event) {
        LOGD(TAG, "Start audio playback - " + event.getLength());
        if (event.getLength() > 0) sbAudioProgress.setMax((int) event.getLength());

        setupPlayPauseButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPlaybackProgressEvent(AudioPlaybackProgressEvent event) {
        // can be 0 if AudioPlaybackStartedEvent event was sent before onStart()
        if (sbAudioProgress.getMax() == 0) sbAudioProgress.setMax(getLength());

        sbAudioProgress.setProgress((int) event.getProgress());
        updateProgressTime((int) event.getProgress());
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
        // play/pause button
        ibtnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toggle(!isPlaying());
            }
        });
        setupPlayPauseButton();
        // audio progress
        sbAudioProgress.setMax(getLength());
        sbAudioProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                LOGD(TAG, "SeekBar.onStartTrackingTouch()");
                startSeek();
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                LOGD(TAG, "SeekBar.onStopTrackingTouch()");
                stopSeek();
            }

            @Override public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (fromUser) {
                    seekBar.post(new Runnable() {
                        @Override public void run() {
                            seek(progress);
                            updateProgressTime(progress);
                        }
                    });
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
    private void updateProgressTime(int progress) {
        tvProgressTime.setText(WavUtils.formatWavProgress(progress));
    }
}
