package com.backyardbrains;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.BYBSlidingButton;

public class BackyardBrainsPlayLiveScopeFragment extends BackyardBrainsBaseScopeFragment {
    BYBSlidingButton stopRecButton;
    // ----------------------------------------------------------------------------------------
    public BackyardBrainsPlayLiveScopeFragment(){
        super();
        rendererClass = WaveformRenderer.class;
        layoutID = R.layout.play_live_rec_scope_layout;
        TAG	= "BackyardBrainsPlayLiveScopeFragment";

    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container,savedInstanceState);
        setupButtons(rootView);
        return rootView;
    }
    @Override
    public void onStart() {
        super.onStart();
        showUIForMode();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- OTHER METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public AudioService getAudioService(){
        if(getActivity()!=null) {
            BackyardBrainsApplication app = ((BackyardBrainsApplication) getActivity().getApplicationContext());
            if (app != null) {
                return app.mAudioService;
            }
        }
        return null;
    }
    public boolean getIsRecording(){
        if(getAudioService() != null){
            return getAudioService().isRecording();
        }
        return false;
    }
    public boolean getIsPlaybackMode(){
        if(getAudioService() != null){
            return getAudioService().isPlaybackMode();
        }
        return false;
    }
    public boolean getIsPlaying(){
        if(getAudioService() != null){
            return getAudioService().isAudioPlayerPlaying();
        }
        return false;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void showUIForMode(){
        boolean bPlaybackMode = getIsPlaybackMode();
            showPlaybackButtons(bPlaybackMode);
            showRecordingButtons(!bPlaybackMode);
    }
    public void showCloseButton(boolean bShow) {
        showButton(getView().findViewById(R.id.closeButton),bShow);
    }
    // ----------------------------------------------------------------------------------------
    public void showPlaybackButtons(boolean bShow){
        boolean bIsPlaying =getIsPlaying();
            showPauseButton(bIsPlaying && bShow);
            showPlayButton(!bIsPlaying && bShow);
            showCloseButton(bShow);
    }
    // ----------------------------------------------------------------------------------------
    public void showPauseButton(boolean bShow) {
        showButton(getView().findViewById(R.id.pauseButton),bShow);
    }
    // ----------------------------------------------------------------------------------------
    public void showPlayButton(boolean bShow) {
        showButton(getView().findViewById(R.id.playButton), bShow);
    }
    // ---------------------------------------------------------------------------------------------
    public void showButton(View view, boolean bShow){
        if (view != null) {
            if (bShow) {
                view.setVisibility(View.VISIBLE);
            } else {
                if (view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }
    // ---------------------------------------------------------------------------------------------
    public void showRecButton(boolean bShow){
        showButton(getView().findViewById(R.id.recordButton), bShow);
    }
    // ----------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    public void showRecordingButtons(boolean bShow) {
        boolean bIsRecording = getIsRecording();
        showRecButton(bShow && !bIsRecording);
        showRecBar(bShow && bIsRecording);
//        toggleRecording();
    }
    // ---------------------------------------------------------------------------------------------
    public void showRecBar(boolean bShow) {
        if(stopRecButton != null) {
            stopRecButton.show(bShow && getIsRecording());
        }
//        ShowRecordingAnimation anim = new ShowRecordingAnimation(getActivity(), bShow && bIsRecording);
//        try {
//
//            View tapToStopRecView = getView().findViewById(R.id.TapToStopRecordingTextView);
//            if(bShow && bIsRecording) {
//                tapToStopRecView.setVisibility(View.VISIBLE );//: View.GONE);
//            }
//            anim.run();
//
//            if(!bShow || !bIsRecording) {
//                tapToStopRecView.setVisibility(View.GONE);//: View.GONE);
//            }
//
//        } catch (RuntimeException e) {
//            if(getContext()!= null){
//                Toast.makeText(context, "No SD Card is available. Recording is disabled", Toast.LENGTH_LONG).show();
//            }
//        }
    }
    // ---------------------------------------------------------------------------------------------
    public void setupButtons(View view) {
        ImageButton mRecordButton = (ImageButton) view.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getAudioService()!=null){
                    getAudioService().startRecording();;
                }
            }
        });
        // ------------------------------------
        View tapToStopRecView = view.findViewById(R.id.TapToStopRecordingTextView);
        tapToStopRecView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getAudioService()!=null){
                    getAudioService().stopRecording();
                }
            }
        });
        stopRecButton = new BYBSlidingButton(tapToStopRecView, getContext());
        // ------------------------------------
        View.OnClickListener closeButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ImageButton) v.findViewById(R.id.closeButton)).setVisibility(View.GONE);
                if(getContext() != null) {
                    Intent i = new Intent();
                    i.setAction("BYBCloseButton");
                    context.sendBroadcast(i);
                }
                // //Log.d("UIFactory", "Close Button Pressed!");
            }
        };
        ImageButton mCloseButton = (ImageButton) view.findViewById(R.id.closeButton);
        mCloseButton.setOnClickListener(closeButtonListener);
        mCloseButton.setVisibility(View.GONE);
        // ------------------------------------
        View.OnClickListener playButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getAudioService()!=null){
                    getAudioService().togglePlayback(true);
                }
