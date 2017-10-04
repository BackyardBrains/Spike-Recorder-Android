package com.backyardbrains;

import android.Manifest;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.events.AmModulationDetectionEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.filters.FilterSettingsDialog;
import com.backyardbrains.utils.BYBConstants;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.view.BYBSlidingView;
import com.crashlytics.android.Crashlytics;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BackyardBrainsRecordScopeFragment extends BaseWaveformFragment
    implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(BackyardBrainsRecordScopeFragment.class);

    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    @BindView(R.id.ibtn_am_modulation) ImageButton ibtnAmModulation;
    @BindView(R.id.ibtn_usb) protected ImageButton ibtnUsb;
    @BindView(R.id.ibtn_record) protected ImageButton ibtnRecord;
    @BindView(R.id.tv_stop_recording) protected TextView tvStopRecording;

    private FilterSettingsDialog filterSettingsDialog;
    private BYBSlidingView stopRecButton;
    private Unbinder unbinder;

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public void onStart() {
        super.onStart();

        // this will start microphone if we are switching from another fragment
        if (getAudioService() != null) getAudioService().startMicrophone();
    }

    @Override public void onResume() {
        super.onResume();

        setupButtons(false);
    }

    @Override public void onStop() {
        super.onStop();

        if (getAudioService() != null) getAudioService().stopMicrophone();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //==============================================
    //  ABSTRACT METHODS IMPLEMENTATIONS
    //==============================================

    @Override protected View createView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_record_scope, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    @Override
    protected BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        final WaveformRenderer renderer = new WaveformRenderer(fragment, preparedBuffer);
        renderer.setCallback(new BYBBaseRenderer.CallbackAdapter() {

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (getAudioService() != null) {
                                setMilliseconds(
                                    drawSurfaceWidth / (float) getAudioService().getSampleRate() * 1000 / 2);
                            }

                            setMillivolts(
                                (float) drawSurfaceHeight / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale);
                        }
                    });
                }
            }
        });
        return renderer;
    }

    @Override protected boolean isBackable() {
        return false;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        // this will start microphone if we are coming from background
        if (getAudioService() != null) getAudioService().startMicrophone();

        // update am modulation button visibility
        ibtnAmModulation.setVisibility(
            getAudioService() != null && getAudioService().isAmModulationDetected() ? View.VISIBLE : View.GONE);
        // update usb button visibility
        ibtnUsb.setVisibility(
            /*getAudioService() != null && getAudioService().getDeviceCount() > 0 ? View.VISIBLE : */View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStartedEvent(AudioRecordingStartedEvent event) {
        setupButtons(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingProgressEvent(AudioRecordingProgressEvent event) {
        tvStopRecording.setText(String.format(getString(R.string.tap_to_stop_recording),
            WavUtils.formatWavProgress((int) event.getProgress())));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStoppedEvent(AudioRecordingStoppedEvent event) {
        setupButtons(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onUsbDeviceConnectionEvent(UsbDeviceConnectionEvent event) {
        ibtnUsb.setVisibility(/*event.isAttached() ? View.VISIBLE : */View.GONE);

        // usb is detached, we should start listening to microphone again
        if (!event.isAttached() && getAudioService() != null) getAudioService().startMicrophone();
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onUsbPermissionEvent(UsbPermissionEvent event) {
        if (!event.isGranted()) {
            ViewUtils.toast(getContext(), "Permission not granted!!!");

            if (getAudioService() != null) getAudioService().startMicrophone();
        } else {
            if (getAudioService() != null) getAudioService().stopMicrophone();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAmModulationDetectionEvent(AmModulationDetectionEvent event) {
        if (event.isStart()) {
            ibtnAmModulation.setVisibility(View.VISIBLE);
        } else {
            ibtnAmModulation.setVisibility(View.GONE);
            if (filterSettingsDialog != null) {
                filterSettingsDialog.dismiss();
                filterSettingsDialog = null;
            }
        }
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        // am modulation button
        ibtnAmModulation.setVisibility(
            getAudioService() != null && getAudioService().isAmModulationDetected() ? View.VISIBLE : View.GONE);
        ibtnAmModulation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openFilterDialog();
            }
        });
        // usb button
        ibtnUsb.setVisibility(
            /*getAudioService() != null && getAudioService().getDeviceCount() > 0 ? View.VISIBLE : */View.GONE);
        ibtnUsb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openDeviceListDialog();
            }
        });
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

    private void openFilterDialog() {
        filterSettingsDialog =
            new FilterSettingsDialog(getContext(), new FilterSettingsDialog.FilterSelectionListener() {
                @Override public void onFilterSelected(@NonNull Filter filter) {
                    setFilter(filter);
                }
            });
        filterSettingsDialog.show(
            getAudioService() != null && getAudioService().getFilter() != null ? getAudioService().getFilter()
                : new Filter());
    }

    private void setFilter(@NonNull Filter filter) {
        if (getAudioService() != null) getAudioService().setFilter(filter);
    }

    void openDeviceListDialog() {
        // TODO: 7/19/2017 This method should open dialog with all available BYB devices
        if (getAudioService() != null && getAudioService().getDeviceCount() > 0) {
            final UsbDevice device = getAudioService().getDevice(0);
            if (device != null) connectWithDevice(device.getDeviceName());
            return;
        }

        ViewUtils.toast(getContext(), "No connected devices!");
    }

    private void connectWithDevice(@NonNull String deviceName) {
        if (getAudioService() != null) {
            try {
                getAudioService().connectToUsbDevice(deviceName);
            } catch (IllegalArgumentException e) {
                Crashlytics.logException(e);
                ViewUtils.toast(getContext(), "Error while connecting with device " + deviceName + "!");
            }

            return;
        }

        ViewUtils.toast(getContext(), "Error while connecting with device " + deviceName + "!");
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
