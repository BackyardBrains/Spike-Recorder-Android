package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.data.Threshold;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.drawing.BYBColors;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.events.AudioAnalysisDoneEvent;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.view.BYBThresholdHandle;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class BackyardBrainsSpikesFragment extends BackyardBrainsPlaybackScopeFragment {

    private static final String TAG = makeLogTag(BackyardBrainsSpikesFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";

    // Max number of thresholds
    private static final int MAX_THRESHOLDS = 3;

    @BindView(R.id.threshold_handle_left) BYBThresholdHandle thresholdHandleLeft;
    @BindView(R.id.threshold_handle_right) BYBThresholdHandle thresholdHandleRight;
    @BindView(R.id.ibtn_remove_threshold) ImageButton ibtnRemoveThreshold;
    @BindViews({ R.id.threshold0, R.id.threshold1, R.id.threshold2 }) List<ImageButton> thresholdButtons;
    @BindView(R.id.ibtn_add_threshold) ImageButton ibtnAddThreshold;

    private Unbinder unbinder;

    float[][] handleColors;
    private int[] handleColorsHex = { 0xffff0000, 0xffffff00, 0xff00ffff };

    // Index of the currently selected threshold
    int selectedThreshold;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link BackyardBrainsSpikesFragment}.
     */
    public static BackyardBrainsSpikesFragment newInstance(@Nullable String filePath) {
        final BackyardBrainsSpikesFragment fragment = new BackyardBrainsSpikesFragment();
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

        handleColors = new float[3][4];

        handleColors[0][0] = 1.0f;
        handleColors[0][1] = 0.0f;
        handleColors[0][2] = 0.0f;
        handleColors[0][3] = 1.0f;

        handleColors[1][0] = 1.0f;
        handleColors[1][1] = 1.0f;
        handleColors[1][2] = 0.0f;
        handleColors[1][3] = 1.0f;

        handleColors[2][0] = 0.0f;
        handleColors[2][1] = 1.0f;
        handleColors[2][2] = 1.0f;
        handleColors[2][3] = 1.0f;
    }

    @Override public void onResume() {
        super.onResume();
        updateThresholdActions();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //=================================================
    //  ABSTRACT METHODS IMPLEMENTATIONS AND OVERRIDES
    //=================================================

    @Override protected FindSpikesRenderer createRenderer(@NonNull float[] preparedBuffer) {
        final FindSpikesRenderer renderer = new FindSpikesRenderer(this, preparedBuffer, filePath);
        renderer.setCallback(new FindSpikesRenderer.CallbackAdapter() {

            @Override public void onThresholdUpdate(@ThresholdOrientation final int threshold, final int value) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setThreshold(threshold, value);
                        }
                    });
                }
            }

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    viewableTimeSpanUpdateRunnable.setSampleRate(sampleRate);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceHeight(drawSurfaceHeight);
                    getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable/*new Runnable() {
                        @Override public void run() {
                            if (getAudioService() != null) {
                                setMilliseconds(
                                    drawSurfaceWidth / (float) getAudioService().getSampleRate() * 1000 / 2);
                            }

                            setMillivolts(
                                (float) drawSurfaceHeight / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale);
                        }
                    }*/);
                }
            }

            @Override public void onHorizontalDragStart() {
                startSeek();
            }

            @Override public void onHorizontalDrag(float dx) {
                int progress = (int) (sbAudioProgress.getProgress() - dx);
                if (progress < 0) progress = 0;
                if (progress > sbAudioProgress.getMax()) progress = sbAudioProgress.getMax();
                playbackSeekRunnable.setProgress(progress);
                playbackSeekRunnable.setUpdateProgressSeekBar(true);
                playbackSeekRunnable.setUpdateProgressTimeLabel(false);
                //final int finalProgress = progress;
                sbAudioProgress.post(playbackSeekRunnable/*new Runnable() {
                    @Override public void run() {
                        seek(finalProgress);
                        sbAudioProgress.setProgress(finalProgress);
                    }
                }*/);
            }

            @Override public void onHorizontalDragEnd() {
                stopSeek();
            }
        });
        return renderer;
    }

    @Override protected FindSpikesRenderer getRenderer() {
        return (FindSpikesRenderer) super.getRenderer();
    }

    @Override protected View createPlaybackView(LayoutInflater inflater, @NonNull ViewGroup container) {
        final View view = inflater.inflate(R.layout.fragment_spikes, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    @Override protected void startPlaying(boolean autoPlay) {
        if (getAnalysisManager() != null) getAnalysisManager().findSpikes(filePath);
        super.startPlaying(false);
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioAnalysisDoneEvent(AudioAnalysisDoneEvent event) {
        LOGD(TAG, "Analysis of audio file finished. Success - " + event.isSuccess());
        if (event.isSuccess() && getAnalysisManager() != null) {
            getAnalysisManager().spikesAnalysisExists(filePath, new AnalysisDataSource.SpikeAnalysisCheckCallback() {
                @Override public void onSpikeAnalysisExistsResult(boolean exists) {
                    if (!exists) addThreshold();
                    updateThresholdActions();
                }
            });
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI() {
        setupThresholdActions();

        ibtnPlayPause.setVisibility(View.GONE);
        tvProgressTime.setVisibility(View.GONE);
    }

    // Initializes threshold actions
    private void setupThresholdActions() {
        // left threshold
        thresholdHandleLeft.setOnHandlePositionChangeListener(new BYBThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                if (getAnalysisManager() != null) {
                    int t = (int) getRenderer().pixelHeightToGlHeight(y);
                    getRenderer().setThreshold(t, ThresholdOrientation.LEFT);
                    getAnalysisManager().setThreshold(selectedThreshold, ThresholdOrientation.LEFT, t, filePath);
                }
            }
        });
        // right threshold
        thresholdHandleRight.setOnHandlePositionChangeListener(new BYBThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                if (getAnalysisManager() != null) {
                    int t = (int) getRenderer().pixelHeightToGlHeight(y);
                    getRenderer().setThreshold(t, ThresholdOrientation.RIGHT);
                    getAnalysisManager().setThreshold(selectedThreshold, ThresholdOrientation.RIGHT, t, filePath);
                }
            }
        });
        // threshold selection buttons
        for (int i = 0; i < thresholdButtons.size(); i++) {
            thresholdButtons.get(i).setColorFilter(handleColorsHex[i]);
            final int index = i;
            thresholdButtons.get(i).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectThreshold(index);
                }
            });
        }
        // add threshold
        ibtnAddThreshold.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                addThreshold();
            }
        });
        // remove threshold
        ibtnRemoveThreshold.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                removeSelectedThreshold();
            }
        });
    }

    // Sets the specified value for the specified threshold
    void setThreshold(@ThresholdOrientation int threshold, int value) {
        BYBThresholdHandle handle = null;
        switch (threshold) {
            case ThresholdOrientation.LEFT:
                handle = thresholdHandleLeft;
                break;
            case ThresholdOrientation.RIGHT:
                handle = thresholdHandleRight;
                break;
        }
        if (handle != null) {
            if (handle.getVisibility() == View.GONE) handle.setVisibility(View.VISIBLE);
            handle.setPosition(value);
        }

        updateThresholdActions();
    }

    // Selects the threshold at specified index and updates UI.
    void selectThreshold(int index) {
        if (index >= 0 && index < MAX_THRESHOLDS) {
            selectedThreshold = index;

            updateThresholdActions();
        }
    }

    // Adds a new threshold to analysis manager and updates UI.
    void addThreshold() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().addThreshold(filePath, new AnalysisDataSource.AddSpikeAnalysisTrainCallback() {
                @Override public void onSpikeAnalysisTrainAdded(@NonNull Train train) {
                    selectedThreshold = train.getOrder();
                    updateThresholdActions();
                }
            });
        }
    }

    // Removes currently selected threshold and updates UI.
    void removeSelectedThreshold() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().removeThreshold(selectedThreshold, filePath,
                new AnalysisDataSource.RemoveSpikeAnalysisTrainCallback() {
                    @Override public void onSpikeAnalysisTrainRemoved(int newTrainCount) {
                        selectedThreshold = newTrainCount - 1;
                        updateThresholdActions();
                    }
                });
        }
    }

    // Updates threshold actions
    void updateThresholdActions() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().getThresholds(filePath, new BYBAnalysisManager.GetThresholdsCallback() {
                @Override public void onThresholdsLoaded(@NonNull List<Threshold> thresholds) {
                    final int thresholdsSize = thresholds.size();
                    if (thresholdsSize > 0 && selectedThreshold >= 0 && selectedThreshold < MAX_THRESHOLDS) {
                        Threshold t = thresholds.get(selectedThreshold);
                        getRenderer().setThreshold(t.getThreshold(ThresholdOrientation.LEFT),
                            ThresholdOrientation.LEFT);
                        getRenderer().setThreshold(t.getThreshold(ThresholdOrientation.RIGHT),
                            ThresholdOrientation.RIGHT);

                        thresholdHandleLeft.setPosition(
                            getRenderer().getThresholdScreenValue(ThresholdOrientation.LEFT));
                        thresholdHandleRight.setPosition(
                            getRenderer().getThresholdScreenValue(ThresholdOrientation.RIGHT));

                        float[] currentColor = handleColors[selectedThreshold];
                        getRenderer().setCurrentColor(currentColor);
                        thresholdHandleLeft.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
                        thresholdHandleRight.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
                    }
                    for (int i = 0; i < MAX_THRESHOLDS; i++) {
                        if (i < thresholdsSize) {
                            thresholdButtons.get(i).setVisibility(View.VISIBLE);
                        } else {
                            thresholdButtons.get(i).setVisibility(View.GONE);
                        }
                    }
                    ibtnAddThreshold.setVisibility(thresholdsSize < MAX_THRESHOLDS ? View.VISIBLE : View.GONE);
                    ibtnRemoveThreshold.setVisibility(thresholdsSize > 1 ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}