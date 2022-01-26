package com.backyardbrains.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.backyardbrains.BuildConfig;
import com.backyardbrains.R;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.dsp.audio.BaseAudioFile;
import com.backyardbrains.dsp.audio.WavAudioFile;
import com.backyardbrains.events.OpenRecordingAnalysisEvent;
import com.backyardbrains.events.OpenRecordingDetailsEvent;
import com.backyardbrains.events.OpenRecordingsEvent;
import com.backyardbrains.events.PlayAudioFileEvent;
import com.backyardbrains.ui.BaseOptionsFragment.OptionsAdapter.OptionItem;
import com.backyardbrains.utils.AudioConversionUtils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.utils.ViewUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class RecordingOptionsFragment extends BaseOptionsFragment
    implements EasyPermissions.PermissionCallbacks {

    public static final String TAG = makeLogTag(RecordingOptionsFragment.class);

    private static final String ARG_FILE_PATH = "bb_analysis_id";

    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    private static final String NOTIFICATION_CHANNEL_ID = "byb_notification_channel_id";

    private String filePath;

    private enum RecordingOption {
        ID_DETAILS(0), ID_PLAY(1), ID_ANALYSIS(2), ID_SHARE(3), ID_DELETE(4), ID_CONVERT(5);

        private final int id;

        RecordingOption(final int id) {
            this.id = id;
        }

        public int value() {
            return id;
        }

        public static RecordingOption find(int id) {
            for (RecordingOption v : values()) {
                if (v.id == id) return v;
            }

            return null;
        }
    }

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link RecordingOptionsFragment}.
     */
    static RecordingOptionsFragment newInstance(@Nullable String filePath) {
        final RecordingOptionsFragment fragment = new RecordingOptionsFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) filePath = getArguments().getString(ARG_FILE_PATH);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        createOptionsData();

        return view;
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) filePath = savedInstanceState.getString(ARG_FILE_PATH);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_FILE_PATH, filePath);
    }

    //==============================================
    //  ABSTRACT IMPLEMENTATIONS
    //==============================================

    @Override protected String getTitle() {
        return RecordingUtils.getFileNameWithoutExtension(new File(filePath));
    }

    @Override protected View.OnClickListener getBackClickListener() {
        return v -> openRecordingsList();
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Creates and sets base options for the current file
    private void createOptionsData() {
        final String[] optionLabels = getResources().getStringArray(R.array.options_recording);
        final List<OptionItem> options = new ArrayList<>();
        final AudioFile af = BaseAudioFile.create(new File(filePath));
        if (af instanceof WavAudioFile) {
            options.add(new OptionItem(RecordingOption.ID_DETAILS.value(), optionLabels[0], true));
            options.add(new OptionItem(RecordingOption.ID_PLAY.value(), optionLabels[1], true));
            options.add(new OptionItem(RecordingOption.ID_ANALYSIS.value(), optionLabels[2], true));
            options.add(new OptionItem(RecordingOption.ID_SHARE.value(), optionLabels[3], false));
            options.add(new OptionItem(RecordingOption.ID_DELETE.value(), optionLabels[4], false));
        } else {
            options.add(new OptionItem(RecordingOption.ID_DETAILS.value(), optionLabels[0], true));
            options.add(new OptionItem(RecordingOption.ID_CONVERT.value(), optionLabels[5], false));
            options.add(new OptionItem(RecordingOption.ID_SHARE.value(), optionLabels[3], false));
            options.add(new OptionItem(RecordingOption.ID_DELETE.value(), optionLabels[4], false));
        }

        setOptions(options, (id, name) -> execOption(id));
    }

    // Opens recordings list view
    private void openRecordingsList() {
        EventBus.getDefault().post(new OpenRecordingsEvent());
    }

    // Executes option for the specified ID
    private void execOption(int id) {
        RecordingOption option = RecordingOption.find(id);
        if (option == null) return;

        switch (option) {
            case ID_DETAILS:
                fileDetails();
                break;
            case ID_PLAY:
                play();
                break;
            case ID_ANALYSIS:
                analysis();
                break;
            case ID_SHARE:
                share();
                break;
            case ID_DELETE:
                deleteFile();
                break;
            case ID_CONVERT:
                convert();
                break;
        }
    }

    // Opens dialog with recording details
    private void fileDetails() {
        EventBus.getDefault().post(new OpenRecordingDetailsEvent(filePath));
    }

    // Starts playing specified audio file
    private void play() {
        EventBus.getDefault().post(new PlayAudioFileEvent(filePath));
    }

    // Opens screen with available analysis
    private void analysis() {
        EventBus.getDefault().post(new OpenRecordingAnalysisEvent(filePath));
    }

    // Initiates sending of the selected recording via email
    private void share() {
        final Context context = getContext();
        final File f = new File(filePath);
        if (context != null) {
            // first check if accompanying events file exists because it needs to be shared as well
            final ArrayList<Uri> uris = new ArrayList<>();
            uris.add(
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", f));
            final File eventsFile = RecordingUtils.getEventFile(f);
            if (eventsFile != null) {
                uris.add(
                    FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider",
                        eventsFile));
            }
            Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "My BackyardBrains Recording");
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            sendIntent.setType("message/rfc822");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.title_share)));
        }
    }

    // Triggers deletion of the selected file
    private void delete() {
        final File f = new File(filePath);
        new AlertDialog.Builder(getContext()).setTitle(getString(R.string.title_delete))
            .setMessage(String.format(getString(R.string.template_delete_file), f.getName()))
            .setPositiveButton(R.string.action_yes, (dialog, which) -> {
                // validate if the specified file already exists
                if (f.exists()) {
                    // get events file before renaming
                    final File ef = RecordingUtils.getEventFile(f);
                    // delete the file
                    if (f.delete()) {
                        // we need to delete events file as well, if it exists
                        if (ef != null && ef.exists()) {
                            // let's delete events file
                            if (!ef.delete()) {
                                BYBUtils.showAlert(getActivity(), getString(R.string.title_error),
                                    getString(R.string.error_message_files_events_delete));
                                FirebaseCrashlytics.getInstance().recordException(new Throwable(
                                    "Deleting events file for the given recording " + f.getPath()
                                        + " failed"));
                            }
                        }
                        // delete db analysis data for the deleted audio file
                        if (getAnalysisManager() != null) {
                            getAnalysisManager().deleteSpikeAnalysis(f.getAbsolutePath());
                        }
                    } else {
                        if (getContext() != null) {
                            ViewUtils.toast(getContext(),
                                getString(R.string.error_message_files_delete));
                        }
                        FirebaseCrashlytics.getInstance().recordException(
                            new Throwable("Deleting file " + f.getPath() + " failed"));
                    }
                } else {
                    if (getContext() != null) {
                        ViewUtils.toast(getContext(),
                            getString(R.string.error_message_files_no_file));
                    }
                    FirebaseCrashlytics.getInstance().recordException(
                        new Throwable("File " + f.getPath() + " doesn't exist"));
                }

                openRecordingsList();
            })
            .setNegativeButton(R.string.action_cancel, null)
            .create()
            .show();
    }

    // Triggers conversion of the M4A file to WAV
    private void convert() {
        final File f = new File(filePath);
        new AlertDialog.Builder(getContext()).setTitle(getString(R.string.title_convert))
            .setMessage(String.format(getString(R.string.template_convert_file), f.getName()))
            .setPositiveButton(R.string.action_yes, (dialog, which) -> {
                // validate if the specified file already exists
                if (f.exists()) {
                    // create recording file
                    final File wavFile = RecordingUtils.createConvertedRecordingFile(f);
                    if (getContext() != null) {
                        // if file with same name and .wav extension already exists
                        // stop and show toast
                        if (wavFile.exists()) {
                            ViewUtils.toast(getContext(),
                                String.format(getString(R.string.error_message_files_exists_2),
                                    f.getName()), Toast.LENGTH_LONG);
                            return;
                        }
                        // start the conversion in background thread
                        new Thread(() -> {
                            final Handler handler = new Handler(Looper.getMainLooper());
                            final MediaFormat oFormat;
                            final Runnable runnable = () -> ViewUtils.toast(getContext(),
                                getString(R.string.error_message_files_convert));
                            try {
                                oFormat = AudioConversionUtils.convertToWav(f, wavFile,
                                    new AudioConversionUtils.ToWavConversionProgressListener() {
                                        @Override public void onConversionProgress(float progress) {
                                            // updates the info with the current progress
                                            handler.post(() -> showInfo(String.format(getString(
                                                R.string.content_desc_conversion_progress),
                                                (int) progress)));
                                        }

                                        @Override public void onConversionComplete() {
                                            // updates the info with the current progress
                                            handler.post(() -> showInfo(String.format(getString(
                                                R.string.content_desc_conversion_progress), 100)));
                                            handler.postDelayed(() -> showInfo(null), 500);
                                            LOGD(TAG, "CONVERSION COMPLETED");
                                        }
                                    });
                                if (oFormat != null) {
                                    int channelCount =
                                        oFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? oFormat
                                            .getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                            : AudioUtils.DEFAULT_CHANNEL_COUNT;
                                    int sampleRate =
                                        oFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                                            ? oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                            : AudioUtils.DEFAULT_SAMPLE_RATE;
                                    int encoding = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                                        && oFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
                                        ? oFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                        : AudioUtils.DEFAULT_ENCODING;
                                    WavAudioFile.save(wavFile, channelCount, sampleRate, encoding);

                                    // delete the original file
                                    if (!f.delete()) {
                                        if (getContext() != null) {
                                            ViewUtils.toast(getContext(), getString(
                                                R.string.error_message_files_convert_delete));
                                        }
                                        FirebaseCrashlytics.getInstance().recordException(new Throwable(
                                            "Deleting file " + f.getPath()
                                                + " after conversion failed"));
                                    }

                                    openRecordingsList();
                                } else {
                                    handler.post(() -> showInfo(null));
                                    handler.post(runnable);
                                    FirebaseCrashlytics.getInstance().recordException(new Throwable(
                                        "Converting " + f.getPath() + " to WAV failed"));
                                }
                            } catch (IOException e) {
                                handler.post(() -> showInfo(null));
                                handler.post(runnable);
                                FirebaseCrashlytics.getInstance().recordException(
                                    new Throwable("Converting " + f.getPath() + " to WAV failed"));
                            }
                        }).start(); // starts the thread by calling the run() method in its Runnable
                    } else {
                        FirebaseCrashlytics.getInstance().recordException(
                            new Throwable("Converting " + f.getPath() + " to WAV failed"));
                    }
                } else {
                    if (getContext() != null) {
                        ViewUtils.toast(getContext(),
                            getString(R.string.error_message_files_no_file));
                    }
                    FirebaseCrashlytics.getInstance().recordException(
                        new Throwable("File " + f.getPath() + " doesn't exist"));
                }
            })
            .setNegativeButton(R.string.action_cancel, null)
            .create()
            .show();
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

    @AfterPermissionGranted(BYB_WRITE_EXTERNAL_STORAGE_PERM) private void deleteFile() {
        if (getContext() != null && EasyPermissions.hasPermissions(getContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (filePath != null) delete();
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this,
                getString(R.string.rationale_write_external_storage_delete),
                BYB_WRITE_EXTERNAL_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
}
