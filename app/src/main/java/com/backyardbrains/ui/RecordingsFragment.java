package com.backyardbrains.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.R;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.dsp.audio.WavAudioFile;
import com.backyardbrains.events.OpenRecordingOptionsEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.DateUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.view.EmptyRecyclerView;
import com.backyardbrains.view.EmptyView;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
public class RecordingsFragment extends BaseFragment implements EasyPermissions.PermissionCallbacks {

    public static final String TAG = makeLogTag(RecordingsFragment.class);

    private static final int BYB_SETTINGS_SCREEN = 126;
    private static final int BYB_READ_EXTERNAL_STORAGE_PERM = 127;

    @BindView(R.id.rv_files) EmptyRecyclerView rvFiles;
    @BindView(R.id.empty_view) EmptyView emptyView;
    @BindView(R.id.btn_privacy_policy) Button btnPrivacyPolicy;

    private Unbinder unbinder;

    private static FilesAdapter adapter;

    static class RescanFilesTask extends AsyncTask<Void, Void, File[]> {

        WeakReference<RecordingsFragment> fragmentRef;

        RescanFilesTask(RecordingsFragment fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override protected File[] doInBackground(Void... voids) {
            final File[] files =
                RecordingUtils.getRecordingsDirectory().listFiles(file -> !RecordingUtils.isEventsFile(file));
            if (files != null) {
                if (files.length > 0) {
                    Arrays.sort(files, (file1, file2) -> {
                        //noinspection UseCompareMethod
                        return Long.valueOf(file2.lastModified()).compareTo(file1.lastModified());
                    });
                }
            }

            return files;
        }

        @Override protected void onPostExecute(File[] files) {
            final RecordingsFragment fragment;
            if ((fragment = fragmentRef.get()) != null) fragment.updateFiles(files);
        }
    }

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link RecordingsFragment}.
     */
    public static RecordingsFragment newInstance() {
        return new RecordingsFragment();
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    @Override public void onStart() {
        super.onStart();

        // scan files to populate recordings list
        scanFiles();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                      Permission Request >= API 23
    //////////////////////////////////////////////////////////////////////////////

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

    @AfterPermissionGranted(BYB_READ_EXTERNAL_STORAGE_PERM) private void scanFiles() {
        if (getContext() != null && EasyPermissions.hasPermissions(getContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE)) {
            rescanFiles();
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_read_external_storage),
                BYB_READ_EXTERNAL_STORAGE_PERM, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    //==============================================
    //  LIST RECORDINGS
    //==============================================

    // Rescans BYB directory and updates the files list.
    void rescanFiles() {
        LOGD(TAG, "RESCAN FILES!!!!!");

        updateEmptyView(true);
        new RescanFilesTask(this).execute();
    }

    void updateFiles(File[] files) {
        LOGD(TAG, "UPDATE FILES (" + files.length + ")!!!!!");

        adapter.setFiles(files);
        updateEmptyView(false);
    }

    //////////////////////////////////////////////////////////////////////////////
    //                         Utility methods
    //////////////////////////////////////////////////////////////////////////////

    // Specified callback is invoked after check that spike analysis for the recording at specified filePath exists or not
    private void spikesAnalysisExists(@NonNull String filePath,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        if (getAnalysisManager() != null) getAnalysisManager().spikesAnalysisExists(filePath, false, callback);
    }

    // Opens Recording options screen
    void openRecordingOptions(@NonNull File f) {
        EventBus.getDefault().post(new OpenRecordingOptionsEvent(f.getAbsolutePath()));
    }

    // Triggers renaming of the selected file
    void renameFile(@NonNull final File f) {
        final EditText e = new EditText(this.getActivity());
        e.setText(f.getName().replace(".wav", "")); // remove file extension when renaming
        e.setSelection(e.getText().length());
        new AlertDialog.Builder(this.getActivity()).setTitle("Rename File")
            .setMessage("Please enter the new name for your file.")
            .setView(e)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                if (f.exists()) {
                    final String filename = e.getText().toString().trim();
                    // validate the new file name
                    if (ApacheCommonsLang3Utils.isBlank(filename)) {
                        if (getContext() != null) {
                            ViewUtils.toast(getContext(), getString(R.string.error_message_validation_file_name));
                        }
                        return;
                    }
                    final File newFile = new File(f.getParent(), filename + ".wav");
                    // validate if file with specified name already exists
                    if (!newFile.exists()) {
                        // get events file before renaming
                        final File ef = RecordingUtils.getEventFile(f);
                        // rename the file
                        if (f.renameTo(newFile)) {
                            // we need to rename events file as well, if it exists
                            if (ef != null) {
                                final File newEventsFile = RecordingUtils.createEventsFile(newFile);
                                if (!newEventsFile.exists()) {
                                    // let's rename events file
                                    if (!ef.renameTo(newEventsFile)) {
                                        BYBUtils.showAlert(getActivity(), "Error",
                                            getString(R.string.error_message_files_events_rename));
                                        Crashlytics.logException(new Throwable(
                                            "Renaming events file for the given recording " + f.getPath() + " failed"));
                                    }
                                }
                            }
                        } else {
                            if (getContext() != null) {
                                ViewUtils.toast(getContext(), getString(R.string.error_message_files_rename));
                            }
                            Crashlytics.logException(new Throwable("Renaming file " + f.getPath() + " failed"));
                        }
                    } else {
                        if (getContext() != null) {
                            ViewUtils.toast(getContext(), getString(R.string.error_message_files_exists));
                        }
                    }
                } else {
                    if (getContext() != null) {
                        ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
                    }
                    Crashlytics.logException(new Throwable("File " + f.getPath() + " doesn't exist"));
                }
                // rescan the files
                rescanFiles();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show();
    }

    // Initializes user interface
    private void setupUI() {
        // update empty view to show loader
        updateEmptyView(true);

        adapter = new FilesAdapter(getContext(), null, this::showRecordingOptions);
        rvFiles.setAdapter(adapter);
        rvFiles.setEmptyView(emptyView);
        rvFiles.setHasFixedSize(true);
        rvFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFiles.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        btnPrivacyPolicy.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://backyardbrains.com/about/privacy"));
            startActivity(browserIntent);
        });
    }

    // Update empty view to loading or empty state
    void updateEmptyView(boolean showLoader) {
        if (showLoader) {
            emptyView.setLoading();
        } else {
            emptyView.setTagline(getString(R.string.no_files_found));
            emptyView.setEmpty();
        }
    }

    // Opens available options for a selected recording. Options are different depending on whether spikes have already
    // been found or not.
    void showRecordingOptions(@NonNull final File file) {
        spikesAnalysisExists(file.getAbsolutePath(), (exists, trainCount) -> {
            openRecordingOptions(file.getAbsoluteFile());
        });
    }

    /**
     * Adapter for listing all the previously recorded files.
     */
    static class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

        private final LayoutInflater inflater;
        private final Callback callback;

        private List<File> files;

        interface Callback {
            void onClick(@NonNull File file);
        }

        FilesAdapter(@NonNull Context context, @Nullable File[] files, @Nullable Callback callback) {
            super();

            this.inflater = LayoutInflater.from(context);
            this.callback = callback;

            if (files != null) this.files = Arrays.asList(files);
        }

        void setFiles(@Nullable File[] files) {
            this.files = files != null ? Arrays.asList(files) : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull @Override public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FileViewHolder(inflater.inflate(R.layout.item_recording, parent, false), callback);
        }

        @Override public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            holder.setFile(files.get(position));
        }

        @Override public int getItemCount() {
            return files != null ? files.size() : 0;
        }

        static class FileViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.tv_file_name) TextView tvFileName;
            @BindView(R.id.tv_file_size) TextView tvFileSize;
            @BindView(R.id.tv_file_last_modified) TextView tvFileLasModified;

            File file;
            Date date = new Date();

            FileViewHolder(View view, final Callback callback) {
                super(view);
                ButterKnife.bind(this, view);

                view.setOnClickListener(v -> {
                    if (callback != null) callback.onClick(file);
                });
            }

            void setFile(@NonNull File file) {
                this.file = file;

                tvFileName.setText(file.getName());
                WavAudioFile waf = null;
                try {
                    waf = new WavAudioFile(file);
                } catch (IOException ignored) {
                }
                tvFileSize.setText(
                    waf != null ? WavUtils.formatWavLength(file.length(), waf.sampleRate(), waf.channelCount())
                        : "UNKNOWN");
                date.setTime(file.lastModified());
                tvFileLasModified.setText(DateUtils.format_MMM_d_yyyy_HH_mm_a(date));
            }
        }
    }
}
