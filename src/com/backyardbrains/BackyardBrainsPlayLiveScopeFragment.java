package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.backyardbrains.drawing.WaveformRenderer;

public class BackyardBrainsPlayLiveScopeFragment extends BackyardBrainsBaseScopeFragment {
    // ----------------------------------------------------------------------------------------
    public BackyardBrainsPlayLiveScopeFragment(){
        super();
        mode = LIVE_MODE;
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
        if(mode == PLAYBACK_MODE){
            showCloseButton();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void showCloseButton() {
        ImageButton mCloseButton = (ImageButton) getView().findViewById(R.id.closeButton);
        if (mCloseButton != null) {
            mCloseButton.setVisibility(View.VISIBLE);
            mode = PLAYBACK_MODE;
        }
        showPauseButton(true);
        showPlayButton(false);
    }
    // ----------------------------------------------------------------------------------------
    public void hideCloseButton() {
        ImageButton mCloseButton = (ImageButton) getView().findViewById(R.id.closeButton);
        if (mCloseButton != null) {
            if (mCloseButton.getVisibility() == View.VISIBLE) {
                mCloseButton.setVisibility(View.GONE);

            }
        }
    }
    // ----------------------------------------------------------------------------------------
    public void showPauseButton(boolean bShow) {
        ImageButton mButton = (ImageButton) getView().findViewById(R.id.pauseButton);
        if (mButton != null) {
            if (bShow) {
                mButton.setVisibility(View.VISIBLE);
            } else {
                if (mButton.getVisibility() == View.VISIBLE) {
                    mButton.setVisibility(View.GONE);
                }
            }
        }
    }
    // ----------------------------------------------------------------------------------------
    public void showPlayButton(boolean bShow) {
        ImageButton mButton = (ImageButton) getView().findViewById(R.id.playButton);
        if (mButton != null) {
            if (bShow) {
                mButton.setVisibility(View.VISIBLE);
            } else {
                if (mButton.getVisibility() == View.VISIBLE) {
                    mButton.setVisibility(View.GONE);
                }
            }
        }
    }
    // ---------------------------------------------------------------------------------------------
    public void setupButtons(View view) {
        View.OnClickListener closeButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ImageButton) v.findViewById(R.id.closeButton)).setVisibility(View.GONE);
//                showRecordingButtons();
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
                showPauseButton(true);
                showPlayButton(false);
                if(getContext() != null) {
                    Intent i = new Intent();
                    i.setAction("BYBTogglePlayback");
                    i.putExtra("play", true);
                    context.sendBroadcast(i);
                }
                // //Log.d("UIFactory", "Close Button Pressed!");
            }
        };
        ImageButton mPlayButton = (ImageButton) view.findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playButtonListener);
        mPlayButton.setVisibility(View.GONE);
        // ------------------------------------
        View.OnClickListener pauseButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPauseButton(false);
                showPlayButton(true);
                if(getContext() != null) {
                    Intent i = new Intent();
                    i.setAction("BYBTogglePlayback");
                    i.putExtra("play", false);
                    context.sendBroadcast(i);
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
            showCloseButton();
        }
    }
    // ----------------------------------------- RECEIVERS INSTANCES
    private AudioPlaybackStartListener			audioPlaybackStartListener;
    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerAudioPlaybackStartReceiver(bRegister);
    }
    private void registerAudioPlaybackStartReceiver(boolean reg) {
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

}
