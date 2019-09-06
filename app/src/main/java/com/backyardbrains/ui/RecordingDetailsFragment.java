package com.backyardbrains.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.R;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.dsp.audio.BaseAudioFile;
import com.backyardbrains.events.OpenRecordingOptionsEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.DateUtils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.utils.ViewUtils;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.util.Date;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class RecordingDetailsFragment extends BaseFragment {
    public static final String TAG = makeLogTag(RecordingDetailsFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";

    @BindView(R.id.ibtn_back) ImageButton ibtnBack;
    @BindView(R.id.tv_filename) TextView tvFilename;
    @BindView(R.id.et_filename) EditText etFilename;
    @BindView(R.id.ibtn_clear_filename) ImageButton ibtnClearFilename;
    @BindView(R.id.tv_mime_type) TextView tvMimeType;
    @BindView(R.id.tv_recorded_on) TextView tvRecordedOn;
    @BindView(R.id.tv_sampling_rate) TextView tvSamplingRate;
    @BindView(R.id.tv_num_of_channels) TextView tvNumOfChannels;
    @BindView(R.id.tv_bits_per_sample) TextView tvBitsPerSample;
    @BindView(R.id.tv_file_length) TextView tvFileLength;

    private Unbinder unbinder;

    private String filePath;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link RecordingDetailsFragment}.
     */
    static RecordingDetailsFragment newInstance(@Nullable String filePath) {
        final RecordingDetailsFragment fragment = new RecordingDetailsFragment();
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
        final View view = inflater.inflate(R.layout.fragment_recording_details, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI(view.getContext());

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

    @Override public void onDestroyView() {
        // stop editing filename
        stopEditFilename();

        super.onDestroyView();
        unbinder.unbind();
    }
    //==============================================
    //  OVERRIDES
    //==============================================

    @Override protected boolean onBackPressed() {
        // rename file
        if (!renameFile()) return true;
        // stop editing
        stopEditFilename();

        // Opens recording options view
        EventBus.getDefault().post(new OpenRecordingOptionsEvent(filePath));

        return true;
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    private void setupUI(@NonNull Context context) {
        final File file = new File(filePath);
        AudioFile af = null;
        if (file.exists()) af = BaseAudioFile.create(file);

        if (af != null) {
            // back button
            ibtnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
            // file name
            final String filename = RecordingUtils.getFileNameWithoutExtension(file);
            tvFilename.setText(filename);
            tvFilename.setVisibility(View.VISIBLE);
            tvFilename.setOnClickListener(v -> startEditFilename());
            etFilename.setText(filename);
            etFilename.setVisibility(View.INVISIBLE);
            ViewUtils.tintDrawable(etFilename.getBackground(),
                ContextCompat.getColor(context, android.R.color.transparent));
            etFilename.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // try to rename file
                    if (!renameFile()) return true;
                    stopEditFilename();

                    return true;
                }
                return false;
            });
            ibtnClearFilename.setVisibility(View.GONE);
            ibtnClearFilename.setOnClickListener(v -> etFilename.setText(null));
            // mime type
            tvMimeType.setText(af.mimeType());
            // recorder on
            tvRecordedOn.setText(DateUtils.format_M_d_yyyy_H_mm_a(new Date(file.lastModified())));
            // sample rate
            tvSamplingRate.setText(String.valueOf(af.sampleRate()));
            // number of channels
            tvNumOfChannels.setText(String.valueOf(af.channelCount()));
            // number of bits per sample1
            tvBitsPerSample.setText(String.valueOf(af.bitsPerSample()));
            // recording length
            tvFileLength.setText(String.valueOf(af.duration()));
        }
    }

    private void startEditFilename() {
        etFilename.setText(tvFilename.getText());
        tvFilename.setVisibility(View.INVISIBLE);
        etFilename.setVisibility(View.VISIBLE);
        etFilename.setSelection(etFilename.getText().length());
        ViewUtils.openSoftKeyboard(etFilename, 200);
        ibtnClearFilename.setVisibility(View.VISIBLE);
    }

    private void stopEditFilename() {
        tvFilename.setText(etFilename.getText());
        tvFilename.setVisibility(View.VISIBLE);
        ViewUtils.hideSoftKeyboard(etFilename, 200);
        etFilename.setVisibility(View.INVISIBLE);
        ibtnClearFilename.setVisibility(View.GONE);
    }

    // Triggers renaming of the selected file
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") private boolean renameFile() {
        final File oldFile = new File(filePath);
        final String oldFilename = RecordingUtils.getFileNameWithoutExtension(oldFile);
        final String newFilename = etFilename.getText().toString().trim();
        if (oldFile.exists()) {
            // if name remained the same do nothing
            if (ObjectUtils.equals(oldFilename, newFilename)) return true;
            // validate the new file name
            if (ApacheCommonsLang3Utils.isBlank(newFilename)) {
                if (getContext() != null) {
                    ViewUtils.toast(getContext(),
                        getString(R.string.error_message_validation_file_name));
                }
                return false;
            }
            final File newFile =
                new File(oldFile.getParent(), newFilename + RecordingUtils.BYB_RECORDING_EXT);
            // validate if file with specified name already exists
            if (!newFile.exists()) {
                // get events file before renaming
                final File ef = RecordingUtils.getEventFile(oldFile);
                // rename the file
                if (oldFile.renameTo(newFile)) {
                    // update db spike analysis data with new file path
                    if (getAnalysisManager() != null) {
                        getAnalysisManager().updateSpikeAnalysisFilePath(filePath,
                            newFile.getAbsolutePath());
                    }
                    // save new file path locally
                    filePath = newFile.getAbsolutePath();
                    // we need to rename events file as well, if it exists
                    if (ef != null) {
                        final File newEventsFile = RecordingUtils.createEventsFile(newFile);
                        if (!newEventsFile.exists()) {
                            // let's rename events file
                            if (!ef.renameTo(newEventsFile)) {
                                BYBUtils.showAlert(getActivity(), getString(R.string.title_error),
                                    getString(R.string.error_message_files_events_rename));
                                Crashlytics.logException(new Throwable(
                                    "Renaming events file for the given recording "
                                        + oldFile.getPath() + " failed"));
                            }
                        }
                    }
                } else {
                    if (getContext() != null) {
                        ViewUtils.toast(getContext(),
                            getString(R.string.error_message_files_rename));
                    }
                    Crashlytics.logException(
                        new Throwable("Renaming file " + oldFile.getPath() + " failed"));
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
            Crashlytics.logException(new Throwable("File " + oldFile.getPath() + " doesn't exist"));
        }

        return true;
    }
}
