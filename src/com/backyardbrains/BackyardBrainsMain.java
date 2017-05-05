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
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.events.PlayAudioFileEvent;
import com.backyardbrains.utls.PrefUtils;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGI;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsMain extends AppCompatActivity
    implements BackyardBrainsBaseScopeFragment.ResourceProvider, EasyPermissions.PermissionCallbacks {

    private static final String TAG = makeLogTag(BackyardBrainsMain.class);

    public static final int INVALID_VIEW = -1;
    public static final int OSCILLOSCOPE_VIEW = 0;
    public static final int THRESHOLD_VIEW = 1;
    public static final int RECORDINGS_LIST = 2;
    public static final int ANALYSIS_VIEW = 3;
    public static final int FIND_SPIKES_VIEW = 4;
    public static final int PLAY_AUDIO_VIEW = 5;

    //private static final int BACK_STACK_MAX_ITEMS = 2;

    public static final String BYB_RECORDINGS_FRAGMENT = "BackyardBrainsRecordingsFragment";
    public static final String BYB_THRESHOLD_FRAGMENT = "BackyardBrainsThresholdFragment";
    public static final String BYB_SPIKES_FRAGMENT = "BackyardBrainsSpikesFragment";
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

    private boolean bAudioServiceRunning = false;
    protected AudioService mAudioService;
    protected BYBAnalysisManager analysisManager;

    //protected BYBSlidingView sliding_drawer;
    private int currentFrag = -1;

    private boolean bShowScalingInstructions = true;
    private boolean bShowingScalingInstructions = false;

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
        startAudioService();
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
        stopAudioService();
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
        if (bShouldPop) {
            if (!popFragment()) finish();
        }
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
            fragType = RECORDINGS_LIST;
        }
        LOGD(TAG, "loadFragment()  fragType: " + fragType + "  currentFrag: " + currentFrag);
        if (fragType != currentFrag) {
            currentFrag = fragType;
            Fragment frag;
            String fragName;
            switch (fragType) {
                //------------------------------
                case RECORDINGS_LIST:
                    frag = new BackyardBrainsRecordingsFragment();
                    fragName = BYB_RECORDINGS_FRAGMENT;
                    break;
                //------------------------------
                case THRESHOLD_VIEW:
                    frag = new BackyardBrainsThresholdFragment();
                    fragName = BYB_THRESHOLD_FRAGMENT;
                    break;
                //------------------------------
                case FIND_SPIKES_VIEW:
                    frag = new BackyardBrainsSpikesFragment();
                    fragName = BYB_SPIKES_FRAGMENT;
                    break;
                //------------------------------
                case ANALYSIS_VIEW:
                    frag = new BackyardBrainsAnalysisFragment();
                    fragName = BYB_ANALYSIS_FRAGMENT;
                    break;
                //------------------------------
                case OSCILLOSCOPE_VIEW:
                default:
                    frag = new BackyardBrainsOscilloscopeFragment();
                    fragName = BYB_OSCILLOSCOPE_FRAGMENT;
                    break;
                //------------------------------
                case PLAY_AUDIO_VIEW:
                    frag = BackyardBrainsPlayLiveScopeFragment.newInstance();
                    fragName = BYB_PLAY_AUDIO_FRAGMENT;
                    break;
            }

            setSelectedButton(fragType);
            //if (flRecordingsContainer != null && fragType == RECORDINGS_LIST) {
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

    //////////////////////////////////////////////////////////////////////////////
    //                      Private Methods
    //////////////////////////////////////////////////////////////////////////////

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

    private String getFragmentNameFromType(int fragType) {
        switch (fragType) {
            case RECORDINGS_LIST:
                return BYB_RECORDINGS_FRAGMENT;
            case THRESHOLD_VIEW:
                return BYB_THRESHOLD_FRAGMENT;
            case FIND_SPIKES_VIEW:
                return BYB_SPIKES_FRAGMENT;
            case ANALYSIS_VIEW:
                return BYB_ANALYSIS_FRAGMENT;
            case OSCILLOSCOPE_VIEW:
                return BYB_OSCILLOSCOPE_FRAGMENT;
            case PLAY_AUDIO_VIEW:
                return BYB_PLAY_AUDIO_FRAGMENT;
            case INVALID_VIEW:
            default:
                return "";
        }
    }

    private int getFragmentTypeFromName(String fragName) {
        switch (fragName) {
            case BYB_RECORDINGS_FRAGMENT:
                return RECORDINGS_LIST;
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
            case RECORDINGS_LIST:
                selectedButton = R.id.action_recordings;
                i = new Intent();
                i.putExtra("tab", RECORDINGS_LIST);
                break;
            default:
                break;
        }
        if (i != null) {
            bottomMenu.setOnNavigationItemSelectedListener(null);
            bottomMenu.setSelectedItemId(selectedButton);
            bottomMenu.setOnNavigationItemSelectedListener(bottomMenuListener);

            LOGD(TAG, "setSelectedButton: " + getFragmentNameFromType(i.getIntExtra("tab", -1)));
            i.setAction("BYBonTabSelected");
            getApplicationContext().sendBroadcast(i);
        }
    }

    protected void showButtons(boolean bShow) {
        if (bottomMenu != null) bottomMenu.setVisibility(bShow ? View.VISIBLE : View.GONE);
    }

    private void showScalingInstructions() {
        if (bShowScalingInstructions && !bShowingScalingInstructions) {
            bShowingScalingInstructions = true;
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Instructions");
            alertDialog.setMessage(getString(R.string.scaling_instructions));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bShowScalingInstructions = false;
                    bShowingScalingInstructions = false;
                    saveSettings();
                    dialog.dismiss();
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bShowingScalingInstructions = false;
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

    @AfterPermissionGranted(BYB_RECORD_AUDIO_PERM) private void startAudioService() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
            startAnalysisManager();
            startService();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_record_audio), BYB_RECORD_AUDIO_PERM,
                Manifest.permission.RECORD_AUDIO);
        }
    }

    private void stopAudioService() {
        stopAnalysisManager();
        stopService();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Analysis Manager and Audio Service
    //////////////////////////////////////////////////////////////////////////////

    @Nullable @Override public AudioService audioService() {
        return mAudioService;
    }

    @Nullable @Override public BYBAnalysisManager analysisManager() {
        return analysisManager;
    }

    public boolean isAudioServiceRunning() {
        return bAudioServiceRunning;
    }

    public void startService() {
        if (!bAudioServiceRunning) {
            startService(new Intent(this, AudioService.class));
            bAudioServiceRunning = true;
            bindAudioService(true);
        }
    }

    public void stopService() {
        if (bAudioServiceRunning) {
            bindAudioService(false);
            stopService(new Intent(this, AudioService.class));
            bAudioServiceRunning = false;
        }
    }

    public void startAnalysisManager() {
        analysisManager = new BYBAnalysisManager(getApplicationContext());
    }

    public void stopAnalysisManager() {
        analysisManager.close();
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

        private boolean mAudioServiceIsBound;

        // Sets a reference in this activity to the {@link AudioService}, which
        // allows for {@link ByteBuffer}s full of audio information to be passed
        // from the {@link AudioService} down into the local
        // {@link OscilloscopeGLSurfaceView}
        //
        // @see
        // android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
        // android.os.IBinder)
        @Override public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            AudioService.AudioServiceBinder binder = (AudioService.AudioServiceBinder) service;
            mAudioService = binder.getService();
            mAudioServiceIsBound = true;
            // inform interested parties that audio service is successfully connected
            EventBus.getDefault().post(new AudioServiceConnectionEvent(true));
        }

        @Override public void onServiceDisconnected(ComponentName className) {
            mAudioService = null;
            mAudioServiceIsBound = false;

            // inform interested parties that audio service successfully disconnected
            EventBus.getDefault().post(new AudioServiceConnectionEvent(false));
        }
    };

    public AudioService getAudioService() {
        return mAudioService;
    }

    public BYBAnalysisManager getAnalysisManager() {
        return analysisManager;
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onNoSubscriberEvent(NoSubscriberEvent event) {
        // this is here to avoid EventBus exception
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    // ---------------------------------------------------------------------------------------------
    private ChangePageListener changePageListener;
    private AudioPlaybackStartListener audioPlaybackStartListener;

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
    private class AudioPlaybackStartListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            LOGD(TAG, "AudioPlaybackStartListener .onReceive");
            //loadFragment(OSCILLOSCOPE_VIEW);
        }
    }

    private class ShowScalingInstructionsListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            showScalingInstructions();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS TOGGLES
    // -------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    private void registerAudioPlaybackStartReceiver(boolean reg) {
        LOGD(TAG, "registerAudioPlaybackStartReceiver");
        if (reg) {
            IntentFilter intentFilter = new IntentFilter("BYBAudioPlaybackStart");
            audioPlaybackStartListener = new AudioPlaybackStartListener();
            getApplicationContext().registerReceiver(audioPlaybackStartListener, intentFilter);
        } else {
            getApplicationContext().unregisterReceiver(audioPlaybackStartListener);
        }
    }

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
        registerAudioPlaybackStartReceiver(true);
        registerChangePageReceiver(true);
        registerShowScalingInstructionsReceiver(true);
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- UNREGISTER RECEIVERS
    // ---------------------------------------------------------------------------------------------
    public void unregisterReceivers() {
        registerAudioPlaybackStartReceiver(false);
        registerChangePageReceiver(false);
        registerShowScalingInstructionsReceiver(false);
    }

    //////////////////////////////////////////////////////////////////////////////
    //                                 Settings
    //////////////////////////////////////////////////////////////////////////////

    public void loadSettings() {
        bShowScalingInstructions = PrefUtils.isShowScalingInstructions(this, TAG);
    }

    // ----------------------------------------------------------------------------------------
    public void saveSettings() {
        PrefUtils.setShowScalingInstructions(this, TAG, bShowScalingInstructions);
    }
}
