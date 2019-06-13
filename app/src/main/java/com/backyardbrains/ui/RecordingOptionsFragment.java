package com.backyardbrains.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.BuildConfig;
import com.backyardbrains.R;
import com.backyardbrains.analysis.AnalysisType;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.events.AnalyzeAudioFileEvent;
import com.backyardbrains.events.FindSpikesEvent;
import com.backyardbrains.events.OpenRecordingDetailsEvent;
import com.backyardbrains.events.OpenRecordingsEvent;
import com.backyardbrains.events.PlayAudioFileEvent;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.utils.ViewUtils;
import com.crashlytics.android.Crashlytics;
import java.io.File;
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
public class RecordingOptionsFragment extends BaseFragment implements EasyPermissions.PermissionCallbacks {

    public static final String TAG = makeLogTag(RecordingOptionsFragment.class);

    private static final String ARG_FILE_PATH = "bb_analysis_id";

    private static final int BYB_SETTINGS_SCREEN = 121;
    private static final int BYB_WRITE_EXTERNAL_STORAGE_PERM = 122;

    @BindView(R.id.ibtn_back) ImageButton ibtnBack;
    @BindView(R.id.tv_filename) TextView tvTextName;
    @BindView(R.id.rv_file_options) RecyclerView rvFileOptions;

    private Unbinder unbinder;

    private OptionsAdapter adapter;

