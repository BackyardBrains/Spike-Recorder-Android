package com.backyardbrains;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.TriggerAverager;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
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

public class BackyardBrainsOscilloscopeFragment extends BackyardBrainsPlayLiveScopeFragment{
    private boolean								isRecording		= false;
    // ----------------------------------------------------------------------------------------
    public BackyardBrainsOscilloscopeFragment(){
        super();
        TAG				= "BackyardBrainsOscilloscopeFragment";
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onStart() {
        super.onStart();
        enableUiForActivity();
    }
    @Override
    public void onPause() {
        super.onPause();
        hideRecordingButtons();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    public void hideRecordingButtons() {
        ImageButton mRecordButton = (ImageButton) getView().findViewById(R.id.recordButton);
        if (mRecordButton != null) {
            mRecordButton.setVisibility(View.GONE);
        }
    }
    // ----------------------------------------------------------------------------------------
    public void showRecordingButtons() {
        ImageButton mRecordButton = (ImageButton) getView().findViewById(R.id.recordButton);
        if (mRecordButton != null) {
            mRecordButton.setVisibility(View.VISIBLE);
            mode = LIVE_MODE;
        }
        showPlayButton(false);
        showPauseButton(false);
    }
    // ----------------------------------------------------------------------------------------
    protected void enableUiForActivity() {
        if (mode == LIVE_MODE) {
            showRecordingButtons();
        } else {
            showCloseButton();
        }
    }
    // ----------------------------------------------------------------------------------------
    public void toggleRecording() {
        ShowRecordingAnimation anim = new ShowRecordingAnimation(getActivity(), isRecording);
        try {
            View tapToStopRecView = getView().findViewById(R.id.TapToStopRecordingTextView);
            anim.run();
            Intent i = new Intent();
            i.setAction("BYBToggleRecording");
            if(getContext()!= null) {
                context.sendBroadcast(i);
            }
            if (isRecording == false) {
                tapToStopRecView.setVisibility(View.VISIBLE);
            } else {
                tapToStopRecView.setVisibility(View.GONE);
            }
        } catch (RuntimeException e) {
            if(getContext()!= null){
                Toast.makeText(context, "No SD Card is available. Recording is disabled", Toast.LENGTH_LONG).show();
            }
        }
        isRecording = !isRecording;
    }
    // ---------------------------------------------------------------------------------------------
    public void setupButtons(View view) {
        super.setupButtons(view);
        OnClickListener recordingToggle = new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        };
        ImageButton mRecordButton = (ImageButton) view.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(recordingToggle);
        // ------------------------------------
        View tapToStopRecView = view.findViewById(R.id.TapToStopRecordingTextView);
        tapToStopRecView.setOnClickListener(recordingToggle);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI ANIMATIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private class ShowRecordingAnimation implements Runnable {

        private Activity	activity;
        private boolean		recording;

        public ShowRecordingAnimation(Activity a, Boolean b) {
            this.activity = a;
            this.recording = b;
        }

        @Override
        public void run() {
            Animation a = null;
            if (this.recording == false) {
                a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
            } else {
                a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
            }
            a.setDuration(250);
            a.setInterpolator(AnimationUtils.loadInterpolator(this.activity, android.R.anim.anticipate_overshoot_interpolator));
            View stopRecView = activity.findViewById(R.id.TapToStopRecordingTextView);
            stopRecView.startAnimation(a);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            boolean yesno = intent.getBooleanExtra("showRecordingButton", true);
            if (yesno) {
                showRecordingButtons();
            } else {
                hideRecordingButtons();
            }
        }
    }
    // ----------------------------------------- RECEIVERS INSTANCES
    private ShowRecordingButtonsReceiver		showRecordingButtonsReceiver;
// ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerReceiverShowRecordingButtons(bRegister);
    }
    private void registerReceiverShowRecordingButtons(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilterRecordingButtons = new IntentFilter("BYBShowRecordingButtons");
                showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
                context.registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);
            } else {
                context.unregisterReceiver(showRecordingButtonsReceiver);
            }
        }
    }
}
