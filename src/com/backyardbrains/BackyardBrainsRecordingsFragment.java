package com.backyardbrains;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.utls.ApacheCommonsLang3Utils;
import com.backyardbrains.utls.DateUtils;
import com.backyardbrains.utls.EventUtils;
import com.backyardbrains.utls.ViewUtils;
import com.backyardbrains.utls.WaveUtils;
import com.backyardbrains.view.BybEmptyRecyclerView;
import com.backyardbrains.view.BybEmptyView;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BackyardBrainsRecordingsFragment extends Fragment {

    public static final String TAG = makeLogTag(BackyardBrainsMain.class);

    @BindView(R.id.rv_files) BybEmptyRecyclerView rvFiles;
    @BindView(R.id.empty_view) BybEmptyView emptyView;
    @BindView(R.id.btn_privacy_policy) Button btnPrivacyPolicy;

    private Context context;
    private Unbinder unbinder;

    private File bybDirectory;
    private FilesAdapter adapter;

    // -------------------------------------------------------------------------------------------------
    // ----------------------------------------- CONSTRUCTOR
    // -------------------------------------------------------------------------------------------------
    public BackyardBrainsRecordingsFragment() {
        super();
    }

    @Override public Context getContext() {
        if (context == null) {
            FragmentActivity act = getActivity();
            if (act == null) {
                return null;
            }
            context = act.getApplicationContext();
        }
        return context;
    }

    // -------------------------------------------------------------------------------------------------
    // ----------------------------------------- FRAGMENT LIFECYCLE
    // -------------------------------------------------------------------------------------------------
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bybDirectory = new File(Environment.getExternalStorageDirectory() + "/BackyardBrains/");

        getContext();
        registerReceivers();
    }

    // ----------------------------------------------------------------------------------------
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onDestroy() {
        bybDirectory = null;
        unregisterReceivers();
        super.onDestroy();
    }

    // -------------------------------------------------------------------------------------------------
    // ----------------------------------------- LIST TASKS
    // -------------------------------------------------------------------------------------------------

    // Rescans BYB directory and updates the files list.
    private void rescanFiles() {
        final File[] files = bybDirectory.listFiles();
        if (files != null) {
            if (files.length > 0) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override public int compare(File object1, File object2) {
                        return (int) (object2.lastModified() - object1.lastModified());
                    }
                });
            }

            LOGD(TAG, "RESCAN FILES!!!!!");
            adapter.setFiles(files);
        }
    }

    // ----------------------------------------------------------------------------------------
    @Nullable private BYBAnalysisManager getAnalysisManger() {
        if (getContext() != null) {
            return ((BackyardBrainsApplication) getContext()).getAnalysisManager();
        }
        return null;
    }

    private boolean isFindSpikesDone() {
        return getAnalysisManger() != null && getAnalysisManger().spikesFound();
    }

    private void fileDetails(File f) {
        String details = "File name: " + f.getName() + "\n";
        details += "Full path: \n" + f.getAbsolutePath() + "\n";
        details += "Duration: " + WaveUtils.getWaveLengthString(f.length());
        BYBUtils.showAlert(getActivity(), "File details", details);
    }

    // ----------------------------------------------------------------------------------------
    private void playAudioFile(File f) {
        //Log.d(TAG, "----------------playAudioFile------------------");
        if (getContext() != null) {
            Intent i = new Intent();
            i.setAction("BYBPlayAudioFile");
            i.putExtra("filePath", f.getAbsolutePath());
            context.sendBroadcast(i);
        }
    }

    // ----------------------------------------------------------------------------------------
    private void findSpikes(File f) {
        if (getContext() != null) {
            if (f.exists()) {
                Intent i = new Intent();
                i.setAction("BYBAnalizeFile");
                i.putExtra("filePath", f.getAbsolutePath());
                context.sendBroadcast(i);
                Intent j = new Intent();
                j.setAction("BYBChangePage");
                j.putExtra("page", BackyardBrainsMain.FIND_SPIKES_VIEW);
                context.sendBroadcast(j);
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    private void autocorrelation(File f) {
        doAnalysis(f, "doAutoCorrelation", "AutoCorrelation");
    }

    // ----------------------------------------------------------------------------------------
    private void ISI(File f) {
        doAnalysis(f, "doISI", "ISI");
    }

    // ----------------------------------------------------------------------------------------
    private void crossCorrelation(File f) {
        doAnalysis(f, "doCrossCorrelation", "CrossCorrelation");
    }

    // ----------------------------------------------------------------------------------------
    private void averageSpike(File f) {
        doAnalysis(f, "doAverageSpike", "AverageSpike");
    }

    // ----------------------------------------------------------------------------------------
    private void doAnalysis(File f, String process, String render) {
        //noinspection ConstantConditions
        if (isFindSpikesDone() && getAnalysisManger().checkCurrentFilePath(f.getAbsolutePath())) {
            if (getContext() != null) {
                if (f.exists()) {
                    Intent j = new Intent();
                    j.setAction("BYBChangePage");
                    j.putExtra("page", BackyardBrainsMain.ANALYSIS_VIEW);
                    context.sendBroadcast(j);
                    Intent i = new Intent();
                    i.setAction("BYBAnalizeFile");
                    i.putExtra("filePath", f.getAbsolutePath());
                    i.putExtra(process, true);
                    context.sendBroadcast(i);
                    Intent k = new Intent();
                    k.setAction("BYBRenderAnalysis");
                    k.putExtra(render, true);
                    context.sendBroadcast(k);
                }
            }
        } else {
            BYBUtils.showAlert(getActivity(), getString(R.string.find_spikes_not_done_title),
                getString(R.string.find_spikes_not_done_message));
        }
    }

    // ----------------------------------------------------------------------------------------
    private void emailFile(File f) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "My BackyardBrains Recording");
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + f.getAbsolutePath()));
        sendIntent.setType("audio/wav");
        startActivity(Intent.createChooser(sendIntent, "Email file"));
    }

    // ----------------------------------------------------------------------------------------
    protected void renameFile(final File f) {
        final EditText e = new EditText(this.getActivity());
        e.setText(f.getName().replace(".wav", "")); // remove file extension when renaming
        e.setSelection(e.getText().length());
        new AlertDialog.Builder(this.getActivity()).setTitle("Rename File")
            .setMessage("Please enter the new name for your file.")
            .setView(e)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (f.exists()) {
                        final String filename = e.getText().toString().trim();
                        // validate the new file name
                        if (ApacheCommonsLang3Utils.isBlank(filename)) {
                            ViewUtils.toast(getContext(), getString(R.string.error_message_validation_file_name));
                            return;
                        }
                        final File newFile = new File(f.getParent(), filename + ".wav");
                        // validate if file with specified name already exists
                        if (!newFile.exists()) {
                            // rename the file
                            if (!f.renameTo(newFile)) {
                                ViewUtils.toast(getContext(), getString(R.string.error_message_files_rename));
                                EventUtils.logCustom("Renaming file " + f.getPath() + " failed", null);
                            }
                        } else {
                            ViewUtils.toast(getContext(), getString(R.string.error_message_files_exists));
                        }
                    } else {
                        ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
                        EventUtils.logCustom("File " + f.getPath() + " doesn't exist", null);
                    }
                    // rescan the files
                    rescanFiles();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show();
    }

    // ----------------------------------------------------------------------------------------
    protected void deleteFile(final File f) {
        new AlertDialog.Builder(this.getActivity()).setTitle("Delete File")
            .setMessage("Are you sure you want to delete " + f.getName() + "?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // validate if the specified file already exists
                    if (f.exists()) {
                        // delete the file
                        if (!f.delete()) {
                            ViewUtils.toast(getContext(), getString(R.string.error_message_files_delete));
                            EventUtils.logCustom("Deleting file " + f.getPath() + " failed", null);
                        }
                    } else {
                        ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
                        EventUtils.logCustom("File " + f.getPath() + " doesn't exist", null);
                    }
                    rescanFiles();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show();
    }

    // Initializes user interface
    private void setupUI() {
        ((BackyardBrainsMain) getActivity()).showButtons(true);

        adapter = new FilesAdapter(context, null, new FilesAdapter.Callback() {
            @Override public void onClick(@NonNull File file) {
                showRecordingOptions(file);
            }
        });
        rvFiles.setAdapter(adapter);
        rvFiles.setEmptyView(emptyView);
        rvFiles.setHasFixedSize(true);
        rvFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFiles.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        btnPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent browserIntent =
                    new Intent(Intent.ACTION_VIEW, Uri.parse("http://backyardbrains.com/about/privacy"));
                getActivity().startActivity(browserIntent);
            }
        });

        rescanFiles();
        // update empty view to show tagline instead of progress bar
        emptyView.setEmpty();
        emptyView.setTagline(getString(R.string.no_files_found));
    }

    // Opens available options for a selected recording. Options are different depending on whether spikes have already
    // been found or not.
    void showRecordingOptions(@NonNull final File file) {
        //noinspection ConstantConditions
        final boolean canAnalyze =
            isFindSpikesDone() && getAnalysisManger().checkCurrentFilePath(file.getAbsolutePath());
        new AlertDialog.Builder(this.getActivity()).setTitle("Choose an action")
            .setCancelable(true)
            .setItems(canAnalyze ? R.array.options_recording : R.array.options_recording_no_spikes,
                new DialogInterface.OnClickListener() {

                    @Override public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                fileDetails(file);
                                break;
                            case 1:
                                playAudioFile(file);
                                break;
                            case 2:
                                findSpikes(file);
                                break;
                            case 3:
                                if (canAnalyze) {
                                    autocorrelation(file);
                                } else {
                                    emailFile(file);
                                }
                                break;
                            case 4:
                                if (canAnalyze) {
                                    ISI(file);
                                } else {
                                    renameFile(file);
                                }
                                break;
                            case 5:
                                if (canAnalyze) {
                                    crossCorrelation(file);
                                } else {
                                    deleteFile(file);
                                }
                                break;
                            case 6:
                                averageSpike(file);
                                break;
                            case 7:
                                emailFile(file);
                                break;
                            case 8:
                                renameFile(file);
                                break;
                            case 9:
                                deleteFile(file);
                                break;
                        }
                    }
                })
            .create()
            .show();
    }

    // ----------------------------------------------------------------------------------------

    // ----------------------------------------------------------------------------------------
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

        void setFiles(@NonNull File[] files) {
            this.files = Arrays.asList(files);
            notifyDataSetChanged();
        }

        @Override public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FileViewHolder(inflater.inflate(R.layout.item_recording, parent, false), callback);
        }

        @Override public void onBindViewHolder(FileViewHolder holder, int position) {
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

            FileViewHolder(View view, final Callback callback) {
                super(view);
                ButterKnife.bind(this, view);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (callback != null) callback.onClick(file);
                    }
                });
            }

            void setFile(@NonNull File file) {
                this.file = file;
                LOGD(TAG, "Binding file " + file.getName());

                tvFileName.setText(file.getName());
                tvFileSize.setText(WaveUtils.getWaveLengthString(file.length()));
                tvFileLasModified.setText(DateUtils.format_MMM_d_yyyy_HH_mm_a(new Date(file.lastModified())));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS INSTANCES
    // ---------------------------------------------------------------------------------------------
    private ToggleRecordingListener toggleRecorder;
    private FileReadReceiver readReceiver;
    private SuccessfulSaveListener successfulSaveListener;
    private RescanFilesListener rescanFilesListener;

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    // ---------------------------------------------------------------------------------------------
    private class ToggleRecordingListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            rescanFiles();
        }
    }

    private class FileReadReceiver extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
        }
    }

    private class SuccessfulSaveListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            rescanFiles();
        }
    }

    private class RescanFilesListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            rescanFiles();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS TOGGLES
    // ---------------------------------------------------------------------------------------------
    private void registerSuccessfulSaveReceiver(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBRecordingSaverSuccessfulSave");
                successfulSaveListener = new SuccessfulSaveListener();
                context.registerReceiver(successfulSaveListener, intentFilter);
            } else {
                context.unregisterReceiver(successfulSaveListener);
            }
        }
    }

    private void registerRecordingToggleReceiver(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBToggleRecording");
                toggleRecorder = new ToggleRecordingListener();
                context.registerReceiver(toggleRecorder, intentFilter);
            } else {
                context.unregisterReceiver(toggleRecorder);
            }
        }
    }

    private void registerFileReadReceiver(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter fileReadIntent = new IntentFilter("BYBFileReadIntent");
                readReceiver = new FileReadReceiver();
                context.registerReceiver(readReceiver, fileReadIntent);
            } else {
                context.unregisterReceiver(readReceiver);
            }
        }
    }

    private void registerRescanFilesReceiver(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intent = new IntentFilter("BYBRescanFiles");
                rescanFilesListener = new RescanFilesListener();
                context.registerReceiver(rescanFilesListener, intent);
            } else {
                context.unregisterReceiver(rescanFilesListener);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // ----------------------------------------- REGISTER RECEIVERS
    // ---------------------------------------------------------------------------------------------
    public void registerReceivers() {
        LOGD(TAG, "registerReceivers");

        registerRecordingToggleReceiver(true);
        registerFileReadReceiver(true);
        registerSuccessfulSaveReceiver(true);
        registerRescanFilesReceiver(true);
    }

    public void unregisterReceivers() {
        LOGD(TAG, "unregisterReceivers");

        registerRecordingToggleReceiver(false);
        registerFileReadReceiver(false);
        registerSuccessfulSaveReceiver(false);
        registerRescanFilesReceiver(false);
    }
}
