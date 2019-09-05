package com.backyardbrains.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.R;
import com.backyardbrains.events.OpenRecordingOptionsEvent;
import com.backyardbrains.utils.DateUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.view.EmptyRecyclerView;
import com.backyardbrains.view.EmptyView;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.greenrobot.eventbus.EventBus;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class RecordingsFragment extends BaseFragment
    implements EasyPermissions.PermissionCallbacks {

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
            final File[] files = RecordingUtils.getRecordingsDirectory()
                .listFiles(file -> !RecordingUtils.isEventsFile(file));
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

        @Override protected void onPostExecute(@Nullable File[] files) {
            final RecordingsFragment fragment;
            if ((fragment = fragmentRef.get()) != null && fragment.isAdded()) {
                fragment.updateFiles(files);
            }
        }
    }

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link RecordingsFragment}.
     */
    static RecordingsFragment newInstance() {
        return new RecordingsFragment();
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI(view.getContext());

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
            EasyPermissions.requestPermissions(this,
                getString(R.string.rationale_read_external_storage), BYB_READ_EXTERNAL_STORAGE_PERM,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    //==============================================
    //  LIST RECORDINGS
    //==============================================

    // Rescans BYB directory and updates the files list.
    private void rescanFiles() {
        LOGD(TAG, "RESCAN FILES!!!!!");

        updateEmptyView(true);
        new RescanFilesTask(this).execute();
    }

    @SuppressWarnings("WeakerAccess") void updateFiles(@Nullable File[] files) {
        LOGD(TAG, "UPDATE FILES (" + (files != null ? files.length : 0) + ")!!!!!");

        adapter.setFiles(files);
        updateEmptyView(false);
    }

    //////////////////////////////////////////////////////////////////////////////
    //                         Utility methods
    //////////////////////////////////////////////////////////////////////////////

    // Initializes user interface
    private void setupUI(@NonNull Context context) {
        // update empty view to show loader
        updateEmptyView(true);

        adapter = new FilesAdapter(context, null, this::openRecordingOptions);
        rvFiles.setAdapter(adapter);
        rvFiles.setEmptyView(emptyView);
        rvFiles.setHasFixedSize(true);
        rvFiles.setLayoutManager(new LinearLayoutManager(context));
        rvFiles.addItemDecoration(
            new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        btnPrivacyPolicy.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://backyardbrains.com/about/privacy"));
            startActivity(browserIntent);
        });
    }

    // Update empty view to loading or empty state
    private void updateEmptyView(boolean showLoader) {
        if (showLoader) {
            emptyView.setLoading();
        } else {
            emptyView.setTagline(getString(R.string.no_files_found));
            emptyView.setEmpty();
        }
    }

    // Opens available options for a selected recording. Options are different depending on whether spikes have already
    // been found or not.
    private void openRecordingOptions(@NonNull final File file) {
        EventBus.getDefault().post(new OpenRecordingOptionsEvent(file.getAbsolutePath()));
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

        FilesAdapter(@NonNull Context context, @Nullable File[] files,
            @Nullable Callback callback) {
            super();

            this.inflater = LayoutInflater.from(context);
            this.callback = callback;

            if (files != null) this.files = Arrays.asList(files);
        }

        void setFiles(@Nullable File[] files) {
            this.files = files != null ? Arrays.asList(files) : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FileViewHolder(inflater.inflate(R.layout.item_recording, parent, false),
                callback);
        }

        @Override public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            holder.setFile(files.get(position));
        }

        @Override public int getItemCount() {
            return files != null ? files.size() : 0;
        }

        static class FileViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.tv_filename) TextView tvFileName;
            @BindView(R.id.tv_file_duration) TextView tvFileDuration;
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
                final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                long millis = -1;
                try (FileInputStream source = new FileInputStream(file)) {
                    retriever.setDataSource(source.getFD());
                    millis = Long.valueOf(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                } catch (Exception ignored) {
                } finally {
                    retriever.release();
                }
                tvFileDuration.setText(millis < 0 ? "UNKNOWN"
                    : WavUtils.formatWavLength(TimeUnit.MILLISECONDS.toSeconds(millis)));
                date.setTime(file.lastModified());
                tvFileLasModified.setText(DateUtils.format_MMM_d_yyyy_HH_mm_a(date));
            }
        }
    }
}
