package com.backyardbrains.ui;

import android.Manifest;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.materialdialogs.GravityEnum;
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
import com.backyardbrains.events.ExpansionBoardTypeDetectionEvent;
import com.backyardbrains.events.HeartbeatEvent;
import com.backyardbrains.events.SpikerBoxHardwareTypeDetectionEvent;
import com.backyardbrains.events.UsbCommunicationEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.events.UsbSignalSourceDisconnectEvent;
import com.backyardbrains.filters.BandFilter;
import com.backyardbrains.filters.FilterSettingsDialog;
import com.backyardbrains.filters.NotchFilter;
import com.backyardbrains.utils.ExpansionBoardType;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.ArrayList;
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
    // Holds names of all the available channels
    private static final List<String> CHANNEL_NAMES = new ArrayList<>();

    private static final String BOOL_THRESHOLD_ON = "bb_threshold_on";
    private static final String BOOL_SETTINGS_ON = "bb_settings_on";
    private static final String BOOL_FFT_ON = "bb_fft_on";

    @BindView(R.id.ibtn_settings) ImageButton ibtnSettings;
    @BindView(R.id.v_settings) SettingsView vSettings;
    @BindView(R.id.ibtn_threshold) ImageButton ibtnThreshold;
    @BindView(R.id.btn_fft) Button btnFft;
    @BindView(R.id.tv_select_channel) TextView tvSelectChannel;
    @BindView(R.id.ibtn_avg_trigger_type) ImageButton ibtnAvgTriggerType;
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
    // Whether signal averaging is turned on or off
    private boolean thresholdOn;
    // Whether fft processing is turned on or off
    private boolean fftOn;

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

    private final View.OnClickListener fftClickListener = v -> {
        toggleFft();
        setupFftView();
    };

    private final View.OnClickListener selectChannelClickListener = v -> openChannelsDialog();

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
            fftOn = savedInstanceState.getBoolean(BOOL_FFT_ON);
        }
    }

    @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");

        // this will start microphone if we are switching from another fragment
        if (getProcessingService() != null) startActiveInput(getProcessingService());
    }

    @Override public void onResume() {
        super.onResume();
        LOGD(TAG, "onResume()");

        setupRecordingButtons(false);
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");

        // stop currently active input source (mic/usb)
        if (getProcessingService() != null) getProcessingService().stopActiveInputSource();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BOOL_SETTINGS_ON, settingsOn);
        outState.putBoolean(BOOL_THRESHOLD_ON, thresholdOn);
        outState.putBoolean(BOOL_FFT_ON, fftOn);
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
        renderer.setOnWaveformSelectionListener(index -> {
            if (getProcessingService() != null) getProcessingService().setSelectedChannel(index);
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
        return getProcessingService() != null && getProcessingService().isRecording();
    }

    protected boolean usbDetected() {
        return getProcessingService() != null && getProcessingService().getUsbDeviceCount() > 0;
    }

    protected boolean usbDisconnecting() {
        return getProcessingService() != null && getProcessingService().isUsbDeviceDisconnecting();
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        // this will setup signal source and averaging if we are coming from background
        if (getProcessingService() != null) startActiveInput(getProcessingService());

        // setup settings view
        setupSettingsView();
        // setup threshold view
        setupThresholdView();
        // setup fft view
        setupFftView();
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
            WavUtils.formatWavProgress((int) event.getProgress(), event.getSampleRate(), event.getChannelCount(),
                event.getBitsPerSample()));
        tvStopRecording.setText(stringBuilder);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStoppedEvent(AudioRecordingStoppedEvent event) {
        setupRecordingButtons(true);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbDeviceConnectionEvent(UsbDeviceConnectionEvent event) {
        // usb is detached, we should start listening to microphone again
        if (!event.isConnected() && getProcessingService() != null) startMicrophone(getProcessingService());
        // setup fft view
        setupFftView();
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
            if (getProcessingService() != null) startMicrophone(getProcessingService());
        }
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbCommunicationEvent(UsbCommunicationEvent event) {
        if (!event.isStarted()) {
            if (getProcessingService() != null) startMicrophone(getProcessingService());
            // load correct horizontal and vertical zoom factors for the connected board
            if (getRenderer() != null) getRenderer().resetBoardType();
        }

        // setup settings view
        setupSettingsView();
        // setup fft view
        setupFftView();
        // setup USB button
        setupUsbButton();
        // update BPM label
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsbSignalSourceDisconnectEvent(UsbSignalSourceDisconnectEvent event) {
        // setup fft view
        setupFftView();
        // setup USB button
        setupUsbButton();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSpikerBoxBoardTypeDetectionEvent(SpikerBoxHardwareTypeDetectionEvent event) {
        final @SpikerBoxHardwareType int boardType = event.getHardwareType();
        final String spikerBoxBoard;
        BandFilter filter = null;
        switch (boardType) {
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
            case SpikerBoxHardwareType.HUMAN_PRO:
                spikerBoxBoard = getString(R.string.board_type_human_pro);
                filter = Filters.FILTER_BAND_HUMAN_PRO;
                break;
            default:
            case SpikerBoxHardwareType.UNKNOWN:
            case SpikerBoxHardwareType.NONE:
                spikerBoxBoard = "UNKNOWN";
                break;
        }

        LOGD(TAG, "BOARD DETECTED: " + spikerBoxBoard);

        // preset filter for the connected board
        if (getProcessingService() != null && filter != null) getProcessingService().setBandFilter(filter);
        // show what boar is connected in toast
        if (getActivity() != null) {
            ViewUtils.customToast(getActivity(),
                String.format(getString(R.string.template_connected_to_board), spikerBoxBoard));
        }
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onExpansionBoardTypeDetectionEvent(ExpansionBoardTypeDetectionEvent event) {
        final @ExpansionBoardType int expansionBoardType = event.getExpansionBoardType();
        final String expansionBoard;
        switch (expansionBoardType) {
            case ExpansionBoardType.ADDITIONAL_INPUTS:
                expansionBoard = "ADDITIONAL INPUTS";
                break;
            case ExpansionBoardType.HAMMER:
                expansionBoard = "HAMMER";
                break;
            case ExpansionBoardType.JOYSTICK:
                expansionBoard = "JOYSTICK";
                break;
            default:
            case ExpansionBoardType.NONE:
                expansionBoard = "NONE";
                break;
        }

        LOGD(TAG, "EXPANSION BOARD DETECTED: " + expansionBoard);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAmModulationDetectionEvent(AmModulationDetectionEvent event) {
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
        // threshold view
        setupThresholdView();
        // fft view
        setupFftView();
        // usb button
        setupUsbButton();
        // for pre-21 SDK we need to tint the progress bar programmatically (post-21 SDK will do it through styles)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ViewUtils.tintDrawable(pbUsbDisconnecting.getIndeterminateDrawable(),
                ContextCompat.getColor(pbUsbDisconnecting.getContext(), R.color.black));
        }
        // record button
        ibtnRecord.setOnClickListener(v -> startRecording());
        // stop record button
        stopRecButton = new SlidingView(tvStopRecording, null/*recordAnimationListener*/);
        tvStopRecording.setOnClickListener(v -> {
            if (getProcessingService() != null) getProcessingService().stopRecording();
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
            if (getProcessingService() != null) {
                vSettings.setupMuteSpeakers(getProcessingService().isMuteSpeakers());
                vSettings.setupFilters(
                    getProcessingService().getBandFilter() != null ? getProcessingService().getBandFilter()
                        : new BandFilter(),
                    getProcessingService().isAmModulationDetected() ? Filters.FREQ_LOW_MAX_CUT_OFF
                        : Filters.FREQ_HIGH_MAX_CUT_OFF,
                    getProcessingService().getNotchFilter() != null ? getProcessingService().getNotchFilter()
                        : new NotchFilter());
                vSettings.setupChannels(getProcessingService().getChannelCount(), getRenderer().getChannelColors());
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
        if (getProcessingService() != null) getProcessingService().setMuteSpeakers(mute);
    }

    // Sets a band filter that should be applied while processing incoming data
    void setBandFilter(@Nullable BandFilter filter) {
        if (getProcessingService() != null) getProcessingService().setBandFilter(filter);

        // update BPM UI
        updateBpmUI();
    }

    // Sets a notch filter that should be applied while processing incoming data
    void setNotchFilter(@Nullable NotchFilter filter) {
        if (getProcessingService() != null) getProcessingService().setNotchFilter(filter);
    }

    // Sets a new color to the channel with specified channelIndex
    void setChannelColor(@Size(4) float[] color, int channelIndex) {
        getRenderer().setChannelColor(channelIndex, color);
    }

    void showChannel(int channelIndex, @Size(4) float[] color) {
        if (getProcessingService() != null) getProcessingService().showChannel(channelIndex);
        getRenderer().setChannelColor(channelIndex, color);

        // setup fft view
        setupFftView();
    }

    void hideChannel(int channelIndex) {
        if (getProcessingService() != null) getProcessingService().hideChannel(channelIndex);

        // setup fft view
        setupFftView();
    }

    //==============================================
    // THRESHOLD
    //==============================================

    // Sets up the threshold view (threshold on/off button, threshold handle, averaged sample count and averaging trigger type button)
    void setupThresholdView() {
        // setup threshold view
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
            // stop fft (if threshold is turned on fft should be stopped)
            stopFft();
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
        // setup threshold trigger type button
        setupThresholdHandleAndAveragingTriggerTypeButtons();
        // setup fft view
        setupFftView();
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
        if (getProcessingService() != null) getProcessingService().setSignalAveraging(thresholdOn);
    }

    // Stops the threshold mode
    void stopThresholdMode() {
        thresholdOn = false;
        if (getProcessingService() != null) getProcessingService().setSignalAveraging(thresholdOn);

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
        if (getProcessingService() != null) getProcessingService().setSignalAveragingTriggerType(triggerType);

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
        return getProcessingService() != null && thresholdOn && (getProcessingService().isUsbActiveInput()
            || getProcessingService().isAmModulationDetected()) && ObjectUtils.equals(
            getProcessingService().getBandFilter(), Filters.FILTER_BAND_HEART);
    }

    //==============================================
    // FFT
    //==============================================

    private void setupFftView() {
        // setup settings button
        btnFft.setBackgroundResource(fftOn ? R.drawable.circle_gray_white_active : R.drawable.circle_gray_white);
        btnFft.setVisibility(thresholdOn ? View.GONE : View.VISIBLE);
        btnFft.setOnClickListener(fftClickListener);
        // setup select channel text view
        tvSelectChannel.setVisibility(
            getProcessingService() != null && getProcessingService().getVisibleChannelCount() > 1 && !thresholdOn
                && fftOn ? View.VISIBLE : View.GONE);
        tvSelectChannel.setOnClickListener(selectChannelClickListener);
    }

    private void toggleFft() {
        fftOn = !fftOn;
        if (getProcessingService() != null) getProcessingService().setFftProcessing(fftOn);
    }

    private void stopFft() {
        if (fftOn) toggleFft();
    }

    // Opens a dialog for channel selection
    void openChannelsDialog() {
        if (getContext() != null) {
            // populate channel names collection
            int channelCount = getProcessingService() != null ? getProcessingService().getChannelCount() : 1;
            int selectedChannel = getProcessingService() != null ? getProcessingService().getSelectedChanel() : 0;
            CHANNEL_NAMES.clear();
            for (int i = 0; i < channelCount; i++) {
                if (getProcessingService() != null && getProcessingService().isChannelVisible(i)) {
                    CHANNEL_NAMES.add(String.format(getString(R.string.template_channel_name), i + 1));
                }
            }
            final MaterialDialog channelsDialog = new MaterialDialog.Builder(getContext()).items(CHANNEL_NAMES)
                .itemsCallbackSingleChoice(selectedChannel, (dialog, itemView, which, text) -> {
                    if (getProcessingService() != null) getProcessingService().setSelectedChannel(which);
                    return true;
                })
                .alwaysCallSingleChoiceCallback()
                .itemsGravity(GravityEnum.CENTER)
                .build();
            channelsDialog.show();
        }
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
        if (getProcessingService() != null && getProcessingService().isUsbActiveInput() || disconnecting) {
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
            final UsbDevice device = getProcessingService().getDevice(0);
            if (device != null) {
                final String deviceName = device.getDeviceName();
                try {
                    startUsb(getProcessingService(), deviceName);
                } catch (IllegalArgumentException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
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
        if (getProcessingService() != null) {
            try {
                getProcessingService().stopUsb();
            } catch (IllegalArgumentException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
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
        ibtnRecord.setVisibility(isRecording() ? View.INVISIBLE : View.VISIBLE);
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
        // this will set fft processing if we are coming from background
        processingService.setFftProcessing(fftOn);
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
            if (getProcessingService() != null) getProcessingService().startRecording();
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_write_external_storage_record),
                BYB_WRITE_EXTERNAL_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
}
