package com.backyardbrains.ui;

import android.Manifest;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.dsp.Filters;
import com.backyardbrains.dsp.ProcessingService;
import com.backyardbrains.events.AmModulationDetectionEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.HeartbeatEvent;
import com.backyardbrains.events.SpikerBoxHardwareTypeDetectionEvent;
import com.backyardbrains.events.UsbCommunicationEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.events.UsbSignalSourceDisconnectEvent;
import com.backyardbrains.filters.BandFilter;
import com.backyardbrains.filters.FilterSettingsDialog;
import com.backyardbrains.filters.NotchFilter;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.view.HeartbeatView;
import com.backyardbrains.view.SettingsView;
import com.backyardbrains.view.SlidingView;
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
public class RecordScopeFragment extends BaseWaveformFragment implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(RecordScopeFragment.class);

    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    // Default number of sample sets that should be summed when averaging
    private static final int DEFAULT_AVERAGED_SAMPLE_COUNT = 30;

    private static final String BOOL_THRESHOLD_ON = "bb_threshold_on";
    private static final String BOOL_SETTINGS_ON = "bb_settings_on";

    @BindView(R.id.ibtn_settings) ImageButton ibtnSettings;
    @BindView(R.id.v_settings) SettingsView vSettings;
    @BindView(R.id.ibtn_threshold) ImageButton ibtnThreshold;
    @BindView(R.id.ibtn_avg_trigger_type) ImageButton ibtnAvgTriggerType;
    //@BindView(R.id.ibtn_filters) ImageButton ibtnFilters;
    @BindView(R.id.ibtn_usb) ImageButton ibtnUsb;
    @BindView(R.id.pb_usb_disconnecting) ProgressBar pbUsbDisconnecting;
    @BindView(R.id.ibtn_record) ImageButton ibtnRecord;
    @BindView(R.id.tv_stop_recording) TextView tvStopRecording;
    @BindView(R.id.sb_averaged_sample_count) SeekBar sbAvgSamplesCount;
    @BindView(R.id.tv_averaged_sample_count) TextView tvAvgSamplesCount;
    @BindView(R.id.tb_sound) ToggleButton tbSound;
    @BindView(R.id.hv_heartbeat) HeartbeatView vHeartbeat;
    @BindView(R.id.tv_beats_per_minute) TextView tvBeatsPerMinute;

    private FilterSettingsDialog filterSettingsDialog;
    private SlidingView stopRecButton;
    private Unbinder unbinder;

    private StringBuilder stringBuilder;
    private int tapToStopLength;

    // Whether settings popup is visible or not
    private boolean settingsOn;
    // Whether signal triggering is turned on or off
    private boolean thresholdOn;

    //==============================================
    // EVENT LISTENERS
    //==============================================

    //private final FilterSettingsDialog.FilterSelectionListener filterSelectionListener = this::setBandFilter;

    private final SettingsView.OnSettingChangeListener settingChangeListener =
        new SettingsView.OnSettingChangeListener() {
            @Override public void onSpeakersMuteChanged(boolean mute) {
                setMuteSpeakers(mute);
            }

            @Override public void onBandFilterChanged(@Nullable BandFilter filter) {
                setBandFilter(filter);
            }

            @Override public void onNotchFilterChanged(@Nullable NotchFilter filter) {
                setNotchFilter(filter);
            }

            @Override public void onChannelColorChanged(int channelIndex, @Size(4) float[] color) {
                setChannelColor(color, channelIndex);
            }

            @Override public void onChannelShown(int channelIndex, float[] color) {
                showChannel(channelIndex, color);
            }

            @Override public void onChannelHidden(int channelIndex) {
                hideChannel(channelIndex);
            }
        };

    private final View.OnClickListener settingsClickListener = v -> {
        toggleSettings();
        setupSettingsView();
    };

    private final View.OnClickListener startThresholdOnClickListener = v -> {
        startThresholdMode();
        setupThresholdView();

        // update BPM UI
        updateBpmUI();
    };

    private final View.OnClickListener stopThresholdOnClickListener = v -> {
        stopThresholdMode();
        setupThresholdView();

        // update BPM UI
        updateBpmUI();
    };

    private final SeekBar.OnSeekBarChangeListener averagedSampleCountChangeListener =
        new SeekBar.OnSeekBarChangeListener() {

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // average sample count has changed
                int averagedSampleCount = seekBar.getProgress() > 0 ? seekBar.getProgress() : 1;
                PrefUtils.setAveragedSampleCount(seekBar.getContext(), BaseWaveformFragment.class, averagedSampleCount);
                JniUtils.setAveragedSampleCount(averagedSampleCount);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // minimum sample count is 1
                if (progress <= 0) progress = 1;

                // update count label
                if (tvAvgSamplesCount != null) {
                    tvAvgSamplesCount.setText(String.format(getString(R.string.label_n_times), progress));
                }
            }
        };

    private final View.OnClickListener changeAveragingTriggerTypeOnClickListener =
        v -> openAveragingTriggerTypeDialog();

    //==============================================
    // LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stringBuilder = new StringBuilder(getString(R.string.tap_to_stop_recording));
        tapToStopLength = stringBuilder.length();
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            settingsOn = savedInstanceState.getBoolean(BOOL_SETTINGS_ON);
            thresholdOn = savedInstanceState.getBoolean(BOOL_THRESHOLD_ON);
        }
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

        setupRecordingButtons(false);
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");

        if (getAudioService() != null) getAudioService().stopActiveInputSource();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BOOL_SETTINGS_ON, settingsOn);
        outState.putBoolean(BOOL_THRESHOLD_ON, thresholdOn);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        LOGD(TAG, "onDestroyView()");
        // remove listener in case animation finishes after view has been unbind
        stopRecButton.setAnimationEndListener(null);

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

        // we should set averaged sample count before UI setup
        int averagedSampleCount = PrefUtils.getAveragedSampleCount(view.getContext(), BaseWaveformFragment.class);
        if (averagedSampleCount < 0) averagedSampleCount = DEFAULT_AVERAGED_SAMPLE_COUNT;
        JniUtils.setAveragedSampleCount(averagedSampleCount);

        setupUI();

        return view;
    }

    @Override protected BaseWaveformRenderer createRenderer() {
        final WaveformRenderer renderer = new WaveformRenderer(this);
        renderer.setOnDrawListener((drawSurfaceWidth) -> {
            if (getAudioService() != null) setMilliseconds(getAudioService().getSampleRate(), drawSurfaceWidth);
        });
        renderer.setOnWaveformSelectionListener(index -> {
            if (getAudioService() != null) getAudioService().setSelectedChannel(index);
        });
        return renderer;
    }

    @Override protected boolean isBackable() {
        return false;
    }

    @Override protected WaveformRenderer getRenderer() {
        return (WaveformRenderer) super.getRenderer();
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    protected boolean isRecording() {
        return getAudioService() != null && getAudioService().isRecording();
    }

    protected boolean usbDetected() {
        return getAudioService() != null && getAudioService().getUsbDeviceCount() > 0;
    }

    protected boolean usbDisconnecting() {
        return getAudioService() != null && getAudioService().isUsbDeviceDisconnecting();
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        // this will setup signal source and averaging if we are coming from background
        if (getAudioService() != null) startActiveInput(getAudioService());

        // setup settings view
        setupSettingsView();
        // setup threshold button
        setupThresholdView();
        // setup filters button
        //setupFiltersButton();
        // setup USB button
        setupUsbButton();
        // setup BPM UI
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStartedEvent(AudioRecordingStartedEvent event) {
        setupRecordingButtons(true);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingProgressEvent(AudioRecordingProgressEvent event) {
        stringBuilder.delete(tapToStopLength, stringBuilder.length());
        stringBuilder.append(
            WavUtils.formatWavProgress((int) event.getProgress(), event.getSampleRate(), event.getChannelCount()));
        tvStopRecording.setText(stringBuilder);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStoppedEvent(AudioRecordingStoppedEvent event) {
        setupRecordingButtons(true);
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
        //setupFiltersButton();
        // setup settings view
        setupSettingsView();
        // setup USB button
        setupUsbButton();
        // update BPM label
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbSignalSourceDisconnectEvent(UsbSignalSourceDisconnectEvent event) {
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSpikerBoxBoardTypeDetectionEvent(SpikerBoxHardwareTypeDetectionEvent event) {
        final String spikerBoxBoard;
        BandFilter filter = null;
        switch (event.getHardwareType()) {
            case SpikerBoxHardwareType.HEART_AND_BRAIN:
                spikerBoxBoard = getString(R.string.board_type_heart);
                filter = Filters.FILTER_BAND_HEART;
                break;
            case SpikerBoxHardwareType.MUSCLE:
                spikerBoxBoard = getString(R.string.board_type_muscle);
                filter = Filters.FILTER_BAND_MUSCLE;
                break;
            case SpikerBoxHardwareType.PLANT:
                spikerBoxBoard = getString(R.string.board_type_plant);
                filter = Filters.FILTER_BAND_PLANT;
                break;
            case SpikerBoxHardwareType.MUSCLE_PRO:
                spikerBoxBoard = getString(R.string.board_type_muscle_pro);
                filter = Filters.FILTER_BAND_MUSCLE;
                break;
            case SpikerBoxHardwareType.NEURON_PRO:
                spikerBoxBoard = getString(R.string.board_type_neuron_pro);
                filter = Filters.FILTER_BAND_NEURON_PRO;
                break;
            default:
            case SpikerBoxHardwareType.UNKNOWN:
                spikerBoxBoard = "UNKNOWN";
                break;
        }

        LOGD(TAG, "BOARD DETECTED: " + spikerBoxBoard);

        // preset filter for the connected board
        if (getAudioService() != null && filter != null) getAudioService().setBandFilter(filter);
        // show what boar is connected in toast
        if (getActivity() != null) {
            ViewUtils.customToast(getActivity(),
                String.format(getString(R.string.template_connected_to_board), spikerBoxBoard));
        }
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAmModulationDetectionEvent(AmModulationDetectionEvent event) {
        // setup filters button
        //setupFiltersButton();
        // filters dialog is opened and AM modulation just ended, close it
        if (!event.isStart() && filterSettingsDialog != null) {
            filterSettingsDialog.dismiss();
            filterSettingsDialog = null;
        }
        // update BPM UI
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHeartbeatEvent(HeartbeatEvent event) {
        tvBeatsPerMinute.setText(
            String.format(getString(R.string.template_beats_per_minute), event.getBeatsPerMinute()));
        if (event.getBeatsPerMinute() > 0) {
            vHeartbeat.beep();
        } else {
            vHeartbeat.off();
        }
    }

    //==============================================
    //  UI
    //==============================================

    // Initializes user interface
    private void setupUI() {
        // settings button
        setupSettingsView();
        // threshold button
        setupThresholdView();
        // filters button
        //setupFiltersButton();
        // usb button
        setupUsbButton();
        // for pre-21 SDK we need to tint the progress bar programmatically (post-21 SDK will do it through styles)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ViewUtils.tintDrawable(pbUsbDisconnecting.getIndeterminateDrawable(),
                ContextCompat.getColor(pbUsbDisconnecting.getContext(), R.color.yellow));
        }
        // record button
        ibtnRecord.setOnClickListener(v -> startRecording());
        // stop record button
        stopRecButton = new SlidingView(tvStopRecording, null/*recordAnimationListener*/);
        tvStopRecording.setOnClickListener(v -> {
            if (getAudioService() != null) getAudioService().stopRecording();
        });
        // recording buttons
        setupRecordingButtons(false);
        // BPM UI
        updateBpmUI();
        if (getContext() != null) tbSound.setChecked(PrefUtils.getBpmSound(getContext()));
        tbSound.setOnCheckedChangeListener((compoundButton, b) -> {
            vHeartbeat.setMuteSound(!b);
            if (getContext() != null) PrefUtils.setBpmSound(getContext(), b);
        });
    }

    //==============================================
    // SETTINGS
    //==============================================

    void setupSettingsView() {
        if (settingsOn) {
            // setup settings button
            ibtnSettings.setBackgroundResource(R.drawable.circle_gray_white_active);
            // setup settings overlay
            if (getAudioService() != null) {
                vSettings.setupMuteSpeakers(getAudioService().isMuteSpeakers());
                vSettings.setupFilters(
                    getAudioService().getBandFilter() != null ? getAudioService().getBandFilter() : new BandFilter(),
                    getAudioService().isAmModulationDetected() ? Filters.FREQ_LOW_MAX_CUT_OFF
                        : Filters.FREQ_HIGH_MAX_CUT_OFF,
                    getAudioService().getNotchFilter() != null ? getAudioService().getNotchFilter()
                        : new NotchFilter());
                vSettings.setupChannels(getAudioService().getChannelCount(), getRenderer().getChannelColors());
            }
            vSettings.setVisibility(View.VISIBLE);
            vSettings.setOnSettingChangeListener(settingChangeListener);
        } else {
            // setup threshold button
            ibtnSettings.setBackgroundResource(R.drawable.circle_gray_white);
            // setup settings overlay
            vSettings.setVisibility(View.GONE);
            vSettings.setOnSettingChangeListener(null);
        }
        ibtnSettings.setOnClickListener(settingsClickListener);
    }

    void toggleSettings() {
        settingsOn = !settingsOn;
    }

    void setMuteSpeakers(boolean mute) {
        if (getAudioService() != null) getAudioService().setMuteSpeakers(mute);
    }

    // Sets a band filter that should be applied while processing incoming data
    void setBandFilter(@Nullable BandFilter filter) {
        if (getAudioService() != null) getAudioService().setBandFilter(filter);

        // update BPM UI
        updateBpmUI();
    }

    // Sets a notch filter that should be applied while processing incoming data
    void setNotchFilter(@Nullable NotchFilter filter) {
        if (getAudioService() != null) getAudioService().setNotchFilter(filter);
    }

    // Sets a new color to the channel with specified channelIndex
    void setChannelColor(@Size(4) float[] color, int channelIndex) {
        getRenderer().setChannelColor(channelIndex, color);
    }

    void showChannel(int channelIndex, @Size(4) float[] color) {
        if (getAudioService() != null) getAudioService().showChannel(channelIndex);
        getRenderer().setChannelColor(channelIndex, color);
    }

    void hideChannel(int channelIndex) {
        if (getAudioService() != null) getAudioService().hideChannel(channelIndex);
    }

    //==============================================
    // FILTERS
    //==============================================

    // Sets up the filters button depending on the input source
    //private void setupFiltersButton() {
    //    ibtnFilters.setVisibility(shouldShowFilterOptions() ? View.VISIBLE : View.GONE);
    //    ibtnFilters.setOnClickListener(v -> openFilterDialog());
    //}

    // Whether filter options button should be visible or not
    //private boolean shouldShowFilterOptions() {
    //    return getAudioService() != null && (getAudioService().isUsbActiveInput()
    //        || getAudioService().isAmModulationDetected());
    //}

    // Opens a dialog with predefined filters that can be applied while processing incoming data
    //void openFilterDialog() {
    //    if (getContext() != null && getAudioService() != null) {
    //        filterSettingsDialog =
    //            getAudioService().isAmModulationDetected() ? new AmModulationFilterSettingsDialog(getContext(),
    //                filterSelectionListener)
    //                : getAudioService().isActiveUsbInputOfType(SpikerBoxHardwareType.MUSCLE_PRO)
    //                    ? new UsbMuscleProFilterSettingsDialog(getContext(), filterSelectionListener)
    //                    : getAudioService().isActiveUsbInputOfType(SpikerBoxHardwareType.NEURON_PRO)
    //                        ? new UsbNeuronProFilterSettingsDialog(getContext(), filterSelectionListener)
    //                        : new UsbSerialFilterSettingsDialog(getContext(), filterSelectionListener);
    //        filterSettingsDialog.show(
    //            getAudioService().getBandFilter() != null ? getAudioService().getBandFilter() : new Filter());
    //    }
    //}

    //==============================================
    // THRESHOLD
    //==============================================

    // Sets up the threshold view (threshold on/off button, threshold handle, averaged sample count and averaging trigger type button)
    void setupThresholdView() {
        if (thresholdOn) {
            // setup threshold button
            ibtnThreshold.setBackgroundResource(R.drawable.circle_gray_white_active);
            ibtnThreshold.setOnClickListener(stopThresholdOnClickListener);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.VISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(averagedSampleCountChangeListener);
            sbAvgSamplesCount.setProgress(JniUtils.getAveragedSampleCount());
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.VISIBLE);
        } else {
            // setup threshold button
            ibtnThreshold.setBackgroundResource(R.drawable.circle_gray_white);
            ibtnThreshold.setOnClickListener(startThresholdOnClickListener);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.INVISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(null);
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.INVISIBLE);
        }
        setupThresholdHandleAndAveragingTriggerTypeButtons();
    }

    // Sets up threshold handle and averaging trigger type button
    void setupThresholdHandleAndAveragingTriggerTypeButtons() {
        if (thresholdOn) {
            // setup averaging trigger type button
            ibtnAvgTriggerType.setVisibility(View.VISIBLE);
            final @SignalAveragingTriggerType int triggerType = JniUtils.getAveragingTriggerType();
            switch (triggerType) {
                case SignalAveragingTriggerType.ALL_EVENTS:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_all_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_1:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_1_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_2:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_2_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_3:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_3_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_4:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_4_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_5:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_5_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_6:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_6_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_7:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_7_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_8:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_8_black_24dp);
                    break;
                case SignalAveragingTriggerType.EVENT_9:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_event_9_black_24dp);
                    break;
                case SignalAveragingTriggerType.THRESHOLD:
                    ibtnAvgTriggerType.setImageResource(R.drawable.ic_trigger_threshold_black_24dp);
                    break;
            }
            ibtnAvgTriggerType.setOnClickListener(changeAveragingTriggerTypeOnClickListener);
        } else {
            // setup averaging trigger type button
            ibtnAvgTriggerType.setVisibility(View.INVISIBLE);
            ibtnAvgTriggerType.setOnClickListener(null);
        }
    }

    // Starts the threshold mode
    void startThresholdMode() {
        thresholdOn = true;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);
    }

    // Stops the threshold mode
    void stopThresholdMode() {
        thresholdOn = false;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);

        // threshold should be reset every time it's enabled so let's reset every time on closing
        JniUtils.resetThreshold();
    }

    // Opens a dialog for averaging trigger type selection
    void openAveragingTriggerTypeDialog() {
        if (getContext() != null) {
            MaterialDialog averagingTriggerTypeDialog =
                new MaterialDialog.Builder(getContext()).items(R.array.options_averaging_trigger_type)
                    .itemsCallback((dialog, itemView, position, text) -> setTriggerType(
                        position == 0 ? SignalAveragingTriggerType.THRESHOLD
                            : position == 10 ? SignalAveragingTriggerType.ALL_EVENTS : position))
                    .build();
            averagingTriggerTypeDialog.show();
        }
    }

    // Sets the specified trigger type as the preferred averaging trigger type
    void setTriggerType(@SignalAveragingTriggerType int triggerType) {
        if (getAudioService() != null) getAudioService().setSignalAveragingTriggerType(triggerType);

        setupThresholdHandleAndAveragingTriggerTypeButtons();
    }

    //==============================================
    // BPM
    //==============================================

    // Updates BpPM UI
    void updateBpmUI() {
        // update whether BPM processing should be on
        JniUtils.setBpmProcessing(shouldShowBpm());

        tbSound.setVisibility(shouldShowBpm() ? View.VISIBLE : View.INVISIBLE);
        vHeartbeat.setVisibility(shouldShowBpm() ? View.VISIBLE : View.INVISIBLE);
        vHeartbeat.setMuteSound(getContext() != null && !PrefUtils.getBpmSound(getContext()));
        vHeartbeat.off();
        tvBeatsPerMinute.setVisibility(shouldShowBpm() ? View.VISIBLE : View.INVISIBLE);
        tvBeatsPerMinute.setText(String.format(getString(R.string.template_beats_per_minute), 0));
    }

    // Whether BPM label should be visible or not
    private boolean shouldShowBpm() {
        // BPM should be shown if either usb is active input source or we are in AM modulation,
        // and if current filter is default EKG filter
        return getAudioService() != null && thresholdOn && (getAudioService().isUsbActiveInput()
            || getAudioService().isAmModulationDetected()) && ObjectUtils.equals(getAudioService().getBandFilter(),
            Filters.FILTER_BAND_HEART);
    }

    //==============================================
    // USB
    //==============================================

    // Sets up the USB connection button depending on whether USB is connected and whether it's active input source.
    private void setupUsbButton() {
        boolean disconnecting = usbDisconnecting();
        ibtnUsb.setVisibility(usbDetected() ? View.VISIBLE : View.GONE);
        ibtnUsb.setImageResource(disconnecting ? 0 : R.drawable.ic_usb_black_24dp);
        setupUsbDisconnectingView(!disconnecting);
        if (getAudioService() != null && getAudioService().isUsbActiveInput() || disconnecting) {
            ibtnUsb.setBackgroundResource(R.drawable.circle_gray_white_active);
            ibtnUsb.setOnClickListener(v -> {
                setupUsbDisconnectingView(false);
                disconnectFromDevice();
            });
        } else {
            ibtnUsb.setBackgroundResource(R.drawable.circle_gray_white);
            ibtnUsb.setOnClickListener(v -> connectWithDevice());
        }
    }

    // Triggers connection with the first found valid usb device
    void connectWithDevice() {
        if (usbDetected()) {
            //noinspection ConstantConditions
            final UsbDevice device = getAudioService().getDevice(0);
            if (device != null) {
                final String deviceName = device.getDeviceName();
                try {
                    startUsb(getAudioService(), deviceName);
                } catch (IllegalArgumentException e) {
                    Crashlytics.logException(e);
                    if (getContext() != null) {
                        ViewUtils.toast(getContext(), "Error while connecting with device " + deviceName + "!");
                    }
                }
            } else if (getContext() != null) {
                ViewUtils.toast(getContext(), "No connected devices!");
            }
            return;
        }

        if (getContext() != null) ViewUtils.toast(getContext(), "No connected devices!");
    }

    // Triggers currently connected usb device to disconnect
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

    private void setupUsbDisconnectingView(boolean enable) {
        ibtnUsb.setEnabled(enable);
        ibtnUsb.setAlpha(enable ? 1f : .75f);
        pbUsbDisconnecting.setVisibility(enable ? View.GONE : View.VISIBLE);
    }

    //==============================================
    // RECORDING
    //==============================================

    // Set buttons visibility depending on whether audio is currently being recorded or not
    private void setupRecordingButtons(boolean animate) {
        ibtnRecord.setVisibility(isRecording() ? View.GONE : View.VISIBLE);
        if (animate) {
            stopRecButton.show(isRecording());
        } else {
            tvStopRecording.setVisibility(isRecording() ? View.VISIBLE : View.GONE);
        }
    }

    //==============================================
    // INPUT SOURCES AND MAX PROCESSING TIMES
    //==============================================

    private void startActiveInput(@NonNull ProcessingService processingService) {
        // this will set signal averaging if we are coming from background
        processingService.setSignalAveraging(thresholdOn);
        // this will set signal averaging trigger type if we are coming from background
        processingService.setSignalAveragingTriggerType(JniUtils.getAveragingTriggerType());
        // this will start microphone if we are coming from background
        processingService.startActiveInputSource();

        // resume the threshold in case it was paused during playback
        JniUtils.resumeThreshold();
    }

    private void startMicrophone(@NonNull ProcessingService processingService) {
        processingService.startMicrophone();
        processingService.setBandFilter(new BandFilter());
    }

    private void startUsb(@NonNull ProcessingService processingService, @NonNull String deviceName) {
        processingService.startUsb(deviceName);
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
