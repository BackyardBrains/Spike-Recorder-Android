package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.audio.ThresholdProcessor;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.utils.BYBConstants;
import com.backyardbrains.view.BYBThresholdHandle;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class BackyardBrainsThresholdFragment extends BaseWaveformFragment {

    private static final String TAG = makeLogTag(BackyardBrainsThresholdFragment.class);

    @BindView(R.id.threshold_handle) BYBThresholdHandle thresholdHandle;
    @BindView(R.id.sb_averaged_sample_count) SeekBar sbAvgSamplesCount;
    @BindView(R.id.tv_averaged_sample_count) TextView tvAvgSamplesCount;

    private static final int DEFAULT_AVERAGED_SAMPLE_COUNT = 30;

    private static final ThresholdProcessor DATA_PROCESSOR = new ThresholdProcessor(DEFAULT_AVERAGED_SAMPLE_COUNT);

    private Unbinder unbinder;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link BackyardBrainsThresholdFragment}.
     */
    public static BackyardBrainsThresholdFragment newInstance() {
        return new BackyardBrainsThresholdFragment();
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
            getAudioService().clearSampleProcessor();
            getAudioService().resetBufferSize();
            getAudioService().stopMicrophone();
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

        setupUI();

        return view;
    }

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        final ThresholdRenderer renderer = new ThresholdRenderer(fragment, preparedBuffer);
        renderer.setCallback(new ThresholdRenderer.CallbackAdapter() {

            @Override public void onThresholdPositionChange(final int position) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setThresholdHandlePosition(position);
                        }
                    });
                }
            }

            @Override public void onThresholdValueChange(final float value) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            updateDataProcessorThreshold(value);
                        }
                    });
                }
            }

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (getAudioService() != null) {
                                setMilliseconds(drawSurfaceWidth / getAudioService().getSampleRate() * 1000 / 2);
                            }

                            setMillivolts(
                                (float) drawSurfaceHeight / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale);
                        }
                    });
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        LOGD(TAG, "Audio serviced connected. Refresh threshold for initial value");
        if (event.isConnected()) {
            startMicAndSetupDataProcessing();
            refreshThreshold();
        }
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        thresholdHandle.setOnHandlePositionChangeListener(new BYBThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                getRenderer().adjustThreshold(y);
            }
        });

        sbAvgSamplesCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
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

                // and inform interested parties that the average sample count has changed
                if (fromUser) DATA_PROCESSOR.setAveragedSampleCount(progress);
            }
        });
        sbAvgSamplesCount.setProgress(DATA_PROCESSOR.getAveragedSampleCount());
    }

    private void startMicAndSetupDataProcessing() {
        if (getAudioService() != null) {
            getAudioService().setSampleProcessor(DATA_PROCESSOR);
            getAudioService().setBufferSize(ThresholdProcessor.SAMPLE_COUNT);
            getAudioService().startMicrophone();
        }
    }

    // Sets the specified value for the threshold.
    private void setThresholdHandlePosition(int value) {
        // can be null if callback is called after activity has finished
        if (thresholdHandle != null) thresholdHandle.setPosition(value);
    }

    // Updates data processor with the newly set threshold.
    private void updateDataProcessorThreshold(float value) {
        // can be null if callback is called after activity has finished
        //noinspection ConstantConditions
        if (DATA_PROCESSOR != null) DATA_PROCESSOR.setThreshold(value);
    }

    // Refreshes renderer thresholds
    private void refreshThreshold() {
        if (getAudioService() != null) getRenderer().refreshThreshold();
    }
}
