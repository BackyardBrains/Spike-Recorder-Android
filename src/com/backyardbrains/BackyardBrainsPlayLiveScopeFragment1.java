package com.backyardbrains;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.BYBSlidingView;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGW;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public abstract class BackyardBrainsPlayLiveScopeFragment1 extends BackyardBrainsBaseScopeFragment
    implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(BackyardBrainsPlayLiveScopeFragment1.class);

    @BindView(R.id.ibtn_record) ImageButton ibtnRecord;
    @BindView(R.id.tv_stop_recording) View tvStopRecording;
    @BindView(R.id.ibtn_close) ImageButton ibtnClose;
    @BindView(R.id.ibtn_play) ImageButton ibtnPlay;
    @BindView(R.id.ibtn_pause) ImageButton ibtnPause;

    private Unbinder unbinder;

    BYBSlidingView stopRecButton;
    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    //////////////////////////////////////////////////////////////////////////////
    //                       Lifecycle overrides
    //////////////////////////////////////////////////////////////////////////////

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

    protected abstract boolean canRecord();

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        return new WaveformRenderer(fragment, preparedBuffer);
    }

    @Override protected int getLayoutID() {
        return R.layout.play_live_rec_scope_layout;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- OTHER METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean getIsRecording() {
        return getAudioService() != null && getAudioService().isRecording();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Permission Request >= API 23
    //////////////////////////////////////////////////////////////////////////////

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override public void onPermissionsGranted(int requestCode, List<String> perms) {
        LOGD(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override public void onPermissionsDenied(int requestCode, List<String> perms) {
        LOGD(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).setRationale(R.string.rationale_ask_again)
                .setTitle(R.string.title_settings_dialog)
                .setPositiveButton(R.string.action_setting)
                .setNegativeButton(R.string.action_cancel)
                .setRequestCode(BYB_SETTINGS_SCREEN)
                .build()
                .show();
        }
    }

    @AfterPermissionGranted(BYB_WRITE_EXTERNAL_STORAGE_PERM) private void startRecording() {
        if (EasyPermissions.hasPermissions(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (getAudioService() != null) getAudioService().startRecording();
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_write_external_storage),
                BYB_WRITE_EXTERNAL_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////

    protected void showPauseButton(boolean show) {
        showButton(ibtnPause, show);
    }

    protected void showPlayButton(boolean show) {
        showButton(ibtnPlay, show);
    }

    protected void showCloseButton(boolean show) {
        showButton(ibtnClose, show);
    }

    protected void showRecButton(boolean show) {
        showButton(ibtnRecord, show);
    }

    private void setupUI() {
        setupButtons();
        showUIForMode();
        ((BackyardBrainsMain) getActivity()).showButtons(true);
    }

    private void setupButtons() {
        // record button
        ibtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startRecording();
            }
        });
        // stop record button
        stopRecButton = new BYBSlidingView(tvStopRecording, getContext(), "tapToStopRec");
        tvStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) getAudioService().stopRecording();
            }
        });
        // close button
        ibtnClose.setVisibility(View.GONE);
        ibtnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ibtnClose.setVisibility(View.GONE);
                if (getContext() != null) {
                    Intent i = new Intent();
                    i.setAction("BYBCloseButton");
                    getContext().sendBroadcast(i);
                }
            }
        });
        // play button
        ibtnPlay.setVisibility(View.GONE);
        ibtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) getAudioService().togglePlayback(true);
            }
        });
        // pause button
        ibtnPause.setVisibility(View.GONE);
        ibtnPause.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) getAudioService().togglePlayback(false);
            }
        });
    }

    private void showUIForMode() {
        boolean bPlaybackMode = getIsPlaybackMode();
        showPlaybackButtons(bPlaybackMode);
        showRecordingButtons(!bPlaybackMode);
    }

    private void showPlaybackButtons(boolean show) {
        boolean bIsPlaying = getIsPlaying();
        showPauseButton(bIsPlaying && show);
        showPlayButton(!bIsPlaying && show);
        showCloseButton(show);
    }

    private void showRecordingButtons(boolean show) {
        boolean bIsRecording = getIsRecording();
        showRecButton(show && canRecord() && !bIsRecording);
        showRecBar(show && canRecord() && bIsRecording);
    }

    private void showRecBar(boolean show) {
        if (stopRecButton != null) stopRecButton.show(show && canRecord() && getIsRecording());
    }

    private void showButton(View view, boolean show) {
        if (view != null) {
            if (show) {
                view.setVisibility(View.VISIBLE);
            } else {
                if (view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class AudioPlaybackStartListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            LOGD(TAG, "BYBAudioPlaybackStart broadcast received!");
            showUIForMode();
        }
    }

    private class UpdateUIListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            LOGD(TAG, "BYBUpdateUI broadcast received!");
            showUIForMode();
        }
    }

    // ----------------------------------------- RECEIVERS INSTANCES
    private AudioPlaybackStartListener audioPlaybackStartListener;
    private UpdateUIListener updateUIListener;

    // ----------------------------------------- REGISTER RECEIVERS
    @Override public void registerReceivers(boolean bRegister) {
        super.registerReceivers(bRegister);
        LOGD(TAG, "registerReceivers()");
        registerAudioPlaybackStartReceiver(bRegister);
        registerUpdateUIReceiver(bRegister);
    }

    private void registerAudioPlaybackStartReceiver(boolean reg) {
        LOGW(TAG, "registerAudioPlaybackStartReceiver: " + (reg ? "true" : "false"));
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBAudioPlaybackStart");
                audioPlaybackStartListener = new AudioPlaybackStartListener();
                getContext().registerReceiver(audioPlaybackStartListener, intentFilter);
            } else {
                getContext().unregisterReceiver(audioPlaybackStartListener);
            }
        }
    }

    private void registerUpdateUIReceiver(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBUpdateUI");
                updateUIListener = new UpdateUIListener();
                getContext().registerReceiver(updateUIListener, intentFilter);
            } else {
                getContext().unregisterReceiver(updateUIListener);
            }
        }
    }
}
