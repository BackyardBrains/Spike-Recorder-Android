package com.backyardbrains;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.BYBSlidingView;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utls.LogUtils.LOGW;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsPlayLiveScopeFragment extends BackyardBrainsBaseScopeFragment
    implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(BackyardBrainsPlayLiveScopeFragment.class);

    BYBSlidingView stopRecButton;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;
    private static final int BYB_SETTINGS_SCREEN = 121;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        setupButtons(rootView);
        ((BackyardBrainsMain) getActivity()).showButtons(true);
        return rootView;
    }

    @Override public void onStart() {
        super.onStart();
        showUIForMode();
    }

    @Override protected BYBBaseRenderer createRenderer(@NonNull Context context, @NonNull float[] preparedBuffer) {
        return new WaveformRenderer(getContext(), preparedBuffer);
    }

    @Override protected int getLayoutID() {
        return R.layout.play_live_rec_scope_layout;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- OTHER METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean getIsRecording() {
        if (getAudioService() != null) {
            return getAudioService().isRecording();
        }
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Permission Request >= API 23
    //////////////////////////////////////////////////////////////////////////////
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(BYB_WRITE_EXTERNAL_STORAGE_PERM) private void startRecording() {
        if (EasyPermissions.hasPermissions(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (getAudioService() != null) {
                getAudioService().startRecording();
            }
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_write_external_storage),
                BYB_WRITE_EXTERNAL_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override public void onPermissionsGranted(int requestCode, List<String> perms) {
        //LOGD(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override public void onPermissionsDenied(int requestCode, List<String> perms) {
        //LOGD(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again)).setTitle(
                getString(R.string.title_settings_dialog))
                .setPositiveButton(getString(R.string.setting))
                .setNegativeButton(getString(R.string.cancel), null /* click listener */)
                .setRequestCode(BYB_SETTINGS_SCREEN)
                .build()
                .show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void showUIForMode() {
        boolean bPlaybackMode = getIsPlaybackMode();
        showPlaybackButtons(bPlaybackMode);
        showRecordingButtons(!bPlaybackMode);
    }

    public void showCloseButton(boolean bShow) {
        showButton(getView().findViewById(R.id.closeButton), bShow);
    }

    // ----------------------------------------------------------------------------------------
    public void showPlaybackButtons(boolean bShow) {
        boolean bIsPlaying = getIsPlaying();
        showPauseButton(bIsPlaying && bShow);
        showPlayButton(!bIsPlaying && bShow);
        showCloseButton(bShow);
    }

    // ----------------------------------------------------------------------------------------
    public void showPauseButton(boolean bShow) {
        showButton(getView().findViewById(R.id.pauseButton), bShow);
    }

    // ----------------------------------------------------------------------------------------
    public void showPlayButton(boolean bShow) {
        showButton(getView().findViewById(R.id.playButton), bShow);
    }

    // ---------------------------------------------------------------------------------------------
    public void showButton(View view, boolean bShow) {
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
    public void showRecButton(boolean bShow) {
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
        if (stopRecButton != null) {
            stopRecButton.show(bShow && getIsRecording());
        }
    }

    // ---------------------------------------------------------------------------------------------
    public void setupButtons(View view) {
        ImageButton mRecordButton = (ImageButton) view.findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startRecording();
            }
        });
        // ------------------------------------
        View tapToStopRecView = view.findViewById(R.id.TapToStopRecordingTextView);
        tapToStopRecView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) {
                    getAudioService().stopRecording();
                }
            }
        });
        stopRecButton = new BYBSlidingView(tapToStopRecView, getContext(), "tapToStopRec");
        // ------------------------------------
        View.OnClickListener closeButtonListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                ((ImageButton) v.findViewById(R.id.closeButton)).setVisibility(View.GONE);
                if (getContext() != null) {
                    Intent i = new Intent();
                    i.setAction("BYBCloseButton");
                    context.sendBroadcast(i);
                }
                // //LOGD("UIFactory", "Close Button Pressed!");
            }
        };
        ImageButton mCloseButton = (ImageButton) view.findViewById(R.id.closeButton);
        mCloseButton.setOnClickListener(closeButtonListener);
        mCloseButton.setVisibility(View.GONE);
        // ------------------------------------
        View.OnClickListener playButtonListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) {
                    getAudioService().togglePlayback(true);
                }
            }
        };
        ImageButton mPlayButton = (ImageButton) view.findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(playButtonListener);
        mPlayButton.setVisibility(View.GONE);
        // ------------------------------------
        View.OnClickListener pauseButtonListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) {
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
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            LOGW(TAG, "AudioPlaybackStartListener");
            showUIForMode();
        }
    }

    private class UpdateUIListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            LOGW(TAG, "UpdateUIListener");
            showUIForMode();
        }
    }

    // ----------------------------------------- RECEIVERS INSTANCES
    private AudioPlaybackStartListener audioPlaybackStartListener;
    private UpdateUIListener updateUIListener;

    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        registerAudioPlaybackStartReceiver(bRegister);
        registerUpdateUIReceiver(bRegister);
    }

    private void registerAudioPlaybackStartReceiver(boolean reg) {
        LOGW(TAG, "registerAudioPlaybackStartReceiver: " + (reg ? "true" : "false"));
        if (getContext() != null) {
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
        if (getContext() != null) {
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
