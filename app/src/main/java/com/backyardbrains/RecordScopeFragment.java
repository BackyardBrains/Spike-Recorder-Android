package com.backyardbrains;

import android.Manifest;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
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
import com.backyardbrains.filters.AmModulationFilterSettingsDialog;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.filters.FilterSettingsDialog;
import com.backyardbrains.filters.UsbMuscleProFilterSettingsDialog;
import com.backyardbrains.filters.UsbNeuronProFilterSettingsDialog;
import com.backyardbrains.filters.UsbSerialFilterSettingsDialog;
import com.backyardbrains.utils.Func;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.view.HeartbeatView;
import com.backyardbrains.view.SlidingView;
import com.backyardbrains.view.ThresholdHandle;
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
    private static final int AVERAGED_SAMPLE_COUNT = 30;

    private static final String BOOL_THRESHOLD_ON = "bb_threshold_on";
    private static final String INT_AVERAGING_TRIGGER_TYPE = "bb_averaging_trigger_type";

    @BindView(R.id.threshold_handle) ThresholdHandle thresholdHandle;
    @BindView(R.id.ibtn_threshold) ImageButton ibtnThreshold;
    @BindView(R.id.ibtn_avg_trigger_type) ImageButton ibtnAvgTriggerType;
    @BindView(R.id.ibtn_filters) ImageButton ibtnFilters;
    @BindView(R.id.ibtn_usb) protected ImageButton ibtnUsb;
    @BindView(R.id.ibtn_record) protected ImageButton ibtnRecord;
    @BindView(R.id.tv_stop_recording) protected TextView tvStopRecording;
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

    // Whether signal triggering is turned on or off
    private boolean thresholdOn;
    // Holds the type of triggering that is used when averaging
    private @SignalAveragingTriggerType int triggerType = SignalAveragingTriggerType.THRESHOLD;

    final SetThresholdHandlePositionRunnable setThresholdHandlePositionRunnable =
        new SetThresholdHandlePositionRunnable();
    final UpdateDataProcessorThresholdRunnable updateDataProcessorThresholdRunnable =
        new UpdateDataProcessorThresholdRunnable();

    /**
     * Runnable that is executed on the UI thread every time threshold position is changed.
     */
    private class SetThresholdHandlePositionRunnable implements Runnable {

        private int position;

        @Override public void run() {
            setThresholdHandlePosition(position);
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }

    /**
     * Runnable that is executed on the UI thread every time threshold value is changed.
     */
    private class UpdateDataProcessorThresholdRunnable implements Runnable {

        private float value;

        @Override public void run() {
            updateDataProcessorThreshold(value);
        }

        public void setValue(float value) {
            this.value = value;
        }
    }

    //==============================================
    // EVENT LISTENERS
    //==============================================

    private final FilterSettingsDialog.FilterSelectionListener filterSelectionListener =
        new FilterSettingsDialog.FilterSelectionListener() {
            @Override public void onFilterSelected(@NonNull Filter filter) {
                setFilter(filter);
            }
        };

    private final View.OnClickListener startThresholdOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            startThresholdMode();
            setupThresholdView();

            // update BPM UI
            updateBpmUI();
        }
    };

    private final View.OnClickListener stopThresholdOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            stopThresholdMode();
            setupThresholdView();

            // update BPM UI
            updateBpmUI();
        }
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

    private final View.OnClickListener changeAveragingTriggerTypeOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            openAveragingTriggerTypeDialog();
        }
    };

    private final ThresholdHandle.OnThresholdChangeListener thresholdChangeListener =
        new ThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                getRenderer().adjustThreshold(y);
            }
        };

    private final SlidingView.AnimationEndListener recordAnimationListener = new SlidingView.AnimationEndListener() {
        @Override public void onShowAnimationEnd() {
            thresholdHandle.setTopOffset(ibtnThreshold.getHeight() + tvStopRecording.getHeight());
        }

        @Override public void onHideAnimationEnd() {
            thresholdHandle.setTopOffset(ibtnThreshold.getHeight());
        }
    };

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
            thresholdOn = savedInstanceState.getBoolean(BOOL_THRESHOLD_ON);
            triggerType = savedInstanceState.getInt(INT_AVERAGING_TRIGGER_TYPE, SignalAveragingTriggerType.THRESHOLD);
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

        setupButtons(false);
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");

        if (getAudioService() != null) getAudioService().stopActiveInputSource();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BOOL_THRESHOLD_ON, thresholdOn);
        outState.putInt(INT_AVERAGING_TRIGGER_TYPE, triggerType);
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

        // we should set averaged sample count before UI setup
        int averagedSampleCount = PrefUtils.getAveragedSampleCount(view.getContext(), BaseWaveformFragment.class);
        if (averagedSampleCount < 0) averagedSampleCount = AVERAGED_SAMPLE_COUNT;
        JniUtils.setAveragedSampleCount(averagedSampleCount);

        setupUI();

        return view;
    }

    @Override protected BaseWaveformRenderer createRenderer() {
        final WaveformRenderer renderer = new WaveformRenderer(this);
        renderer.setOnDrawListener(new BaseWaveformRenderer.OnDrawListener() {

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
        renderer.setOnThresholdChangeListener(new WaveformRenderer.OnThresholdChangeListener() {

            @Override public void onThresholdPositionChange(final int position) {
                if (getActivity() != null) {
                    setThresholdHandlePositionRunnable.setPosition(position);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(setThresholdHandlePositionRunnable);
                }
            }

            @Override public void onThresholdValueChange(final float value) {
                if (getActivity() != null) {
                    updateDataProcessorThresholdRunnable.setValue(value);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(updateDataProcessorThresholdRunnable);
                }
            }
        });
        renderer.setSignalAveraging(thresholdOn);
        renderer.setAveragingTriggerType(triggerType);
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
        return getAudioService() != null && getAudioService().getDeviceCount() > 0;
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        // this will start microphone if we are coming from background
        if (getAudioService() != null) startActiveInput(getAudioService());

        // setup threshold button
        setupThresholdView();
        // setup filters button
        setupFiltersButton();
        // setup USB button
        setupUsbButton();
        // setup BPM UI
        updateBpmUI();
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingStartedEvent(AudioRecordingStartedEvent event) {
        setupButtons(true);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioRecordingProgressEvent(AudioRecordingProgressEvent event) {
        stringBuilder.delete(tapToStopLength, stringBuilder.length());
        stringBuilder.append(WavUtils.formatWavProgress((int) event.getProgress(), event.getSampleRate()));
        tvStopRecording.setText(stringBuilder);
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
        // update BPM label
        updateBpmUI();
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
        // threshold button
        ViewUtils.playAfterNextLayout(ibtnThreshold, new Func<View, Void>() {
            @Nullable @Override public Void apply(@Nullable View source) {
                thresholdHandle.setTopOffset(ibtnThreshold.getHeight());
                return null;
            }
        });
        setupThresholdView();
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
        stopRecButton = new SlidingView(tvStopRecording, recordAnimationListener);
        tvStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (getAudioService() != null) getAudioService().stopRecording();
            }
        });
        // set initial visibility
        setupButtons(false);
        // BPM UI
        updateBpmUI();
        if (getContext() != null) tbSound.setChecked(PrefUtils.getBpmSound(getContext()));
        tbSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                vHeartbeat.setMuteSound(!b);
                if (getContext() != null) PrefUtils.setBpmSound(getContext(), b);
            }
        });
    }

    //==============================================
    // THRESHOLD
    //==============================================

    // Sets up the threshold view (threshold on/off button, threshold handle, averaged sample count and averaging trigger type button)
    void setupThresholdView() {
        if (thresholdOn) {
            // setup threshold button
            ibtnThreshold.setImageResource(R.drawable.ic_threshold_off);
            ibtnThreshold.setOnClickListener(stopThresholdOnClickListener);
            // setup averaged sample count progress bar
            sbAvgSamplesCount.setVisibility(View.VISIBLE);
            sbAvgSamplesCount.setOnSeekBarChangeListener(averagedSampleCountChangeListener);
            sbAvgSamplesCount.setProgress(JniUtils.getAveragedSampleCount());
            // setup averaged sample count text view
            tvAvgSamplesCount.setVisibility(View.VISIBLE);
        } else {
            // setup threshold button
            ibtnThreshold.setImageResource(R.drawable.ic_threshold);
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
            // setup threshold handle
            thresholdHandle.setVisibility(
                triggerType == SignalAveragingTriggerType.THRESHOLD ? View.VISIBLE : View.INVISIBLE);
            thresholdHandle.setOnHandlePositionChangeListener(thresholdChangeListener);
            // setup averaging trigger type button
            ibtnAvgTriggerType.setVisibility(View.VISIBLE);
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
            // setup threshold handle
            thresholdHandle.setVisibility(View.INVISIBLE);
            thresholdHandle.setOnHandlePositionChangeListener(null);
            // setup averaging trigger type button
            ibtnAvgTriggerType.setVisibility(View.INVISIBLE);
            ibtnAvgTriggerType.setOnClickListener(null);
        }
    }

    // Starts the threshold mode
    void startThresholdMode() {
        thresholdOn = true;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);

        getRenderer().onSaveSettings(ibtnThreshold.getContext());
        getRenderer().setSignalAveraging(thresholdOn);
        getRenderer().onLoadSettings(ibtnThreshold.getContext());
    }

    // Stops the threshold mode
    void stopThresholdMode() {
        thresholdOn = false;
        if (getAudioService() != null) getAudioService().setSignalAveraging(thresholdOn);

        getRenderer().onSaveSettings(ibtnThreshold.getContext());
        getRenderer().setSignalAveraging(thresholdOn);
        getRenderer().onLoadSettings(ibtnThreshold.getContext());

        // threshold should be reset every time it's enabled so let's reset every time on closing
        JniUtils.resetThreshold();
    }

    // Sets the specified value for the threshold handle.
    void setThresholdHandlePosition(int value) {
        // can be null if callback is called after activity has finished
        if (thresholdHandle != null) thresholdHandle.setPosition(value);
    }

    // Updates data processor with the newly set threshold.
    void updateDataProcessorThreshold(float value) {
        JniUtils.setThreshold((int) value);
    }

    // Opens a dialog for averaging trigger type selection
    void openAveragingTriggerTypeDialog() {
        if (getContext() != null) {
            MaterialDialog averagingTriggerTypeDialog =
                new MaterialDialog.Builder(getContext()).items(R.array.options_averaging_trigger_type)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                            setTriggerType(position == 0 ? SignalAveragingTriggerType.THRESHOLD
                                : position == 10 ? SignalAveragingTriggerType.ALL_EVENTS : position);
                        }
                    })
                    .build();
            averagingTriggerTypeDialog.show();
        }
    }

    // Sets the specified trigger type as the prefered averaging trigger type
    void setTriggerType(@SignalAveragingTriggerType int triggerType) {
        this.triggerType = triggerType;

        setupThresholdHandleAndAveragingTriggerTypeButtons();

        JniUtils.setAveragingTriggerType(triggerType);
        getRenderer().setAveragingTriggerType(triggerType);
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
            || getAudioService().isAmModulationDetected()) && ObjectUtils.equals(getAudioService().getFilter(),
            Filters.FILTER_HEART);
    }

    //==============================================
    // FILTERS
    //==============================================

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
        if (getContext() != null && getAudioService() != null) {
            filterSettingsDialog =
                getAudioService().isAmModulationDetected() ? new AmModulationFilterSettingsDialog(getContext(),
                    filterSelectionListener)
                    : getAudioService().isActiveUsbInputOfType(SpikerBoxHardwareType.MUSCLE_PRO)
                        ? new UsbMuscleProFilterSettingsDialog(getContext(), filterSelectionListener)
                        : getAudioService().isActiveUsbInputOfType(SpikerBoxHardwareType.NEURON_PRO)
                            ? new UsbNeuronProFilterSettingsDialog(getContext(), filterSelectionListener)
                            : new UsbSerialFilterSettingsDialog(getContext(), filterSelectionListener);
            filterSettingsDialog.show(
                getAudioService().getFilter() != null ? getAudioService().getFilter() : new Filter());
        }
    }

    // Sets a filter that should be applied while processing incoming data
    void setFilter(@NonNull Filter filter) {
        if (getAudioService() != null) getAudioService().setFilter(filter);

        // update BPM UI
        updateBpmUI();
    }

    //==============================================
    // USB
    //==============================================

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
                    connectWithDevice();
                }
            });
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

    //==============================================
    // RECORDING
    //==============================================

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
    // INPUT SOURCES AND MAX PROCESSING TIMES
    //==============================================

    private void startActiveInput(@NonNull AudioService audioService) {
        audioService.setSignalAveraging(thresholdOn);
        audioService.startActiveInputSource();

        // resume the threshold in case it was paused during playback
        JniUtils.resumeThreshold();
    }

    private void startMicrophone(@NonNull AudioService audioService) {
        audioService.startMicrophone();
    }

    // Triggers
    private void startUsb(@NonNull AudioService audioService, @NonNull String deviceName) {
        audioService.startUsb(deviceName);
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
