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
import com.backyardbrains.analysis.AnalysisManager;
import com.backyardbrains.data.Threshold;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.drawing.BYBColors;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.events.AnalysisDoneEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.utils.Func;
import com.backyardbrains.utils.GlUtils;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.view.ThresholdHandle;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesFragment extends PlaybackScopeFragment {

    private static final String TAG = makeLogTag(FindSpikesFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";

    // Max number of thresholds
    private static final int MAX_THRESHOLDS = 3;

    // Runnable used for updating playback seek bar
    final protected SetThresholdRunnable setThresholdRunnable = new SetThresholdRunnable();

    @BindView(R.id.ll_finding_spikes_progress) ViewGroup llFindingSpikesProgress;
    @BindView(R.id.threshold_handle_left) ThresholdHandle thresholdHandleLeft;
    @BindView(R.id.threshold_handle_right) ThresholdHandle thresholdHandleRight;
    @BindView(R.id.ibtn_remove_threshold) ImageButton ibtnRemoveThreshold;
    @BindViews({ R.id.threshold0, R.id.threshold1, R.id.threshold2 }) List<ImageButton> thresholdButtons;
    @BindView(R.id.ibtn_add_threshold) ImageButton ibtnAddThreshold;

    private Unbinder unbinder;

    private int[] handleColorsHex = { 0xffff0000, 0xffffff00, 0xff00ff00 };

    // Index of the currently selected threshold
    int selectedThreshold;

    /**
     * Runnable that is executed on the UI thread every time one of the thresholds is updated.
     */
    protected class SetThresholdRunnable implements Runnable {

        private @ThresholdOrientation int orientation;
        private int value;

        @Override public void run() {
            setThreshold(orientation, value);
        }

        public void setOrientation(@ThresholdOrientation int orientation) {
            this.orientation = orientation;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link FindSpikesFragment}.
     */
    public static FindSpikesFragment newInstance(@Nullable String filePath) {
        final FindSpikesFragment fragment = new FindSpikesFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

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

    /**
     * {@inheritDoc}
     */
    @Override protected FindSpikesRenderer createRenderer() {
        final FindSpikesRenderer renderer = new FindSpikesRenderer(this, filePath);
        renderer.setOnDrawListener(new BaseWaveformRenderer.OnDrawListener() {

            @Override public void onDraw(final int drawSurfaceWidth, final int drawSurfaceHeight) {
                if (getActivity() != null) {
                    viewableTimeSpanUpdateRunnable.setSampleRate(sampleRate);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceWidth(drawSurfaceWidth);
                    viewableTimeSpanUpdateRunnable.setDrawSurfaceHeight(drawSurfaceHeight);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(viewableTimeSpanUpdateRunnable);
                }
            }
        });
        renderer.setOnScrollListener(new BaseWaveformRenderer.OnScrollListener() {

            @Override public void onScrollStart() {
                startSeek();
            }

            @Override public void onScroll(float dx) {
                if (getActivity() != null) {
                    int progress = (int) (sbAudioProgress.getProgress() - dx);
                    if (progress < 0) progress = 0;
                    if (progress > sbAudioProgress.getMax()) progress = sbAudioProgress.getMax();
                    playbackSeekRunnable.setProgress(progress);
                    playbackSeekRunnable.setUpdateProgressSeekBar(true);
                    playbackSeekRunnable.setUpdateProgressTimeLabel(true);
                    // we need to call it on UI thread because renderer is drawing on background thread
                    getActivity().runOnUiThread(playbackSeekRunnable);
                }
            }

            @Override public void onScrollEnd() {
                stopSeek();
            }
        });
        renderer.setOnThresholdUpdateListener(new FindSpikesRenderer.OnThresholdUpdateListener() {

            @Override public void onThresholdUpdate(@ThresholdOrientation final int threshold, final int value) {
                // we need to call it on UI thread because renderer is drawing on background thread
                if (getActivity() != null) {
                    setThresholdRunnable.setOrientation(threshold);
                    setThresholdRunnable.setValue(value);
                    getActivity().runOnUiThread(setThresholdRunnable);
                }
            }
        });
        return renderer;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected FindSpikesRenderer getRenderer() {
        return (FindSpikesRenderer) super.getRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected View createPlaybackView(LayoutInflater inflater, @NonNull ViewGroup container) {
        final View view = inflater.inflate(R.layout.fragment_spikes, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI();

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected boolean showThresholdView() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void startPlaying(boolean autoPlay) {
        if (getAnalysisManager() != null) getAnalysisManager().findSpikes(filePath);
        super.startPlaying(false);
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN) @Override
    public void onAudioPlaybackStartedEvent(AudioPlaybackStartedEvent event) {
        super.onAudioPlaybackStartedEvent(event);

        // let's set the playhead so that the begining of the waveform is at the middle of the screen
        int playhead = (int) (getRenderer().getGlWindowWidth() * .5f);
        seek(playhead);
        sbAudioProgress.setProgress(playhead);
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnalysisDoneEvent(AnalysisDoneEvent event) {
        LOGD(TAG, "Analysis of audio file finished. Success - " + event.isSuccess());
        if (event.isSuccess() && getAnalysisManager() != null) {
            getAnalysisManager().spikesAnalysisExists(filePath, new AnalysisDataSource.SpikeAnalysisCheckCallback() {
                @Override public void onSpikeAnalysisExistsResult(boolean exists, int trainCount) {
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
        ViewUtils.playAfterNextLayout(ibtnBack, new Func<View, Void>() {
            @Nullable @Override public Void apply(@Nullable View source) {
                thresholdHandleLeft.setTopOffset(ibtnBack.getHeight());
                thresholdHandleRight.setTopOffset(ibtnBack.getHeight());
                return null;
            }
        });
        if (getAnalysisManager() != null) {
            getAnalysisManager().spikesAnalysisExists(filePath, new AnalysisDataSource.SpikeAnalysisCheckCallback() {
                @Override public void onSpikeAnalysisExistsResult(boolean exists, int trainCount) {
                    if (!exists) llFindingSpikesProgress.setVisibility(View.VISIBLE);
                }
            });
        }

        setupThresholdActions();

        ibtnPlayPause.setVisibility(View.GONE);
        tvProgressTime.setVisibility(View.GONE);
    }

    // Initializes threshold actions
    private void setupThresholdActions() {
        // left threshold
        thresholdHandleLeft.setOnHandlePositionChangeListener(new ThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                if (getAnalysisManager() != null) {
                    int t = (int) getRenderer().pixelHeightToGlHeight(y);
                    getRenderer().setThreshold(t, ThresholdOrientation.LEFT);
                    getAnalysisManager().setThreshold(selectedThreshold, ThresholdOrientation.LEFT, t, filePath);
                }
            }
        });
        // right threshold
        thresholdHandleRight.setOnHandlePositionChangeListener(new ThresholdHandle.OnThresholdChangeListener() {
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
        ThresholdHandle handle = null;
        switch (threshold) {
            case ThresholdOrientation.LEFT:
                handle = thresholdHandleLeft;
                break;
            case ThresholdOrientation.RIGHT:
                handle = thresholdHandleRight;
                break;
        }
        if (handle != null) {
            //if (handle.getVisibility() != View.VISIBLE) handle.setVisibility(View.VISIBLE);
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
            getAnalysisManager().getThresholds(filePath, new AnalysisManager.GetThresholdsCallback() {
                @Override public void onThresholdsLoaded(@NonNull List<Threshold> thresholds) {
                    final int thresholdsSize = thresholds.size();
                    if (getRenderer() != null && thresholdsSize > 0 && selectedThreshold >= 0
                        && selectedThreshold < MAX_THRESHOLDS) {
                        llFindingSpikesProgress.setVisibility(View.GONE);

                        Threshold t = thresholds.get(selectedThreshold);
                        getRenderer().setThreshold(t.getThreshold(ThresholdOrientation.LEFT),
                            ThresholdOrientation.LEFT);
                        getRenderer().setThreshold(t.getThreshold(ThresholdOrientation.RIGHT),
                            ThresholdOrientation.RIGHT);

                        thresholdHandleLeft.setPosition(
                            getRenderer().getThresholdScreenValue(ThresholdOrientation.LEFT));
                        thresholdHandleRight.setPosition(
                            getRenderer().getThresholdScreenValue(ThresholdOrientation.RIGHT));

                        float[] currentColor = GlUtils.SPIKE_TRAIN_COLORS[selectedThreshold];
                        getRenderer().setCurrentColor(currentColor);
                        thresholdHandleLeft.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
                        thresholdHandleRight.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));

                        thresholdHandleLeft.setVisibility(View.VISIBLE);
                        thresholdHandleRight.setVisibility(View.VISIBLE);

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
                }
            });
        }
    }
}