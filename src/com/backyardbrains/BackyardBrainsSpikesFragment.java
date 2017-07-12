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
import com.backyardbrains.drawing.BYBColors;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.drawing.ThresholdOrientation;
import com.backyardbrains.events.AudioAnalysisDoneEvent;
import com.backyardbrains.utils.BYBConstants;
import com.backyardbrains.view.BYBThresholdHandle;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class BackyardBrainsSpikesFragment extends BackyardBrainsPlaybackScopeFragment {

    private static final String TAG = makeLogTag(BackyardBrainsSpikesFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";

    @BindView(R.id.threshold_handle_left) BYBThresholdHandle thresholdHandleLeft;
    @BindView(R.id.threshold_handle_right) BYBThresholdHandle thresholdHandleRight;
    @BindView(R.id.ibtn_remove_threshold) ImageButton ibtnRemoveThreshold;
    @BindViews({ R.id.threshold0, R.id.threshold1, R.id.threshold2 }) List<ImageButton> thresholdButtons;
    @BindView(R.id.ibtn_add_threshold) ImageButton ibtnAddThreshold;

    private Unbinder unbinder;

    private float[][] handleColors;
    private int[] handleColorsHex = { 0xffff0000, 0xffffff00, 0xff00ffff };

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

    @Override
    protected FindSpikesRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        final FindSpikesRenderer renderer = new FindSpikesRenderer(fragment, preparedBuffer);
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
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() {
                            final float millisecondsInThisWindow = drawSurfaceWidth / 44100.0f * 1000 / 2;
                            setMilliseconds(millisecondsInThisWindow);

                            float yPerDiv =
                                (float) drawSurfaceHeight / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale;
                            setMillivolts(yPerDiv);
                        }
                    });
                }
            }

            @Override public void onHorizontalDragStart() {
                startSeek();
            }

            @Override public void onHorizontalDrag(float dx) {
                int progress = (int) (sbAudioProgress.getProgress() - dx);
                if (progress < 0) progress = 0;
                if (progress > sbAudioProgress.getMax()) progress = sbAudioProgress.getMax();
                final int finalProgress = progress;
                sbAudioProgress.post(new Runnable() {
                    @Override public void run() {
                        seek(finalProgress);
                        sbAudioProgress.setProgress(finalProgress);
                    }
                });
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
        if (event.isSuccess()) {
            if (getAnalysisManager() != null && !getAnalysisManager().spikesFound()) addThreshold();
            updateThresholdActions();
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
                    int thresholdsSize = getAnalysisManager().getThresholdsSize();
                    if (thresholdsSize > 0) {
                        int t = (int) getRenderer().pixelHeightToGlHeight(y);
                        getRenderer().setThreshold(t, ThresholdOrientation.LEFT);
                        getAnalysisManager().setThreshold(ThresholdOrientation.LEFT, t);
                    }
                }
            }
        });
        // right threshold
        thresholdHandleRight.setOnHandlePositionChangeListener(new BYBThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                if (getAnalysisManager() != null) {
                    int thresholdsSize = getAnalysisManager().getThresholdsSize();
                    if (thresholdsSize > 0) {
                        int t = (int) getRenderer().pixelHeightToGlHeight(y);
                        getRenderer().setThreshold(t, ThresholdOrientation.RIGHT);
                        getAnalysisManager().setThreshold(ThresholdOrientation.RIGHT, t);
                    }
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
    private void setThreshold(@ThresholdOrientation int threshold, int value) {
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
    private void selectThreshold(int index) {
        if (getAnalysisManager() != null) getAnalysisManager().selectThreshold(index);

        updateThresholdActions();
    }

    // Adds a new threshold to analysis manager and updates UI.
    private void addThreshold() {
        if (getAnalysisManager() != null) getAnalysisManager().addThreshold();

        updateThresholdActions();
    }

    // Removes currently selected threshold and updates UI.
    private void removeSelectedThreshold() {
        if (getAnalysisManager() != null) getAnalysisManager().removeSelectedThreshold();

        updateThresholdActions();
    }

    // Updates threshold actions
    private void updateThresholdActions() {
        if (getAnalysisManager() != null) {
            int thresholdsSize = getAnalysisManager().getThresholdsSize();
            final int minThresholds = 1;
            int maxThresholds = getAnalysisManager().getMaxThresholds();
            int selectedThreshold = getAnalysisManager().getSelectedThresholdIndex();
            if (thresholdsSize > 0 && selectedThreshold >= 0 && selectedThreshold < maxThresholds) {
                int[] t = getAnalysisManager().getSelectedThresholds();
                getRenderer().setThreshold(t[ThresholdOrientation.LEFT], ThresholdOrientation.LEFT);
                getRenderer().setThreshold(t[ThresholdOrientation.RIGHT], ThresholdOrientation.RIGHT);

                thresholdHandleLeft.setPosition(getRenderer().getThresholdScreenValue(ThresholdOrientation.LEFT));
                thresholdHandleRight.setPosition(getRenderer().getThresholdScreenValue(ThresholdOrientation.RIGHT));

                float[] currentColor = handleColors[selectedThreshold];
                getRenderer().setCurrentColor(currentColor);
                thresholdHandleLeft.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
                thresholdHandleRight.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
            }
            for (int i = 0; i < maxThresholds; i++) {
                if (i < thresholdsSize) {
                    thresholdButtons.get(i).setVisibility(View.VISIBLE);
                } else {
                    thresholdButtons.get(i).setVisibility(View.GONE);
                }
            }
            ibtnAddThreshold.setVisibility(thresholdsSize < maxThresholds ? View.VISIBLE : View.GONE);
            ibtnRemoveThreshold.setVisibility(thresholdsSize > minThresholds ? View.VISIBLE : View.GONE);
        }
    }
}