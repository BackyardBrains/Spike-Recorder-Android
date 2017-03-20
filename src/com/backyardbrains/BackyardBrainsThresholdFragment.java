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
import com.backyardbrains.audio.TriggerAverager;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.view.BYBThresholdHandle;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsThresholdFragment extends BackyardBrainsPlayLiveScopeFragment {

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

    @Override protected BYBBaseRenderer createRenderer(@NonNull Context context, @NonNull float[] preparedBuffer) {
        return new ThresholdRenderer(context, preparedBuffer);
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

        sbAvgSamplesCount.setProgress(TriggerAverager.defaultSize);
        sbAvgSamplesCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (getView() != null) {
                    if (tvAvgSamplesCount != null) {
                        tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), progress));
                    }
                }
                if (fromUser && getContext() != null) {
                    Intent i = new Intent();
                    i.setAction("BYBThresholdNumAverages");
                    i.putExtra("num", progress);
                    context.sendBroadcast(i);
                }
            }
        });
        tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), TriggerAverager.defaultSize));
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
        if (reg) {
            IntentFilter intentFilter = new IntentFilter("BYBUpdateThresholdHandle");
            updateThresholdHandleListener = new UpdateThresholdHandleListener();
            context.registerReceiver(updateThresholdHandleListener, intentFilter);
        } else {
            context.unregisterReceiver(updateThresholdHandleListener);
            updateThresholdHandleListener = null;
        }
    }

    private void registerReceiverSetAverageSlider(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentSetAverageSliderFilter = new IntentFilter("BYBSetAveragerSlider");
                setAverageSliderListener = new SetAverageSliderListener();
                context.registerReceiver(setAverageSliderListener, intentSetAverageSliderFilter);
            } else {
                context.unregisterReceiver(setAverageSliderListener);
            }
        }
    }
}
