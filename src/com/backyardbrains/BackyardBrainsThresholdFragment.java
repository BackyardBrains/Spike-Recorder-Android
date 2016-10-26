package com.backyardbrains;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.TriggerAverager;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.BYBThresholdHandle;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;

public class BackyardBrainsThresholdFragment extends BackyardBrainsPlayLiveScopeFragment {

    private BYBThresholdHandle					thresholdHandle;
    public BackyardBrainsThresholdFragment(){
        super();

        rendererClass = ThresholdRenderer.class;
        layoutID = R.layout.thresh_scope_layout;
        TAG	= "BackyardBrainsThresholdFragment";
    }
    @Override
    public void onStart() {
        super.onStart();
        setThresholdGuiVisibility(true);

//        if(rendererClass!=null){
//            ((ThresholdRenderer)renderer).defaultThresholdValue();
//        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GUI
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void setViewVisibility(View view, boolean bVisible){
        if (view != null) {
            view.setVisibility(bVisible?View.VISIBLE:View.GONE);
        }
    }
    private void setThresholdGuiVisibility(boolean bVisible) {
        Log.d(TAG, "setThresholdGuiVisibility: "+ (bVisible?"TRUE":"FaLSE"));
        if (getView() != null) {
            setViewVisibility(thresholdHandle.getHandlerView(), bVisible);
//            ImageView b = thresholdHandle.getHandlerView();
//            if (b != null) {
//                if (bVisible) {
//                    b.setVisibility(View.VISIBLE);
//                } else {
//                    b.setVisibility(View.GONE);
//                }
//            }
            setViewVisibility( getView().findViewById(R.id.triggerViewSampleChangerLayout),bVisible);
//            LinearLayout ll = (LinearLayout) getView().findViewById(R.id.triggerViewSampleChangerLayout);
//            if (ll != null) {
//                if (bVisible) {
//                    ll.setVisibility(View.VISIBLE);
//                } else {
//                    ll.setVisibility(View.GONE);
//                }
//            }
            setViewVisibility(getView().findViewById(R.id.samplesSeekBar), bVisible);
//            SeekBar sk = (SeekBar) getView().findViewById(R.id.samplesSeekBar);
//            if (sk != null) {
//                if (bVisible) {
//                    sk.setVisibility(View.VISIBLE);
//                } else {
//                    sk.setVisibility(View.GONE);
//                }
//            }
        }
    }

    @Override
    // ---------------------------------------------------------------------------------------------
    public void setupButtons(View view) {
        Log.d(TAG,"setupButtons");
        super.setupButtons(view);

        thresholdHandle = new BYBThresholdHandle(context, ((ImageView) view.findViewById(R.id.thresholdHandle)),  view.findViewById(R.id.thresholdHandleLayout),"OsciloscopeHandle"); // .setOnTouchListener(threshTouch);

        final SeekBar sk = (SeekBar) view.findViewById(R.id.samplesSeekBar);

        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("maxSize")) {
                ((SeekBar) getView().findViewById(R.id.samplesSeekBar)).setProgress(intent.getIntExtra("maxSize", 32));
            }
        }
    }
    // ----------------------------------------- RECEIVERS INSTANCES
    private SetAverageSliderListener			setAverageSliderListener;
    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerReceiverSetAverageSlider(bRegister);
        thresholdHandle.registerUpdateThresholdHandleListener(bRegister);
    }
    private void registerReceiverSetAverageSlider(boolean reg) {
        if(getContext() != null) {
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
