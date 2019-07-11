package com.backyardbrains.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.analysis.AnalysisConfig;
import com.backyardbrains.analysis.AnalysisManager;
import com.backyardbrains.analysis.AnalysisType;
import com.backyardbrains.analysis.EventTriggeredAveragesConfig;
import com.backyardbrains.dsp.ProcessingService;
import com.backyardbrains.events.AnalyzeAudioFileEvent;
import com.backyardbrains.events.AnalyzeEventTriggeredAveragesEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.FindSpikesEvent;
import com.backyardbrains.events.OpenRecordingAnalysisEvent;
import com.backyardbrains.events.OpenRecordingDetailsEvent;
import com.backyardbrains.events.OpenRecordingOptionsEvent;
import com.backyardbrains.events.OpenRecordingsEvent;
import com.backyardbrains.events.PlayAudioFileEvent;
import com.backyardbrains.events.ShowToastEvent;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.ImportUtils;
import com.backyardbrains.utils.ImportUtils.ImportResultCode;
import com.backyardbrains.utils.ViewUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGI;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class MainActivity extends AppCompatActivity
    implements BaseFragment.ResourceProvider, EasyPermissions.PermissionCallbacks {

    static final String TAG = makeLogTag(MainActivity.class);

    public static final int INVALID_VIEW = -1;
    public static final int OSCILLOSCOPE_VIEW = 0;
    public static final int RECORDINGS_VIEW = 2;
    public static final int ANALYSIS_VIEW = 3;
    public static final int FIND_SPIKES_VIEW = 4;
    public static final int PLAY_AUDIO_VIEW = 5;
    public static final int RECORDING_OPTIONS_VIEW = 6;
    public static final int RECORDING_DETAILS_VIEW = 7;
    public static final int RECORDING_ANALYSIS_VIEW = 8;
    public static final int EVENT_TRIGGERED_AVERAGES_VIEW = 9;

    public static final String RECORDINGS_FRAGMENT = "RecordingsFragment";
    public static final String SPIKES_FRAGMENT = "FindSpikesFragment";
    public static final String ANALYSIS_FRAGMENT = "AnalysisFragment";
    public static final String OSCILLOSCOPE_FRAGMENT = "OscilloscopeFragment";
    public static final String PLAY_AUDIO_FRAGMENT = "PlaybackScopeFragment";
    public static final String RECORDING_OPTIONS_FRAGMENT = "RecordingOptionsFragment";
    public static final String RECORDING_DETAILS_FRAGMENT = "RecordingDetailsFragment";
    public static final String RECORDING_ANALYSIS_FRAGMENT = "RecordingAnalysisFragment";

    private static final int BYB_RECORD_AUDIO_PERM = 123;
    private static final int BYB_WRITE_STORAGE_PERM = 124;
    private static final int BYB_SETTINGS_SCREEN = 125;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @BindView(R.id.bottom_menu) BottomNavigationView bottomMenu;

    private boolean audioServiceRunning = false;
    ProcessingService processingService;
    private AnalysisManager analysisManager;

    private int currentFrag = -1;

    // Bottom menu navigation listener
    private BottomNavigationView.OnNavigationItemSelectedListener bottomMenuListener = item -> {
        loadFragment(item.getItemId(), false);
        return true;
    };

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG, "onCreate()");

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupUI(savedInstanceState);
    }

    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // use the new intent, not the original one
        setIntent(intent);
    }

    @Override protected void onStart() {
        LOGD(TAG, "onStart()");

        getSupportFragmentManager().addOnBackStackChangedListener(this::printBackStack);

        super.onStart();

        // start AnalysisManager right away cause we might need it for import
        startAnalysisManager();
        // check if we should process file import
        if (getIntent() != null && ImportUtils.checkImport(getIntent())) importRecording();

        // start the processing service for reads mic data, recording and playing recorded files
        start();

        // register activity with event bus
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override protected void onStop() {
        LOGD(TAG, "onStop()");

        // unregister activity from event bus
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);

        super.onStop();

        // stop processing service
        stop();
    }

    //==============================================
    //  OVERRIDES
    //==============================================

    @Override public void onBackPressed() {
        final int fragmentCount = getSupportFragmentManager().getBackStackEntryCount();
        final FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(fragmentCount - 1);
        final BaseFragment frag = (BaseFragment) getSupportFragmentManager().findFragmentByTag(entry.getName());
        if (frag != null && !frag.onBackPressed() && !popFragment()) finish();
    }

    //==============================================
    // FRAGMENT MANAGEMENT
    //==============================================

    public void loadFragment(int fragType, boolean popExisting, Object... args) {
        if (fragType == R.id.action_scope) {
            fragType = OSCILLOSCOPE_VIEW;
        } else if (fragType == R.id.action_recordings) {
            fragType = RECORDINGS_VIEW;
        }
        LOGD(TAG, "loadFragment()  fragType: " + fragType + "  currentFrag: " + currentFrag);
        if (fragType != currentFrag) {
            currentFrag = fragType;
            final BaseFragment frag;
            final String fragName;
            switch (fragType) {
                case RECORDINGS_VIEW:
                    frag = RecordingsFragment.newInstance();
                    fragName = RECORDINGS_FRAGMENT;
                    break;
                case FIND_SPIKES_VIEW:
                    frag = FindSpikesFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = SPIKES_FRAGMENT;
                    break;
                case ANALYSIS_VIEW:
                    frag = AnalysisFragment.newInstance(args.length > 0 ? (Parcelable) args[0] : null);
                    fragName = ANALYSIS_FRAGMENT;
                    break;
                case EVENT_TRIGGERED_AVERAGES_VIEW:
                    frag = AnalysisFragment.newInstance(args.length > 0 ? (Parcelable) args[0] : null);
                    fragName = ANALYSIS_FRAGMENT;
                    break;
                case OSCILLOSCOPE_VIEW:
                default:
                    frag = OscilloscopeFragment.newInstance();
                    fragName = OSCILLOSCOPE_FRAGMENT;
                    break;
                case PLAY_AUDIO_VIEW:
                    frag = PlaybackScopeFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = PLAY_AUDIO_FRAGMENT;
                    break;
                case RECORDING_OPTIONS_VIEW:
                    frag = RecordingOptionsFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = RECORDING_OPTIONS_FRAGMENT;
                    break;
                case RECORDING_DETAILS_VIEW:
                    frag = RecordingDetailsFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = RECORDING_DETAILS_FRAGMENT;
                    break;
                case RECORDING_ANALYSIS_VIEW:
                    frag = RecordingAnalysisFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = RECORDING_ANALYSIS_FRAGMENT;
                    break;
            }
            // Log with Fabric Answers what view did the user opened
            Answers.getInstance()
                .logContentView(new ContentViewEvent().putContentName(fragName).putContentType("Screen View"));

            setSelectedButton(fragType);
            showFragment(frag, fragName, R.id.fragment_container, popExisting, false, R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    public void showFragment(Fragment frag, String fragName, int fragContainer, boolean popExisting, boolean animate,
        int animEnter, int animExit, int animPopEnter, int animPopExit) {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (animate) transaction.setCustomAnimations(animEnter, animExit, animPopEnter, animPopExit);
        final boolean popped = popFragment(fragName);
        if (popped && popExisting) getSupportFragmentManager().popBackStack();
        if (!popped || popExisting) {
            transaction.replace(fragContainer, frag, fragName);
            transaction.addToBackStack(fragName);
            transaction.commit();
        }
    }

    public boolean popFragment(String fragName) {
        boolean popped = false;
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            popped = getSupportFragmentManager().popBackStackImmediate(fragName, 0);
            LOGD(TAG, "popFragment name: " + fragName);
            final int fragType = getFragmentTypeFromName(fragName);
            if (fragType != INVALID_VIEW) {
                LOGD(TAG, "popFragment type: " + fragType);
                setSelectedButton(fragType);
                currentFrag = fragType;
            }
        } else {
            LOGI(TAG, "popFragment noStack");
        }
        return popped;
    }

    public boolean popFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            final int lastFragIndex = getSupportFragmentManager().getBackStackEntryCount() - 2;
            final String lastFragName = getSupportFragmentManager().getBackStackEntryAt(lastFragIndex).getName();
            return popFragment(lastFragName);
        } else {
            LOGI(TAG, "popFragment noStack");
            return false;
        }
    }

    private void printBackStack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            StringBuilder s = new StringBuilder("BackStack:\n");
            for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
                s.append(getSupportFragmentManager().getBackStackEntryAt(i).getName()).append("\n");
            }
            LOGD(TAG, s.toString());
        } else {
            LOGD(TAG, "printBackStack noStack");
        }
    }

    //==============================================
    //  PUBLIC METHODS
    //==============================================

    public boolean isTouchSupported() {
        return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayAudioFileEvent(PlayAudioFileEvent event) {
        loadFragment(PLAY_AUDIO_VIEW, false, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnalyzeEventTriggeredAveragesEvent(AnalyzeEventTriggeredAveragesEvent event) {
        final AnalysisConfig config =
            new EventTriggeredAveragesConfig(event.getFilePath(), AnalysisType.EVENT_TRIGGERED_AVERAGE,
                event.getEvents(), event.isRemoveNoiseIntervals());
        loadFragment(EVENT_TRIGGERED_AVERAGES_VIEW, false, config);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFindSpikesEvent(FindSpikesEvent event) {
        loadFragment(FIND_SPIKES_VIEW, false, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnalyzeAudioFileEvent(AnalyzeAudioFileEvent event) {
        final AnalysisConfig config = new AnalysisConfig(event.getFilePath(), event.getType());
        loadFragment(ANALYSIS_VIEW, false, config);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenRecordingsEvent(OpenRecordingsEvent event) {
        loadFragment(RECORDINGS_VIEW, false);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenRecordingOptionsEvent(OpenRecordingOptionsEvent event) {
        loadFragment(RECORDING_OPTIONS_VIEW, true, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenRecordingDetailsEvent(OpenRecordingDetailsEvent event) {
        loadFragment(RECORDING_DETAILS_VIEW, false, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenRecordingAnalysisEvent(OpenRecordingAnalysisEvent event) {
        loadFragment(RECORDING_ANALYSIS_VIEW, false, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowToastEvent(ShowToastEvent event) {
        ViewUtils.toast(this, event.getToast());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNoSubscriberEvent(NoSubscriberEvent event) {
        // this is here to avoid EventBus exception
    }

    //=================================================
    // PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI(Bundle savedInstanceState) {
        // load initial fragment
        if (null == savedInstanceState) loadFragment(OSCILLOSCOPE_VIEW, false);

        // init bottom menu clicks
        bottomMenu.setOnNavigationItemSelectedListener(bottomMenuListener);
    }

    @SuppressLint("SwitchIntDef") private void showImportError(@ImportResultCode int result) {
        final @StringRes int stringRes;
        switch (result) {
            case ImportResultCode.ERROR_EXISTS:
                stringRes = R.string.error_message_import_exists;
                break;
            case ImportResultCode.ERROR_OPEN:
                stringRes = R.string.error_message_import_open;
                break;
            case ImportResultCode.ERROR_SAVE:
                stringRes = R.string.error_message_import_save;
                break;
            default:
            case ImportResultCode.ERROR:
                stringRes = R.string.error_message_import_error;
                break;
        }

        BYBUtils.showAlert(this, "Error", getString(stringRes));
    }

    private int getFragmentTypeFromName(String fragName) {
        switch (fragName) {
            case RECORDINGS_FRAGMENT:
                return RECORDINGS_VIEW;
            case SPIKES_FRAGMENT:
                return FIND_SPIKES_VIEW;
            case ANALYSIS_FRAGMENT:
                return ANALYSIS_VIEW;
            case OSCILLOSCOPE_FRAGMENT:
                return OSCILLOSCOPE_VIEW;
            case PLAY_AUDIO_FRAGMENT:
                return PLAY_AUDIO_VIEW;
            case RECORDING_OPTIONS_FRAGMENT:
                return RECORDING_OPTIONS_VIEW;
            case RECORDING_DETAILS_FRAGMENT:
                return RECORDING_DETAILS_VIEW;
            case RECORDING_ANALYSIS_FRAGMENT:
                return RECORDING_ANALYSIS_VIEW;
            default:
                return INVALID_VIEW;
        }
    }

    private void setSelectedButton(int select) {
        @IdRes int selectedButton = -1;
        LOGD(TAG, "setSelectedButton");
        switch (select) {
            case OSCILLOSCOPE_VIEW:
                selectedButton = R.id.action_scope;
                break;
            case RECORDINGS_VIEW:
                selectedButton = R.id.action_recordings;
                break;
            default:
                break;
        }
        if (selectedButton != -1) {
            bottomMenu.setOnNavigationItemSelectedListener(null);
            bottomMenu.setSelectedItemId(selectedButton);
            bottomMenu.setOnNavigationItemSelectedListener(bottomMenuListener);
        }
    }

    //==============================================
    // RECORD_AUDIO PERMISSION
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

    @AfterPermissionGranted(BYB_WRITE_STORAGE_PERM) private void importRecording() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ImportUtils.ImportResult result =
                ImportUtils.importRecording(getApplicationContext(), getIntent().getScheme(), getIntent().getData());
            // we don't need intent data anymore
            if (!result.isSuccessful()) {
                showImportError(result.getCode());
            } else {
                ViewUtils.toast(getApplicationContext(), getString(R.string.toast_import_successful),
                    Toast.LENGTH_LONG);
                loadFragment(RECORDINGS_VIEW, false);
                loadFragment(RECORDING_OPTIONS_VIEW, false, result.getFile().getAbsolutePath());
            }
            setIntent(null);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_write_external_storage_import),
                BYB_WRITE_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    /**
     * Requests {@link Manifest.permission#RECORD_AUDIO} permission if it's not already allowed and starts {@link
     * ProcessingService} and {@link AnalysisManager}.
     */
    @AfterPermissionGranted(BYB_RECORD_AUDIO_PERM) private void start() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
            startProcessingService();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_record_audio), BYB_RECORD_AUDIO_PERM,
                Manifest.permission.RECORD_AUDIO);
        }
    }

    /**
     * Stops {@link ProcessingService} and {@link AnalysisManager}. Needs to be called in {@link #onStop()} to release
     * resources.
     */
    private void stop() {
        stopAnalysisManager();
        stopProcessingService();
    }

    //==============================================
    //  PROCESSING SERVICE
    //==============================================

    /**
     * {@inheritDoc}
     */
    @Nullable @Override public ProcessingService processingService() {
        return processingService;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable @Override public AnalysisManager analysisManager() {
        return analysisManager;
    }

    // Starts ProcessingService
    private void startProcessingService() {
        if (!audioServiceRunning) {
            LOGD(TAG, "Starting ProcessingService");

            Intent intent = new Intent(this, ProcessingService.class);
            bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE);
            audioServiceRunning = true;
        }
    }

    // Stops ProcessingService
    private void stopProcessingService() {
        if (audioServiceRunning) {
            LOGD(TAG, "Stopping ProcessingService");

            audioServiceRunning = false;
            unbindService(audioServiceConnection);
        }
    }

    protected ServiceConnection audioServiceConnection = new ServiceConnection() {

        /**
         * Saves a reference to the {@link ProcessingService} through which UI will be able to communication with part
         * of the application in charge of incoming signal processing.
         */
        @Override public void onServiceConnected(ComponentName className, IBinder service) {
            LOGD(TAG, "ProcessingService connected!");
            // we're bound to ProcessingService, cast the IBinder and get ProcessingService instance
            ProcessingService.ServiceBinder binder = (ProcessingService.ServiceBinder) service;
            processingService = binder.getService();

            // inform interested parties that processing service is successfully connected
            EventBus.getDefault().post(new AudioServiceConnectionEvent(true));
        }

        @Override public void onServiceDisconnected(ComponentName className) {
            LOGD(TAG, "ProcessingService disconnected!");

            processingService = null;

            // inform interested parties that processing service successfully disconnected
            EventBus.getDefault().post(new AudioServiceConnectionEvent(false));
        }
    };

    //==============================================
    // ANALYSIS MANAGER
    //==============================================

    // Starts AnalysisManager
    private void startAnalysisManager() {
        if (analysisManager == null) {
            LOGD(TAG, "Starting AnalysisManager");

            analysisManager = new AnalysisManager(getApplicationContext());
        }
    }

    // Stops AnalysisManager
    private void stopAnalysisManager() {
        if (analysisManager != null) {
            LOGD(TAG, "Stopping AnalysisManager");

            analysisManager = null;
        }
    }
}
