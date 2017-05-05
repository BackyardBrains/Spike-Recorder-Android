package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.TriggerAverager;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.ThresholdAverageSampleCountSet;
import com.backyardbrains.view.BYBThresholdHandle;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsThresholdFragment extends BackyardBrainsPlayLiveScopeFragment1 {

    private static final String TAG = makeLogTag(BackyardBrainsThresholdFragment.class);

    @BindView(R.id.threshold_handle) BYBThresholdHandle thresholdHandle;
    @BindView(R.id.triggerViewSampleChangerLayout) LinearLayout llAvgSamplesContainer;
    @BindView(R.id.samplesSeekBar) SeekBar sbAvgSamplesCount;
    @BindView(R.id.numberOfSamplesAveraged) TextView tvAvgSamplesCount;

    private Unbinder unbinder;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        LOGD(TAG, "onCreateView()");
        if (view != null) {
            unbinder = ButterKnife.bind(this, view);
            setupUI();
        }

        return view;
    }

    @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");
    }

    @Override public void onResume() {
        super.onResume();
        LOGD(TAG, "onResume()");
    }

    @Override public void onPause() {
        super.onPause();
        LOGD(TAG, "onPause()");
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        LOGD(TAG, "onDestroyView()");
        unbinder.unbind();
    }

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        return new ThresholdRenderer(fragment, preparedBuffer);
    }

    @Override protected int getLayoutID() {
        return R.layout.thresh_scope_layout;
    }

    @Override protected boolean shouldUseAverager() {
        return true;
    }

    @Override protected ThresholdRenderer getRenderer() {
        return (ThresholdRenderer) super.getRenderer();
    }

    @Override protected boolean canRecord() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GUI
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void setupUI() {
        thresholdHandle.setOnHandlePositionChangeListener(new BYBThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                getRenderer().adjustThreshold(y);
            }
        });

        sbAvgSamplesCount.setProgress(TriggerAverager.DEFAULT_SIZE);
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
                if (fromUser) {
                    EventBus.getDefault().post(new ThresholdAverageSampleCountSet(progress));
                    Intent i = new Intent();
                    i.setAction("BYBThresholdNumAverages");
                    i.putExtra("num", progress);
                    getContext().sendBroadcast(i);
                }
            }
        });
        tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), TriggerAverager.DEFAULT_SIZE));
    }

    //////////////////////////////////////////////////////////////////////////////
    //                            Event Bus
    //////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        super.onAudioServiceConnectionEvent(event);
        if (event.isConnected()) {
            final AudioService provider = getAudioService();
            if (provider != null) getRenderer().refreshThreshold();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCAST RECEIVERS CLASS
    private class SetAverageSliderListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG, "BYBSetAveragerSlider broadcast received!");
            if (intent.hasExtra("maxSize")) sbAvgSamplesCount.setProgress(intent.getIntExtra("maxSize", 32));
        }
    }

    private class UpdateThresholdHandleListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG, "BYBSetAveragerSlider broadcast received! Name: " + intent.getStringExtra("name"));
            if (intent.hasExtra("name") && intent.hasExtra("pos") && "OsciloscopeHandle".equals(
                intent.getStringExtra("name"))) {
                thresholdHandle.setPosition(intent.getIntExtra("pos", 0));
            }
        }
    }

    // ----------------------------------------- RECEIVERS INSTANCES
    private SetAverageSliderListener setAverageSliderListener;
    private UpdateThresholdHandleListener updateThresholdHandleListener;

    // ----------------------------------------- REGISTER RECEIVERS
    @Override public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        LOGD(TAG, "registerReceivers()");
        registerReceiverSetAverageSlider(bRegister);
        registerUpdateThresholdHandleListener(bRegister);
    }

    public void registerUpdateThresholdHandleListener(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBUpdateThresholdHandle");
                updateThresholdHandleListener = new UpdateThresholdHandleListener();
                getContext().registerReceiver(updateThresholdHandleListener, intentFilter);
            } else {
                getContext().unregisterReceiver(updateThresholdHandleListener);
                updateThresholdHandleListener = null;
            }
        }
    }

    private void registerReceiverSetAverageSlider(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentSetAverageSliderFilter = new IntentFilter("BYBSetAveragerSlider");
                setAverageSliderListener = new SetAverageSliderListener();
                getContext().registerReceiver(setAverageSliderListener, intentSetAverageSliderFilter);
            } else {
                getContext().unregisterReceiver(setAverageSliderListener);
            }
        }
    }
}
