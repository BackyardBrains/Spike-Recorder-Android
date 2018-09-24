package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.events.AmModulationDetectionEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.HeartbeatEvent;
import com.backyardbrains.events.UsbCommunicationEvent;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.view.HeartbeatView;
import com.backyardbrains.view.ThresholdHandle;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ThresholdFragment extends BaseWaveformFragment {

    private static final String TAG = makeLogTag(ThresholdFragment.class);

    @BindView(R.id.threshold_handle) ThresholdHandle thresholdHandle;
    @BindView(R.id.sb_averaged_sample_count) SeekBar sbAvgSamplesCount;
    @BindView(R.id.tv_averaged_sample_count) TextView tvAvgSamplesCount;
    @BindView(R.id.tb_sound) ToggleButton tbSound;
    @BindView(R.id.hv_heartbeat) HeartbeatView vHeartbeat;
    @BindView(R.id.tv_beats_per_minute) TextView tvBeatsPerMinute;

    private static final int AVERAGED_SAMPLE_COUNT = 30;
    private static final double MAX_PROCESSING_TIME = 2.4; // 2.4 seconds

    final SetThresholdHandlePositionRunnable setThresholdHandlePositionRunnable =
        new SetThresholdHandlePositionRunnable();
    final UpdateDataProcessorThresholdRunnable updateDataProcessorThresholdRunnable =
        new UpdateDataProcessorThresholdRunnable();

    private Unbinder unbinder;

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
     * @return A new instance of fragment {@link ThresholdFragment}.
     */
    public static ThresholdFragment newInstance() {
        return new ThresholdFragment();
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onStart() {
        super.onStart();

        startMicAndSetupDataProcessing();
    }

    @Override public void onStop() {
        super.onStop();

        if (getAudioService() != null) {
            getAudioService().stopActiveInputSource();
            getAudioService().setSignalAveraging(false);
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //==============================================
    //  ABSTRACT METHODS IMPLEMENTATIONS
    //==============================================

    @Override protected View createView(LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_threshold, container, false);
        unbinder = ButterKnife.bind(this, view);

        // we should set averaged sample count before UI setup
        JniUtils.setAveragedSampleCount(AVERAGED_SAMPLE_COUNT);

        setupUI();

        return view;
    }

    @Override protected BaseWaveformRenderer createRenderer() {
        final ThresholdRenderer renderer = new ThresholdRenderer(this);
        renderer.setOnDrawListener(new BaseWaveformRenderer.OnDrawListener() {

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                if (getActivity() != null && getAudioService() != null) {
                    viewableTimeSpanUpdateRunnable.setSampleRate(getAudioService().getSampleRate());
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceHeight(drawSurfaceHeight);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable);
                }
            }
        });
        renderer.setOnThresholdChangeListener(new ThresholdRenderer.OnThresholdChangeListener() {

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
        return renderer;
    }

    @Override protected boolean isBackable() {
        return false;
    }

    @Override protected ThresholdRenderer getRenderer() {
        return (ThresholdRenderer) super.getRenderer();
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        LOGD(TAG, "Audio serviced connected. Refresh threshold for initial value");
        if (event.isConnected()) {
            startMicAndSetupDataProcessing();
            refreshThreshold();
            // setup BPM UI
            updateBpmUI();
        }
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbCommunicationEvent(UsbCommunicationEvent event) {
        // update BPM label
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAmModulationDetectionEvent(AmModulationDetectionEvent event) {
        // update BPM UI
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHeartbeatEvent(HeartbeatEvent event) {
        tvBeatsPerMinute.setText(
            String.format(getString(R.string.template_beats_per_minute), event.getBeatsPerMinute()));
        if (event.getBeatsPerMinute() > 0) {
            vHeartbeat.beep();
        } else {
            vHeartbeat.off();
        }
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        // threshold handle
        thresholdHandle.setOnHandlePositionChangeListener(new ThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                getRenderer().adjustThreshold(y);
            }
        });
        // average sample count
        sbAvgSamplesCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // inform interested parties that the average sample count has changed
                JniUtils.setAveragedSampleCount(seekBar.getProgress() > 0 ? seekBar.getProgress() : 1);
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
        });
        sbAvgSamplesCount.setProgress(JniUtils.getAveragedSampleCount());
        // BPM UI
        updateBpmUI();
        if (getContext() != null) tbSound.setChecked(PrefUtils.getBpmSound(getContext()));
        tbSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                vHeartbeat.setMuteSound(!b);
                if (getContext() != null) PrefUtils.setBpmSound(getContext(), b);
            }
        });
    }

    // Updates BpPM UI
    private void updateBpmUI() {
        // update whether BPM processing should be on
        JniUtils.setBpmProcessing(shouldShowBpm());

        tbSound.setVisibility(shouldShowBpm() ? View.VISIBLE : View.INVISIBLE);
        vHeartbeat.setVisibility(shouldShowBpm() ? View.VISIBLE : View.INVISIBLE);
        vHeartbeat.setMuteSound(getContext() != null && !PrefUtils.getBpmSound(getContext()));
        vHeartbeat.off();
        tvBeatsPerMinute.setVisibility(shouldShowBpm() ? View.VISIBLE : View.INVISIBLE);
        tvBeatsPerMinute.setText(String.format(getString(R.string.template_beats_per_minute), 0));
    }

    // Whether BPM label should be visible or not
    private boolean shouldShowBpm() {
        // BPM should be shown if either usb is active input source or we are in AM modulation,
        // and if current filter is default EKG filter
        return getAudioService() != null && (getAudioService().isUsbActiveInput()
            || getAudioService().isAmModulationDetected()) && ObjectUtils.equals(getAudioService().getFilter(),
            Filters.FILTER_HEART);
    }

    // Starts the active input source and sets up data processor
    private void startMicAndSetupDataProcessing() {
        if (getAudioService() != null) {
            getAudioService().startActiveInputSource();

            getAudioService().setSignalAveraging(true);
            //getAudioService().setMaxProcessingTimeInSeconds(MAX_PROCESSING_TIME);
        }
    }

    // Sets the specified value for the threshold.
    void setThresholdHandlePosition(int value) {
        // can be null if callback is called after activity has finished
        if (thresholdHandle != null) thresholdHandle.setPosition(value);
    }

    // Updates data processor with the newly set threshold.
    void updateDataProcessorThreshold(float value) {
        JniUtils.setThreshold((int) value);
    }

    // Refreshes renderer thresholds
    private void refreshThreshold() {
        if (getAudioService() != null) getRenderer().refreshThreshold();
    }
}
