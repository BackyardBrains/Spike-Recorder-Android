package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.backyardbrains.audio.TriggerAverager;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.view.BYBThresholdHandle;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsThresholdFragment extends BackyardBrainsPlayLiveScopeFragment {

    private static final String TAG = makeLogTag(BackyardBrainsThresholdFragment.class);

    private BYBThresholdHandle thresholdHandle;

    @Override public void onStart() {
        super.onStart();
        setThresholdGuiVisibility(true);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GUI
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void setViewVisibility(View view, boolean bVisible) {
        if (view != null) {
            view.setVisibility(bVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void setThresholdGuiVisibility(boolean bVisible) {
        LOGD(TAG, "setThresholdGuiVisibility: " + (bVisible ? "TRUE" : "FaLSE"));
        if (getView() != null) {
            setViewVisibility(thresholdHandle.getHandlerView(), bVisible);
            setViewVisibility(getView().findViewById(R.id.triggerViewSampleChangerLayout), bVisible);
            setViewVisibility(getView().findViewById(R.id.samplesSeekBar), bVisible);
        }
    }

    @Override
    // ---------------------------------------------------------------------------------------------
    public void setupButtons(View view) {
        LOGD(TAG, "setupButtons");
        super.setupButtons(view);

        thresholdHandle = new BYBThresholdHandle(context, ((ImageView) view.findViewById(R.id.thresholdHandle)),
            view.findViewById(R.id.thresholdHandleLayout), "OsciloscopeHandle"); // .setOnTouchListener(threshTouch);

        final SeekBar sk = (SeekBar) view.findViewById(R.id.samplesSeekBar);

        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (getView() != null) {
                    TextView tx = ((TextView) getView().findViewById(R.id.numberOfSamplesAveraged));
                    if (tx != null) {
                        tx.setText(progress + "x");
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
        ((TextView) view.findViewById(R.id.numberOfSamplesAveraged)).setText(TriggerAverager.defaultSize + "x");
        sk.setProgress(TriggerAverager.defaultSize);
        //        ((RelativeLayout.LayoutParams)(view.findViewById(R.id.millisecondsViewLayout)).getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.triggerViewSampleChangerLayout);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCAST RECEIVERS CLASS
    private class SetAverageSliderListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("maxSize")) {
                ((SeekBar) getView().findViewById(R.id.samplesSeekBar)).setProgress(intent.getIntExtra("maxSize", 32));
            }
        }
    }

    // ----------------------------------------- RECEIVERS INSTANCES
    private SetAverageSliderListener setAverageSliderListener;

    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerReceiverSetAverageSlider(bRegister);
        thresholdHandle.registerUpdateThresholdHandleListener(bRegister);
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
