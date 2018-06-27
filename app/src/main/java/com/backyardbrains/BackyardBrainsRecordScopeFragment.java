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
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.events.AmModulationDetectionEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.SpikerBoxHardwareTypeDetectionEvent;
import com.backyardbrains.events.UsbCommunicationEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.filters.AmModulationFilterSettingsDialog;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.filters.FilterSettingsDialog;
import com.backyardbrains.filters.UsbMuscleProFilterSettingsDialog;
import com.backyardbrains.filters.UsbNeuronProFilterSettingsDialog;
import com.backyardbrains.filters.UsbSerialFilterSettingsDialog;
import com.backyardbrains.utils.SpikerBoxHardwareType;
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
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BackyardBrainsRecordScopeFragment extends BaseWaveformFragment
    implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(BackyardBrainsRecordScopeFragment.class);

    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    // Maximum time that should be processed in any given moment (in seconds)
    private static final double MAX_AUDIO_PROCESSING_TIME = 6; // 6 seconds
    private static final double MAX_USB_PROCESSING_TIME = 12; // 12 seconds

    @BindView(R.id.ibtn_filters) ImageButton ibtnFilters;
    @BindView(R.id.ibtn_usb) protected ImageButton ibtnUsb;
    @BindView(R.id.ibtn_record) protected ImageButton ibtnRecord;
    @BindView(R.id.tv_stop_recording) protected TextView tvStopRecording;

    private FilterSettingsDialog filterSettingsDialog;
    private BYBSlidingView stopRecButton;
    private Unbinder unbinder;

    private final FilterSettingsDialog.FilterSelectionListener FILTER_SELECTION_LISTENER =
        new FilterSettingsDialog.FilterSelectionListener() {
            @Override public void onFilterSelected(@NonNull Filter filter) {
                setFilter(filter);
            }
        };

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");

        // this will start microphone if we are switching from another fragment
        if (getAudioService() != null) startActiveInput(getAudioService());
    }

    @Override public void onResume() {
        super.onResume();
        LOGD(TAG, "onResume()");

        setupButtons(false);
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");

        if (getAudioService() != null) getAudioService().stopActiveInputSource();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        LOGD(TAG, "onDestroyView()");
        unbinder.unbind();
    }

    //==============================================
    //  ABSTRACT METHODS IMPLEMENTATIONS
    //==============================================

    @Override protected View createView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        LOGD(TAG, "createView()");

        final View view = inflater.inflate(R.layout.fragment_record_scope, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    @Override protected BYBBaseRenderer createRenderer() {
        final WaveformRenderer renderer = new WaveformRenderer(this);
        renderer.setOnDrawListener(new BYBBaseRenderer.OnDrawListener() {

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                if (getActivity() != null && getAudioService() != null) {
                    viewableTimeSpanUpdateRunnable.setSampleRate(getAudioService().getSampleRate());
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceHeight(drawSurfaceHeight);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable);
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

    protected boolean usbDetected() {
        return getAudioService() != null && getAudioService().getDeviceCount() > 0;
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        // this will start microphone if we are coming from background
        if (getAudioService() != null) startActiveInput(getAudioService());

        // update filters button
        setupFiltersButton();
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStartedEvent(AudioRecordingStartedEvent event) {
        setupButtons(true);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingProgressEvent(AudioRecordingProgressEvent event) {
        tvStopRecording.setText(String.format(getString(R.string.tap_to_stop_recording),
            WavUtils.formatWavProgress((int) event.getProgress(), event.getSampleRate())));
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStoppedEvent(AudioRecordingStoppedEvent event) {
        setupButtons(true);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbDeviceConnectionEvent(UsbDeviceConnectionEvent event) {
        // usb is detached, we should start listening to microphone again
        if (!event.isConnected() && getAudioService() != null) startMicrophone(getAudioService());
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbPermissionEvent(UsbPermissionEvent event) {
        if (!event.isGranted()) {
            if (getContext() != null) {
                ViewUtils.toast(getContext(), "Please reconnect the device and grant permission to be able to use it");
            }

            // user didn't get , we should start listening to microphone again
            if (getAudioService() != null) startMicrophone(getAudioService());
        }
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbCommunicationEvent(UsbCommunicationEvent event) {
        if (!event.isStarted()) if (getAudioService() != null) startMicrophone(getAudioService());

        // update filters button
        setupFiltersButton();
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSpikerBoxBoardTypeDetectionEvent(SpikerBoxHardwareTypeDetectionEvent event) {
        final String spikerBoxBoard;
        Filter filter = null;
        switch (event.getHardwareType()) {
            case SpikerBoxHardwareType.HEART:
                spikerBoxBoard = getString(R.string.board_type_heart);
                filter = Filters.FILTER_HEART;
                break;
            case SpikerBoxHardwareType.MUSCLE:
                spikerBoxBoard = getString(R.string.board_type_muscle);
                filter = Filters.FILTER_MUSCLE;
                break;
            case SpikerBoxHardwareType.PLANT:
                spikerBoxBoard = getString(R.string.board_type_plant);
                filter = Filters.FILTER_PLANT;
                break;
            case SpikerBoxHardwareType.MUSCLE_PRO:
                spikerBoxBoard = getString(R.string.board_type_muscle_pro);
                filter = Filters.FILTER_MUSCLE;
                break;
            case SpikerBoxHardwareType.NEURON_PRO:
                spikerBoxBoard = getString(R.string.board_type_neuron_pro);
                filter = Filters.FILTER_NEURON_PRO;
                break;
            default:
            case SpikerBoxHardwareType.UNKNOWN:
                spikerBoxBoard = "UNKNOWN";
                break;
        }

        // preset filter for the connected board
        if (getAudioService() != null && filter != null) getAudioService().setFilter(filter);
        // show what boar is connected in toast
        if (getActivity() != null) {
            ViewUtils.customToast(getActivity(),
                String.format(getString(R.string.template_connected_to_board), spikerBoxBoard));
        }
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAmModulationDetectionEvent(AmModulationDetectionEvent event) {
        // setup filters button
        setupFiltersButton();
        // filters dialog is opened and AM modulation just ended, close it
        if (!event.isStart() && filterSettingsDialog != null) {
            filterSettingsDialog.dismiss();
            filterSettingsDialog = null;
        }
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        // filters button
        setupFiltersButton();
        // usb button
        setupUsbButton();
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

    // Sets up the filters button depending on the input source
    private void setupFiltersButton() {
        ibtnFilters.setVisibility(shouldShowFilterOptions() ? View.VISIBLE : View.GONE);
        ibtnFilters.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openFilterDialog();
            }
        });
    }

    // Whether filter options button should be visible or not
    private boolean shouldShowFilterOptions() {
        return getAudioService() != null && (getAudioService().isUsbActiveInput()
            || getAudioService().isAmModulationDetected());
    }

    // Opens a dialog with predefined filters that can be applied while processing incoming data
    void openFilterDialog() {
        if (getContext() != null) {
            filterSettingsDialog = getAudioService() != null && getAudioService().isAmModulationDetected()
                ? new AmModulationFilterSettingsDialog(getContext(), FILTER_SELECTION_LISTENER)
                : getAudioService().isActiveUsbInputOfType(SpikerBoxHardwareType.MUSCLE_PRO)
                    ? new UsbMuscleProFilterSettingsDialog(getContext(), FILTER_SELECTION_LISTENER)
                    : getAudioService().isActiveUsbInputOfType(SpikerBoxHardwareType.NEURON_PRO)
                        ? new UsbNeuronProFilterSettingsDialog(getContext(), FILTER_SELECTION_LISTENER)
                        : new UsbSerialFilterSettingsDialog(getContext(), FILTER_SELECTION_LISTENER);
            filterSettingsDialog.show(

                getAudioService() != null && getAudioService().getFilter() != null ? getAudioService().getFilter()
                    : new Filter());
        }
    }

    // Sets a filter that should be applied while processing incoming data
    void setFilter(@NonNull Filter filter) {
        if (getAudioService() != null) getAudioService().setFilter(filter);
    }

    // Sets up the USB connection button depending on whether USB is connected and whether it's active input source.
    private void setupUsbButton() {
        ibtnUsb.setVisibility(usbDetected() ? View.VISIBLE : View.GONE);
        if (getAudioService() != null && getAudioService().isUsbActiveInput()) {
            ibtnUsb.setImageResource(R.drawable.ic_usb_off);
            ibtnUsb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    disconnectFromDevice();
                }
            });
        } else {
            ibtnUsb.setImageResource(R.drawable.ic_usb);
            ibtnUsb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    openDeviceListDialog();
                }
            });
        }
    }

    void openDeviceListDialog() {
        // TODO: 7/19/2017 This method should open dialog with all available BYB devices
        if (usbDetected()) {
            //noinspection ConstantConditions
            final UsbDevice device = getAudioService().getDevice(0);
            if (device != null) connectWithDevice(device.getDeviceName());
            return;
        }

        if (getContext() != null) ViewUtils.toast(getContext(), "No connected devices!");
    }

    private void connectWithDevice(@NonNull String deviceName) {
        if (getAudioService() != null) {
            try {
                startUsb(getAudioService(), deviceName);
            } catch (IllegalArgumentException e) {
                Crashlytics.logException(e);
                if (getContext() != null) {
                    ViewUtils.toast(getContext(), "Error while connecting with device " + deviceName + "!");
                }
            }

            return;
        }

        if (getContext() != null) {
            ViewUtils.toast(getContext(), "Error while connecting with device " + deviceName + "!");
        }
    }

    void disconnectFromDevice() {
        if (getAudioService() != null) {
            try {
                getAudioService().stopUsb();
            } catch (IllegalArgumentException e) {
                Crashlytics.logException(e);
                if (getContext() != null) {
                    ViewUtils.toast(getContext(), "Error while disconnecting from currently connected device!");
                }
            }

            return;
        }

        if (getContext() != null) {
            ViewUtils.toast(getContext(), "Error while disconnecting from currently connected device!");
        }
    }

    private void startActiveInput(@NonNull AudioService audioService) {
        audioService.startActiveInputSource();
        audioService.setMaxProcessingTimeInSeconds(
            audioService.isUsbActiveInput() ? MAX_USB_PROCESSING_TIME : MAX_AUDIO_PROCESSING_TIME);
    }

    private void startMicrophone(@NonNull AudioService audioService) {
        audioService.setMaxProcessingTimeInSeconds(MAX_AUDIO_PROCESSING_TIME);
        audioService.startMicrophone();
    }

    private void startUsb(@NonNull AudioService audioService, @NonNull String deviceName) {
        audioService.setMaxProcessingTimeInSeconds(MAX_USB_PROCESSING_TIME);
        audioService.startUsb(deviceName);
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

    @Override public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        LOGD(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
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

    @AfterPermissionGranted(BYB_WRITE_EXTERNAL_STORAGE_PERM) void startRecording() {
        if (getContext() != null && EasyPermissions.hasPermissions(getContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (getAudioService() != null) getAudioService().startRecording();
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_write_external_storage),
                BYB_WRITE_EXTERNAL_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
}
