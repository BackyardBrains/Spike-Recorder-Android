package com.backyardbrains;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.analysis.BYBAnalysisType;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.events.AnalyzeAudioFileEvent;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.FindSpikesEvent;
import com.backyardbrains.events.OpenRecordingsEvent;
import com.backyardbrains.events.PlayAudioFileEvent;
import com.backyardbrains.utils.PrefUtils;
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

public class BackyardBrainsMain extends AppCompatActivity
    implements BaseFragment.ResourceProvider, EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(BackyardBrainsMain.class);

    public static final int INVALID_VIEW = -1;
    public static final int OSCILLOSCOPE_VIEW = 0;
    public static final int THRESHOLD_VIEW = 1;
    public static final int RECORDINGS_VIEW = 2;
    public static final int ANALYSIS_VIEW = 3;
    public static final int FIND_SPIKES_VIEW = 4;
    public static final int PLAY_AUDIO_VIEW = 5;

    //private static final int BACK_STACK_MAX_ITEMS = 2;

    public static final String BYB_RECORDINGS_FRAGMENT = "BackyardBrainsRecordingsFragment";
    public static final String BYB_THRESHOLD_FRAGMENT = "BackyardBrainsThresholdFragment";
    public static final String BYB_SPIKES_FRAGMENT = "BackyardBrainsSpikesFragment1";
    public static final String BYB_ANALYSIS_FRAGMENT = "BackyardBrainsAnalysisFragment";
    public static final String BYB_OSCILLOSCOPE_FRAGMENT = "BackyardBrainsOscilloscopeFragment";
    public static final String BYB_PLAY_AUDIO_FRAGMENT = "BackyardBrainsPlayAudioFragment";

    private static final int BYB_RECORD_AUDIO_PERM = 123;
    private static final int BYB_SETTINGS_SCREEN = 125;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    //@Nullable @BindView(R.id.fragment_recordings_list) FrameLayout flRecordingsContainer;
    @BindView(R.id.bottom_menu) BottomNavigationView bottomMenu;

    private boolean audioServiceRunning = false;
    protected AudioService audioService;
    protected BYBAnalysisManager analysisManager;

    //protected BYBSlidingView sliding_drawer;
    private int currentFrag = -1;

    private boolean showScalingInstructions = true;
    private boolean showingScalingInstructions = false;

    private enum FragTransaction {
        ADD, REPLACE, REMOVE
    }

    // Bottom menu navigation listener
    private BottomNavigationView.OnNavigationItemSelectedListener bottomMenuListener =
        new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                loadFragment(item.getItemId());
                return true;
            }
        };

    //////////////////////////////////////////////////////////////////////////////
    //                       Lifecycle overrides
    //////////////////////////////////////////////////////////////////////////////

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupUI();
    }

    @Override protected void onStart() {
        // start the audio service for reads mic data, recording and playing recorded files
        start();
        // load settings saved from last session
        loadSettings();
        // registers all broadcast receivers
        registerReceivers();

        super.onStart();
        // register activity with event bus
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override protected void onStop() {
        // unregister activity from event bus
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onStop();

        // unregisters all broadcast receivers
        unregisterReceivers();
        // saves settings set in this session
        saveSettings();
        // stop audio service
        stop();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                       OnKey methods
    //////////////////////////////////////////////////////////////////////////////

    @Override public void onBackPressed() {
        boolean bShouldPop = true;
        if (currentFrag == ANALYSIS_VIEW) {
            Fragment frag = getSupportFragmentManager().findFragmentByTag(BYB_ANALYSIS_FRAGMENT);
            if (frag != null && frag instanceof BackyardBrainsAnalysisFragment) {
                bShouldPop = false;
                ((BackyardBrainsAnalysisFragment) frag).onBackPressed();
            }
        }
        if (bShouldPop && !popFragment()) finish();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Fragment management
    //////////////////////////////////////////////////////////////////////////////

    public void loadFragment(int fragType, Object... args) {
        if (fragType == R.id.action_scope) {
            fragType = OSCILLOSCOPE_VIEW;
        } else if (fragType == R.id.action_threshold) {
            fragType = THRESHOLD_VIEW;
        } else if (fragType == R.id.action_recordings) {
            fragType = RECORDINGS_VIEW;
        }
        LOGD(TAG, "loadFragment()  fragType: " + fragType + "  currentFrag: " + currentFrag);
        if (fragType != currentFrag) {
            currentFrag = fragType;
            Fragment frag;
            String fragName;
            switch (fragType) {
                //------------------------------
                case RECORDINGS_VIEW:
                    frag = BackyardBrainsRecordingsFragment.newInstance();
                    fragName = BYB_RECORDINGS_FRAGMENT;
                    break;
                //------------------------------
                case THRESHOLD_VIEW:
                    frag = BackyardBrainsThresholdFragment.newInstance();
                    fragName = BYB_THRESHOLD_FRAGMENT;
                    break;
                //------------------------------
                case FIND_SPIKES_VIEW:
                    frag = BackyardBrainsSpikesFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = BYB_SPIKES_FRAGMENT;
                    break;
                //------------------------------
                case ANALYSIS_VIEW:
                    frag = BackyardBrainsAnalysisFragment.newInstance(args.length > 0 ? String.valueOf(args[0]) : null,
                        args.length > 0 ? (int) args[1] : BYBAnalysisType.NONE);
                    fragName = BYB_ANALYSIS_FRAGMENT;
                    break;
                //------------------------------
                case OSCILLOSCOPE_VIEW:
                default:
                    frag = BackyardBrainsOscilloscopeFragment.newInstance();
                    fragName = BYB_OSCILLOSCOPE_FRAGMENT;
                    break;
                //------------------------------
                case PLAY_AUDIO_VIEW:
                    frag = BackyardBrainsPlaybackScopeFragment.newInstance(
                        args.length > 0 ? String.valueOf(args[0]) : null);
                    fragName = BYB_PLAY_AUDIO_FRAGMENT;
                    break;
            }

            setSelectedButton(fragType);
            //if (flRecordingsContainer != null && fragType == RECORDINGS_VIEW) {
            //    Intent i = new Intent();
            //    i.setAction("BYBRescanFiles");
            //    getApplicationContext().sendBroadcast(i);
            //} else {
            showFragment(frag, fragName, R.id.fragment_container, FragTransaction.REPLACE, false, R.anim.slide_in_right,
                R.anim.slide_out_left);
            //}
        }
    }

    public boolean popFragment(String fragName) {
        boolean bPopped = false;
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            bPopped = getSupportFragmentManager().popBackStackImmediate(fragName, 0);
            LOGD(TAG, "popFragment name: " + fragName);
            int fragType = getFragmentTypeFromName(fragName);
            if (fragType != INVALID_VIEW) {
                LOGD(TAG, "popFragment type: " + fragType);
                setSelectedButton(fragType);
                currentFrag = fragType;
            }
        } else {
            LOGI(TAG, "popFragment noStack");
        }
        return bPopped;
    }

    public boolean popFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            int lastFragIndex = getSupportFragmentManager().getBackStackEntryCount() - 2;
            String lastFragName = getSupportFragmentManager().getBackStackEntryAt(lastFragIndex).getName();
            return popFragment(lastFragName);
        } else {
            LOGI(TAG, "popFragment noStack");
            return false;
        }
    }

    private void printBackStack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            String s = "BackStack:\n";
            for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
                s += getSupportFragmentManager().getBackStackEntryAt(i).getName() + "\n";
            }
            LOGD(TAG, s);
        } else {
            LOGI(TAG, "printBackStack noStack");
        }
    }

    public void showFragment(Fragment frag, String fragName, int fragContainer, FragTransaction fragTransaction,
        boolean bAnimate, int animIn, int animOut, boolean bAddToBackStack) {
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!popFragment(fragName)) {
            if (bAnimate) {
                transaction.setCustomAnimations(animIn, animOut);
            }
            if (fragTransaction == FragTransaction.REPLACE) {
                transaction.replace(fragContainer, frag, fragName);
                if (bAddToBackStack) {
                    transaction.addToBackStack(fragName);
                }
            } else if (fragTransaction == FragTransaction.REMOVE) {
                transaction.remove(frag);
            } else if (fragTransaction == FragTransaction.ADD) {
                transaction.add(fragContainer, frag, fragName);
                if (bAddToBackStack) {
                    transaction.addToBackStack(fragName);
                }
            }
            transaction.commit();
        }
        printBackStack();
    }

    public void showFragment(Fragment frag, String fragName, int fragContainer, FragTransaction fragTransaction,
        boolean bAnimate, int animIn, int animOut) {
        showFragment(frag, fragName, fragContainer, fragTransaction, bAnimate, animIn, animOut, true);
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Public Methods
    //////////////////////////////////////////////////////////////////////////////

    public boolean isTouchSupported() {
        return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayAudioFileEvent(PlayAudioFileEvent event) {
        loadFragment(PLAY_AUDIO_VIEW, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFindSpikesEvent(FindSpikesEvent event) {
        loadFragment(FIND_SPIKES_VIEW, event.getFilePath());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnalyzeAudioFileEvent(AnalyzeAudioFileEvent event) {
        loadFragment(ANALYSIS_VIEW, event.getFilePath(), event.getType());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenRecordingsEvent(OpenRecordingsEvent event) {
        loadFragment(RECORDINGS_VIEW);
    }

    //=================================================
    // PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI() {
        // we will have recordings container only if in landscape mode
        //if (flRecordingsContainer != null) {
        //    sliding_drawer =
        //        new BYBSlidingView(flRecordingsContainer, this, "recordings sliding drawer", R.anim.slide_in_right,
        //            R.anim.slide_out_right);
        //    showFragment(new BackyardBrainsRecordingsFragment(), BYB_RECORDINGS_FRAGMENT, R.id.fragment_recordings_list,
        //        FragTransaction.REPLACE, false, R.anim.slide_in_right, R.anim.slide_out_right);
        //}
        // init bottom menu clicks
        bottomMenu.setOnNavigationItemSelectedListener(bottomMenuListener);

        // load initial fragment
        loadFragment(OSCILLOSCOPE_VIEW);
    }

    private int getFragmentTypeFromName(String fragName) {
        switch (fragName) {
            case BYB_RECORDINGS_FRAGMENT:
                return RECORDINGS_VIEW;
            case BYB_THRESHOLD_FRAGMENT:
                return THRESHOLD_VIEW;
            case BYB_SPIKES_FRAGMENT:
                return FIND_SPIKES_VIEW;
            case BYB_ANALYSIS_FRAGMENT:
                return ANALYSIS_VIEW;
            case BYB_OSCILLOSCOPE_FRAGMENT:
                return OSCILLOSCOPE_VIEW;
            case BYB_PLAY_AUDIO_FRAGMENT:
                return PLAY_AUDIO_VIEW;
            default:
                return INVALID_VIEW;
        }
    }

    private void setSelectedButton(int select) {
        Intent i = null;
        @IdRes int selectedButton = -1;
        LOGD(TAG, "setSelectedButton");
        switch (select) {
            case OSCILLOSCOPE_VIEW:
                selectedButton = R.id.action_scope;
                i = new Intent();
                i.putExtra("tab", OSCILLOSCOPE_VIEW);
                break;
            case THRESHOLD_VIEW:
                selectedButton = R.id.action_threshold;
                i = new Intent();
                i.putExtra("tab", THRESHOLD_VIEW);
                break;
            case RECORDINGS_VIEW:
                selectedButton = R.id.action_recordings;
                i = new Intent();
                i.putExtra("tab", RECORDINGS_VIEW);
                break;
            default:
                break;
        }
        if (i != null) {
            bottomMenu.setOnNavigationItemSelectedListener(null);
            bottomMenu.setSelectedItemId(selectedButton);
            bottomMenu.setOnNavigationItemSelectedListener(bottomMenuListener);
        }
    }

    protected void showButtons(boolean bShow) {
        if (bottomMenu != null) bottomMenu.setVisibility(bShow ? View.VISIBLE : View.GONE);
    }

    private void showScalingInstructions() {
        if (showScalingInstructions && !showingScalingInstructions) {
            showingScalingInstructions = true;
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Instructions");
            alertDialog.setMessage(getString(R.string.scaling_instructions));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    showScalingInstructions = false;
                    showingScalingInstructions = false;
                    saveSettings();
                    dialog.dismiss();
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    showingScalingInstructions = false;
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        }
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
        if (perms != null && perms.contains(Manifest.permission.RECORD_AUDIO) && requestCode == BYB_RECORD_AUDIO_PERM) {
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
    }

    /**
     * Requests {@link Manifest.permission#RECORD_AUDIO} permission if it's not already allowed and starts {@link
     * AudioService} and {@link BYBAnalysisManager}.
     */
    @AfterPermissionGranted(BYB_RECORD_AUDIO_PERM) private void start() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
            startAnalysisManager();
            startAudioService();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_record_audio), BYB_RECORD_AUDIO_PERM,
                Manifest.permission.RECORD_AUDIO);
        }
    }

    /**
     * Stops {@link AudioService} and {@link BYBAnalysisManager}. Needs to be called in {@link #onStop()} to release
     * resources.
     */
    private void stop() {
        stopAnalysisManager();
        stopAudioService();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Analysis Manager and Audio Service
    //////////////////////////////////////////////////////////////////////////////

    @Nullable @Override public AudioService audioService() {
        return audioService;
    }

    @Nullable @Override public BYBAnalysisManager analysisManager() {
        return analysisManager;
    }

    /**
     * Starts {@link AudioService}.
     */
    public void startAudioService() {
        if (!audioServiceRunning) {
            LOGD(TAG, "Starting AudioService");

            startService(new Intent(this, AudioService.class));
            audioServiceRunning = true;
            bindAudioService(true);
        }
    }

    /**
     * Stops {@link AudioService}.
     */
    public void stopAudioService() {
        if (audioServiceRunning) {
            LOGD(TAG, "Stopping AudioService");

            bindAudioService(false);
            stopService(new Intent(this, AudioService.class));
            audioServiceRunning = false;
        }
    }

    /**
     * Starts {@link BYBAnalysisManager}.
     */
    public void startAnalysisManager() {
        if (analysisManager == null) {
            LOGD(TAG, "Starting AnalysisManager");

            analysisManager = new BYBAnalysisManager();
        }
    }

    /**
     * Stops {@link BYBAnalysisManager}.
     */
    public void stopAnalysisManager() {
        if (analysisManager != null) {
            LOGD(TAG, "Stopping AnalysisManager");

            analysisManager = null;
        }
    }

    protected void bindAudioService(boolean on) {
        if (on) {
            Intent intent = new Intent(this, AudioService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } else {
            unbindService(mConnection);
        }
    }

    protected ServiceConnection mConnection = new ServiceConnection() {

        // Sets a reference in this activity to the {@link AudioService}, which
        // allows for {@link ByteBuffer}s full of audio information to be passed
        // from the {@link AudioService} down into the local
        // {@link OscilloscopeGLSurfaceView}
        //
        // @see
        // android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
        // android.os.IBinder)
        @Override public void onServiceConnected(ComponentName className, IBinder service) {
            LOGD(TAG, "AudioService connected!");
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            AudioService.AudioServiceBinder binder = (AudioService.AudioServiceBinder) service;
            audioService = binder.getService();
            // inform interested parties that audio service is successfully connected
            EventBus.getDefault().post(new AudioServiceConnectionEvent(true));
        }

        @Override public void onServiceDisconnected(ComponentName className) {
            LOGD(TAG, "AudioService disconnected!");

            audioService = null;

            // inform interested parties that audio service successfully disconnected
            EventBus.getDefault().post(new AudioServiceConnectionEvent(false));
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN) public void onNoSubscriberEvent(NoSubscriberEvent event) {
        // this is here to avoid EventBus exception
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    // ---------------------------------------------------------------------------------------------
    private ChangePageListener changePageListener;

    private ShowScalingInstructionsListener showScalingInstructionsListener;

    private class ChangePageListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("page")) {
                if (intent.hasExtra("page")) {
                    loadFragment(intent.getIntExtra("page", 0));
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    private class ShowScalingInstructionsListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            showScalingInstructions();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS TOGGLES
    // -------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------
    private void registerChangePageReceiver(boolean reg) {
        if (reg) {
            IntentFilter intentFilter = new IntentFilter("BYBChangePage");
            changePageListener = new ChangePageListener();
            getApplicationContext().registerReceiver(changePageListener, intentFilter);
        } else {
            getApplicationContext().unregisterReceiver(changePageListener);
        }
    }

    // ----------------------------------------------------------------------------------------
    private void registerShowScalingInstructionsReceiver(boolean reg) {
        if (reg) {
            IntentFilter intentFilter = new IntentFilter("showScalingInstructions");
            showScalingInstructionsListener = new ShowScalingInstructionsListener();
            getApplicationContext().registerReceiver(showScalingInstructionsListener, intentFilter);
        } else {
            getApplicationContext().unregisterReceiver(showScalingInstructionsListener);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- REGISTER RECEIVERS
    // ---------------------------------------------------------------------------------------------
    public void registerReceivers() {
        registerChangePageReceiver(true);
        registerShowScalingInstructionsReceiver(true);
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- UNREGISTER RECEIVERS
    // ---------------------------------------------------------------------------------------------
    public void unregisterReceivers() {
        registerChangePageReceiver(false);
        registerShowScalingInstructionsReceiver(false);
    }

    //////////////////////////////////////////////////////////////////////////////
    //                                 Settings
    //////////////////////////////////////////////////////////////////////////////

    public void loadSettings() {
        showScalingInstructions = PrefUtils.isShowScalingInstructions(this, BackyardBrainsMain.class);
    }

    // ----------------------------------------------------------------------------------------
    public void saveSettings() {
        PrefUtils.setShowScalingInstructions(this, BackyardBrainsMain.class, showScalingInstructions);
    }
}
