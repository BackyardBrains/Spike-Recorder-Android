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
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.ThresholdHelper;
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

        if (getAudioService() != null) getAudioService().startMicrophone();
    }

    @Override public void onStop() {
        super.onStop();

        if (getAudioService() != null) getAudioService().stopMicrophone();
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
        final View view = inflater.inflate(R.layout.fragmnet_threshold, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        final ThresholdRenderer renderer = new ThresholdRenderer(fragment, preparedBuffer);
        renderer.setCallback(new ThresholdRenderer.CallbackAdapter() {

            @Override public void onThresholdUpdate(final int value) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setThreshold(value);
                        }
                    });
                }
            }

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            final float millisecondsInThisWindow = drawSurfaceWidth / 44100.0f * 1000 / 2;
                            setMilliseconds(millisecondsInThisWindow);

                            float yPerDiv =
                                (float) drawSurfaceHeight / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale;
                            setMillivolts(yPerDiv);
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

    @Override protected boolean shouldUseAverager() {
        return true;
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
        if (event.isConnected()) refreshThreshold();
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

        sbAvgSamplesCount.setProgress(ThresholdHelper.DEFAULT_SIZE);
        sbAvgSamplesCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // update count label
                if (tvAvgSamplesCount != null) {
                    tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), progress));
                }
                // and inform interested parties that the average sample count has changed
                if (fromUser && getAudioService() != null) getAudioService().setThresholdAveragedSampleCount(progress);
            }
        });
        tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), ThresholdHelper.DEFAULT_SIZE));
    }

    // Sets the specified value for the threshold
    private void setThreshold(int value) {
        thresholdHandle.setPosition(value);
    }

    // Refreshes renderer thresholds
    private void refreshThreshold() {
        final AudioService provider = getAudioService();
        if (provider != null) getRenderer().refreshThreshold();
    }
}