//                if(getContext() != null) {
//                    Intent i = new Intent();
//                    i.setAction("BYBTogglePlayback");
//                    i.putExtra("play", true);
//                    context.sendBroadcast(i);
//                }
            }
        };
        ImageButton mPlayButton = (ImageButton) view.findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playButtonListener);
        mPlayButton.setVisibility(View.GONE);
        // ------------------------------------
        View.OnClickListener pauseButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getAudioService()!=null){
                    getAudioService().togglePlayback(false);
                }
//                if(getContext() != null) {
//                    Intent i = new Intent();
//                    i.setAction("BYBTogglePlayback");
//                    i.putExtra("play", false);
//                    context.sendBroadcast(i);
//                }
            }
        };
        ImageButton mPauseButton = (ImageButton) view.findViewById(R.id.pauseButton);
        mPauseButton.setOnClickListener(pauseButtonListener);
        mPauseButton.setVisibility(View.GONE);
        // ------------------------------------
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI ANIMATIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////
//    private class ShowRecordingAnimation implements Runnable {
//
//        private Activity activity;
//        private boolean		recording;
//
//        public ShowRecordingAnimation(Activity a, Boolean b) {
//            this.activity = a;
//            this.recording = b;
//        }
//
//        @Override
//        public void run() {
//            Animation a = null;
//            if (this.recording == false) {
//                a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
//            } else {
//                a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
//            }
//            a.setDuration(250);
//            a.setInterpolator(AnimationUtils.loadInterpolator(this.activity, android.R.anim.anticipate_overshoot_interpolator));
//            View stopRecView = activity.findViewById(R.id.TapToStopRecordingTextView);
//            stopRecView.startAnimation(a);
//        }
//    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class AudioPlaybackStartListener extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.w(TAG, "AudioPlaybackStartListener");
//            showCloseButton();
            showUIForMode();
        }
    }
    private class UpdateUIListener extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.w(TAG, "UpdateUIListener");
            showUIForMode();
        }
    }
    /*private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            showRecordingButtons(intent.getBooleanExtra("showRecordingButton", true));
        }
    }*/
    // ----------------------------------------- RECEIVERS INSTANCES
//    private ShowRecordingButtonsReceiver		showRecordingButtonsReceiver;
    private AudioPlaybackStartListener			audioPlaybackStartListener;
    private UpdateUIListener                    updateUIListener;
    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerAudioPlaybackStartReceiver(bRegister);
        registerUpdateUIReceiver(bRegister);
//        registerReceiverShowRecordingButtons(bRegister);
    }
    private void registerAudioPlaybackStartReceiver(boolean reg) {
        Log.w(TAG, "registerAudioPlaybackStartReceiver: " + (reg?"true":"false"));
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBAudioPlaybackStart");
                audioPlaybackStartListener = new AudioPlaybackStartListener();
                context.registerReceiver(audioPlaybackStartListener, intentFilter);
            } else {
                context.unregisterReceiver(audioPlaybackStartListener);
            }
        }
    }
    private void registerUpdateUIReceiver(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBUpdateUI");
                updateUIListener = new UpdateUIListener();
                context.registerReceiver(updateUIListener, intentFilter);
            } else {
                context.unregisterReceiver(updateUIListener);
            }
        }
    }

  /*  private void registerReceiverShowRecordingButtons(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilterRecordingButtons = new IntentFilter("BYBShowRecordingButtons");
                showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
                context.registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);
            } else {
                context.unregisterReceiver(showRecordingButtonsReceiver);
            }
        }
    }*/
}
