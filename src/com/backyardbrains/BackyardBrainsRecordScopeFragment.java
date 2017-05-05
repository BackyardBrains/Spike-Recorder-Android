package com.backyardbrains;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.utls.LogUtils;
import com.backyardbrains.view.BYBSlidingView;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utls.LogUtils.LOGD;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BackyardBrainsRecordScopeFragment extends BaseWaveformFragment
    implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = LogUtils.makeLogTag(BackyardBrainsRecordScopeFragment.class);

    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    @BindView(R.id.ibtn_record) ImageButton ibtnRecord;
    @BindView(R.id.tv_stop_recording) View tvStopRecording;

    private Unbinder unbinder;
    private BYBSlidingView stopRecButton;

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //==============================================
    //  ABSTRACT METHODS IMPLEMENTATIONS
    //==============================================

    @Override protected int getLayoutRes() {
        return R.layout.fragment_record_scope;
    }

    @Override
    protected void initView(@NonNull View view, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        unbinder = ButterKnife.bind(this, view);

        setupUI();
    }

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        final WaveformRenderer renderer = new WaveformRenderer(fragment, preparedBuffer);
        renderer.setCallback(new BYBBaseRenderer.CallbackAdapter() {
            @Override public void onTimeChange(final float milliseconds) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setMilliseconds(milliseconds);
                        }
                    });
                }
            }

            @Override public void onSignalChange(final float millivolts) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setMillivolts(millivolts);
                        }
                    });
                }
            }
        });
        return renderer;
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    protected boolean isRecording() {
        return getAudioService() != null && getAudioService().isRecording();
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStartedEvent(AudioRecordingStartedEvent event) {
        setupButtons(true);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStoppedEvent(AudioRecordingStoppedEvent event) {
        setupButtons(true);
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
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
        // set initial visibility
        setupButtons(false);
    }

    // Set buttons visibility depending on whether audio is currently being recorded or not
    private void setupButtons(boolean animate) {
        ibtnRecord.setVisibility(isRecording() ? View.GONE : View.VISIBLE);
        if (animate) {
            stopRecButton.show(isRecording());
        } else {
            tvStopRecording.setVisibility(isRecording() ? View.VISIBLE : View.GONE);
        }
    }

    //==============================================
    // WRITE_EXTERNAL_STORAGE PERMISSION
    //==============================================

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
}
