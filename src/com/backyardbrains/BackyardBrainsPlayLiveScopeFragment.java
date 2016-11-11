package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.BYBSlidingView;

public class BackyardBrainsPlayLiveScopeFragment extends BackyardBrainsBaseScopeFragment {
    BYBSlidingView stopRecButton;

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
        ((BackyardBrainsMain)getActivity()).showButtons(true);
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
    public boolean getIsRecording(){
        if(getAudioService() != null){
            return getAudioService().isRecording();
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
    }
    // ---------------------------------------------------------------------------------------------
    public void showRecBar(boolean bShow) {
        if(stopRecButton != null) {
            stopRecButton.show(bShow && getIsRecording());
        }
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
        stopRecButton = new BYBSlidingView(tapToStopRecView, getContext(), "tapToStopRec");
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
            }
        };
        ImageButton mPauseButton = (ImageButton) view.findViewById(R.id.pauseButton);
        mPauseButton.setOnClickListener(pauseButtonListener);
        mPauseButton.setVisibility(View.GONE);
        // ------------------------------------
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class AudioPlaybackStartListener extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.w(TAG, "AudioPlaybackStartListener");
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
    // ----------------------------------------- RECEIVERS INSTANCES
    private AudioPlaybackStartListener			audioPlaybackStartListener;
    private UpdateUIListener                    updateUIListener;
    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerAudioPlaybackStartReceiver(bRegister);
        registerUpdateUIReceiver(bRegister);
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

}