    private String filePath;
    private SparseArray<String> optionsBase;
    private SparseArray<String> optionsNoCrossCorrelation;
    private SparseArray<String> optionsAll;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link RecordingOptionsFragment}.
     */
    public static RecordingOptionsFragment newInstance(@Nullable String filePath) {
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recording_options, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI(view.getContext());

        return view;
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) filePath = savedInstanceState.getString(ARG_FILE_PATH);

        if (filePath != null) {
            spikesAnalysisExists(filePath,
                (analysis, trainCount) -> constructOptions(analysis != null, trainCount > 0));
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_FILE_PATH, filePath);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI(@NonNull Context context) {
        final File file = new File(filePath);

        tvTextName.setText(RecordingUtils.getFileNameWithoutExtension(file));
        ibtnBack.setOnClickListener(v -> openRecordingsList());
        adapter = new OptionsAdapter(context, (id, name) -> execOption(id, file));
        rvFileOptions.setAdapter(adapter);
        rvFileOptions.setHasFixedSize(true);
        rvFileOptions.setLayoutManager(new LinearLayoutManager(context));
        rvFileOptions.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        createOptionsData();
    }

    // Opens recordings list view
    private void openRecordingsList() {
        EventBus.getDefault().post(new OpenRecordingsEvent());
    }

    // Specified callback is invoked after check that spike analysis for file located at specified filePath exists or not
    private void spikesAnalysisExists(@NonNull String filePath,
        @Nullable AnalysisDataSource.SpikeAnalysisCheckCallback callback) {
        if (getAnalysisManager() != null) getAnalysisManager().spikesAnalysisExists(filePath, false, callback);
    }

    // Executes option for the specified ID
    void execOption(int id, @NonNull File file) {
        switch (id) {
            case OptionsAdapter.OptionItem.ID_DETAILS:
                fileDetails();
                break;
            case OptionsAdapter.OptionItem.ID_PLAY:
                play(file);
                break;
            case OptionsAdapter.OptionItem.ID_FIND_SPIKES:
                findSpikes(file);
                break;
            case OptionsAdapter.OptionItem.ID_AUTOCORRELATION:
                autocorrelation(file);
                break;
            case OptionsAdapter.OptionItem.ID_ISI:
                ISI(file);
                break;
            case OptionsAdapter.OptionItem.ID_CROSS_CORRELATION:
                crossCorrelation(file);
                break;
            case OptionsAdapter.OptionItem.ID_AVERAGE_SPIKE:
                averageSpike(file);
                break;
            case OptionsAdapter.OptionItem.ID_SHARE:
                share(file);
                break;
            case OptionsAdapter.OptionItem.ID_DELETE:
                deleteFile();
                break;
        }
    }

    // Opens dialog with recording details
    void fileDetails() {
        EventBus.getDefault().post(new OpenRecordingDetailsEvent(filePath));
    }

    // Starts playing specified audio file
    void play(@NonNull File f) {
        EventBus.getDefault().post(new PlayAudioFileEvent(f.getAbsolutePath()));
    }

    // Start process of finding spikes for the specified audio file
    void findSpikes(@NonNull File f) {
        if (f.exists()) EventBus.getDefault().post(new FindSpikesEvent(f.getAbsolutePath()));
    }

    // Start process of autocorrelation analysis for specified audio file
    void autocorrelation(@NonNull File f) {
        startAnalysis(f, AnalysisType.AUTOCORRELATION);
    }

    // Start process of inter spike interval analysis for specified audio file
    void ISI(@NonNull File f) {
        startAnalysis(f, AnalysisType.ISI);
    }

    // Start process of cross-correlation analysis for specified audio file
    void crossCorrelation(@NonNull File f) {
        startAnalysis(f, AnalysisType.CROSS_CORRELATION);
    }

    // Start process of average spike analysis for specified audio file
    void averageSpike(@NonNull File f) {
        startAnalysis(f, AnalysisType.AVERAGE_SPIKE);
    }

    // Starts analysis process for specified type and specified audio file
    private void startAnalysis(@NonNull final File file, @AnalysisType final int type) {
        spikesAnalysisExists(file.getAbsolutePath(), (analysis, trainCount) -> {
            if (analysis != null) {
                if (file.exists()) {
                    EventBus.getDefault().post(new AnalyzeAudioFileEvent(file.getAbsolutePath(), type));
                }
            } else {
                BYBUtils.showAlert(getActivity(), getString(R.string.find_spikes_not_done_title),
                    getString(R.string.find_spikes_not_done_message));
            }
        });
    }

    // Initiates sending of the selected recording via email
    void share(@NonNull File f) {
        final Context context = getContext();
        if (context != null) {
            // first check if accompanying events file exists because it needs to be shared as well
            final ArrayList<Uri> uris = new ArrayList<>();
            uris.add(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", f));
            final File eventsFile = RecordingUtils.getEventFile(f);
            if (eventsFile != null) {
                uris.add(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", eventsFile));
            }
            Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "My BackyardBrains Recording");
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            sendIntent.setType("message/rfc822");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.title_share)));
        }
    }

    // Triggers deletion of the selected file
    void delete(@NonNull final File f) {
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
                                Crashlytics.logException(new Throwable(
                                    "Deleting events file for the given recording " + f.getPath() + " failed"));
                            }
                        }
                        // delete db analysis data for the deleted audio file
                        if (getAnalysisManager() != null) getAnalysisManager().deleteSpikeAnalysis(f.getAbsolutePath());
                    } else {
                        if (getContext() != null) {
                            ViewUtils.toast(getContext(), getString(R.string.error_message_files_delete));
                        }
                        Crashlytics.logException(new Throwable("Deleting file " + f.getPath() + " failed"));
                    }
                } else {
                    if (getContext() != null) {
                        ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
                    }
                    Crashlytics.logException(new Throwable("File " + f.getPath() + " doesn't exist"));
                }

                openRecordingsList();
            })
            .setNegativeButton(R.string.action_cancel, null)
            .create()
            .show();
    }

    // Creates all possible combination of options for the current file
    private void createOptionsData() {
        // create options for file that hasn't been analyzed yet
        String[] options = getResources().getStringArray(R.array.options_recording_base);
        optionsBase = new SparseArray<>();
        optionsBase.put(OptionsAdapter.OptionItem.ID_DETAILS, options[0]);
        optionsBase.put(OptionsAdapter.OptionItem.ID_PLAY, options[1]);
        optionsBase.put(OptionsAdapter.OptionItem.ID_FIND_SPIKES, options[2]);
        optionsBase.put(OptionsAdapter.OptionItem.ID_SHARE, options[3]);
        optionsBase.put(OptionsAdapter.OptionItem.ID_DELETE, options[4]);
        // create options for file that has only one spike train (cross-correlation not possible)
        options = getResources().getStringArray(R.array.options_recording_no_cross_correlation);
        optionsNoCrossCorrelation = new SparseArray<>();
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_DETAILS, options[0]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_PLAY, options[1]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_FIND_SPIKES, options[2]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_AUTOCORRELATION, options[3]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_ISI, options[4]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_AVERAGE_SPIKE, options[5]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_SHARE, options[6]);
        optionsNoCrossCorrelation.put(OptionsAdapter.OptionItem.ID_DELETE, options[7]);
        // create options for file with all the options
        options = getResources().getStringArray(R.array.options_recording);
        optionsAll = new SparseArray<>();
        optionsAll.put(OptionsAdapter.OptionItem.ID_DETAILS, options[0]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_PLAY, options[1]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_FIND_SPIKES, options[2]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_AUTOCORRELATION, options[3]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_ISI, options[4]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_CROSS_CORRELATION, options[5]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_AVERAGE_SPIKE, options[6]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_SHARE, options[7]);
        optionsAll.put(OptionsAdapter.OptionItem.ID_DELETE, options[8]);
    }

    // Creates and opens recording options dialog
    void constructOptions(final boolean canAnalyze, final boolean showCrossCorrelation) {
        final SparseArray<String> options =
            canAnalyze ? (showCrossCorrelation ? optionsAll : optionsNoCrossCorrelation) : optionsBase;
        adapter.setOptions(options);
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

    @AfterPermissionGranted(BYB_WRITE_EXTERNAL_STORAGE_PERM) void deleteFile() {
        if (getContext() != null && EasyPermissions.hasPermissions(getContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (filePath != null) delete(new File(filePath));
        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_write_external_storage_delete),
                BYB_WRITE_EXTERNAL_STORAGE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    /**
     * Adapter for listing all the available options for the currently file.
     */
    static class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.OptionViewHolder> {

        private final LayoutInflater inflater;
        private final Callback callback;

        private SparseArray<String> options;

        interface Callback {
            void onClick(int id, @NonNull String name);
        }

        OptionsAdapter(@NonNull Context context, @Nullable Callback callback) {
            super();

            this.inflater = LayoutInflater.from(context);
            this.callback = callback;
        }

        void setOptions(@NonNull SparseArray<String> options) {
            this.options = options;
            notifyDataSetChanged();
        }

        @NonNull @Override public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new OptionViewHolder(inflater.inflate(R.layout.item_recording_option, parent, false), callback);
        }

        @Override public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
            final int key = options.keyAt(position);
            holder.setOption(key, options.get(key));
        }

        @Override public int getItemCount() {
            return options != null ? options.size() : 0;
        }

        static class OptionItem {
            static final int ID_DETAILS = 0;
            static final int ID_PLAY = 1;
            static final int ID_FIND_SPIKES = 2;
            static final int ID_AUTOCORRELATION = 3;
            static final int ID_ISI = 4;
            static final int ID_CROSS_CORRELATION = 5;
            static final int ID_AVERAGE_SPIKE = 6;
            static final int ID_SHARE = 7;
            static final int ID_DELETE = 8;
        }

        static class OptionViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.tv_option_name) TextView tvOptionName;

            int id;
            String name;

            OptionViewHolder(@NonNull View view, @Nullable final Callback callback) {
                super(view);
                ButterKnife.bind(this, view);

                view.setOnClickListener(v -> {
                    if (callback != null) callback.onClick(id, name);
                });
            }

            void setOption(int id, @NonNull String name) {
                this.id = id;
                this.name = name;

                tvOptionName.setText(this.name);
            }
        }
    }
}
