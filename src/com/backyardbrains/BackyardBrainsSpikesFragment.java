package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.drawing.BYBColors;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.view.BYBThresholdHandle;
import java.util.List;

import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsSpikesFragment extends BackyardBrainsBaseScopeFragment {

    private static final String TAG = makeLogTag(BackyardBrainsSpikesFragment.class);

    @BindView(R.id.threshold_handle_left) BYBThresholdHandle thresholdHandleLeft;
    @BindView(R.id.threshold_handle_right) BYBThresholdHandle thresholdHandleRight;
    @BindView(R.id.ibtn_back) ImageButton ibtnBack;
    @BindView(R.id.ibtn_remove_threshold) ImageButton ibtnRemoveThreshold;
    @BindViews({ R.id.threshold0, R.id.threshold1, R.id.threshold2 }) List<ImageButton> thresholdButtons;
    @BindView(R.id.ibtn_add_threshold) ImageButton ibtnAddThreshold;
    @BindView(R.id.playheadBar) SeekBar sbAudioSeeker;

    private Unbinder unbinder;

    private float[][] handleColors;
    private int[] handleColorsHex = { 0xffff0000, 0xffffff00, 0xff00ffff };

    // ----------------------------------------------------------------------------------------
    public BackyardBrainsSpikesFragment() {
        super();
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

    @Override protected FindSpikesRenderer createRenderer(@NonNull Context context, @NonNull float[] preparedBuffer) {
        return new FindSpikesRenderer(context, preparedBuffer);
    }

    @Override protected FindSpikesRenderer getRenderer() {
        return (FindSpikesRenderer) super.getRenderer();
    }

    @Override protected int getLayoutID() {
        return R.layout.backyard_spikes;
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- FRAGMENT LIFECYCLE
    // ---------------------------------------------------------------------------------------------
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            unbinder = ButterKnife.bind(this, view);
            setupUI();
        }

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onResume() {
        super.onResume();
        updateThresholdHandles();
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- GL
    // ---------------------------------------------------------------------------------------------

    @Override protected void reassignSurfaceView() {
        super.reassignSurfaceView();
        updateThresholdHandles();
    }

    @Nullable private BYBAnalysisManager getAnalysisManager() {
        if (getContext() != null) return ((BackyardBrainsApplication) getContext()).getAnalysisManager();

        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- THRESHOLDS
    // ---------------------------------------------------------------------------------------------

    protected void selectThreshold(int index) {
        if (getAnalysisManager() != null) {
            getAnalysisManager().selectThreshold(index);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void addThreshold() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().addThreshold();
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void removeSelectedThreshold() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().removeSelectedThreshold();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- UI SETUPS
    // ---------------------------------------------------------------------------------------------

    private void setupUI() {
        setupButtons();
        ((BackyardBrainsMain) getActivity()).showButtons(false);

        if (getAnalysisManager() != null && !getAnalysisManager().spikesFound()) addThreshold();
    }

    // ----------------------------------------------------------------------------------------
    public void setupButtons() {
        // left threshold
        thresholdHandleLeft.setOnHandlePositionChangeListener(new BYBThresholdHandle.OnThresholdChangeListener() {
            @Override public void onChange(@NonNull View view, float y) {
                if (getAnalysisManager() != null) {
                    int thresholdsSize = getAnalysisManager().getThresholdsSize();
                    if (thresholdsSize > 0) {
                        int t = (int) getRenderer().pixelHeightToGlHeight(y);
                        getRenderer().setThreshold(t, FindSpikesRenderer.LEFT_THRESH_INDEX);
                        getAnalysisManager().setThreshold(thresholdsSize - 1, FindSpikesRenderer.LEFT_THRESH_INDEX, t);
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
                        getRenderer().setThreshold(t, FindSpikesRenderer.RIGHT_THRESH_INDEX);
                        getAnalysisManager().setThreshold(thresholdsSize - 1, FindSpikesRenderer.RIGHT_THRESH_INDEX, t);
                    }
                }
            }
        });
        // threshold selection buttons
        for (int i = 0; i < thresholdButtons.size(); i++) {
            thresholdButtons.get(i).setColorFilter(handleColorsHex[i]);
        }
        thresholdButtons.get(0).setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            selectThreshold(0);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        thresholdButtons.get(1).setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            selectThreshold(1);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        thresholdButtons.get(2).setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            selectThreshold(2);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        // back button
        ibtnBack.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            if (getContext() != null) {
                                Intent j = new Intent();
                                j.setAction("BYBChangePage");
                                j.putExtra("page", BackyardBrainsMain.RECORDINGS_LIST);
                                context.sendBroadcast(j);
                            }
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        // add threshold
        ibtnAddThreshold.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            addThreshold();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        // remove threshold
        ibtnRemoveThreshold.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            removeSelectedThreshold();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        // audio controls
        sbAudioSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (getRenderer() != null) {
                        getRenderer().setStartSample(
                            (float) BackyardBrainsSpikesFragment.this.sbAudioSeeker.getProgress()
                                / (float) BackyardBrainsSpikesFragment.this.sbAudioSeeker.getMax());
                    }
                }
            }
        });
        sbAudioSeeker.setProgress(0);
    }

    // ----------------------------------------------------------------------------------------
    private void updateThresholdHandles() {
        if (getAnalysisManager() != null) {
            int thresholdsSize = getAnalysisManager().getThresholdsSize();
            int maxThresholds = getAnalysisManager().getMaxThresholds();
            int selectedThreshold = getAnalysisManager().getSelectedThresholdIndex();
            if (thresholdsSize > 0 && selectedThreshold >= 0 && selectedThreshold < maxThresholds) {
                int[] t = getAnalysisManager().getSelectedThresholds();
                for (int i = 0; i < 2; i++) {
                    getRenderer().setThreshold(t[i], i);
                }
                thresholdHandleLeft.setPosition(
                    getRenderer().getThresholdScreenValue(FindSpikesRenderer.LEFT_THRESH_INDEX));
                thresholdHandleRight.setPosition(
                    getRenderer().getThresholdScreenValue(FindSpikesRenderer.RIGHT_THRESH_INDEX));

                float[] currentColor = handleColors[selectedThreshold];
                getRenderer().setCurrentColor(currentColor);
                thresholdHandleLeft.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
                thresholdHandleRight.setColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
            }
            if (thresholdsSize < maxThresholds) {
                ibtnAddThreshold.setVisibility(View.VISIBLE);
            } else {
                ibtnAddThreshold.setVisibility(View.GONE);
            }
            for (int i = 0; i < maxThresholds; i++) {
                if (i < thresholdsSize) {
                    thresholdButtons.get(i).setVisibility(View.VISIBLE);
                } else {
                    thresholdButtons.get(i).setVisibility(View.GONE);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // --------------------------------------------------- LISTENERS
    // ---------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------ BROADCAST RECEIVERS OBJECTS
    private ThresholdHandlePosListener thresholdHandlePosListener;
    private UpdateThresholdHandleListener updateThresholdHandleListener;

    // ------------------------------------------------------------------------ BROADCAST RECEIVERS CLASS
    private class UpdateThresholdHandleListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (intent.hasExtra("name") && intent.hasExtra("pos")) {
                BYBThresholdHandle handle = null;
                if ("LeftSpikesHandle".equals(intent.getStringExtra("name"))) {
                    handle = thresholdHandleLeft;
                } else if ("RightSpikesHandle".equals(intent.getStringExtra("name"))) {
                    handle = thresholdHandleRight;
                }
                if (handle != null) {
                    if (handle.getVisibility() == View.GONE) handle.setVisibility(View.VISIBLE);
                    handle.setPosition(intent.getIntExtra("pos", 0));
                }
            }

            updateThresholdHandles();
        }
    }

    // ------------------------------------------------------------------------
    private class ThresholdHandlePosListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context mContext, android.content.Intent intent) {
            if (intent.hasExtra("y")) {
                float pos = intent.getFloatExtra("y", 0);
                if (intent.hasExtra("name")) {
                    if (getRenderer() != null) {
                        int index = -1;
                        if ("LeftSpikesHandle".equals(intent.getStringExtra("name"))) {
                            index = FindSpikesRenderer.LEFT_THRESH_INDEX;
                        } else if ("RightSpikesHandle".equals(intent.getStringExtra("name"))) {
                            index = FindSpikesRenderer.RIGHT_THRESH_INDEX;
                        }
                        if (getAnalysisManager() != null) {
                            int thresholdsSize = getAnalysisManager().getThresholdsSize();
                            if (thresholdsSize > 0 && index >= 0) {
                                int t = (int) getRenderer().pixelHeightToGlHeight(pos);
                                getAnalysisManager().setThreshold(thresholdsSize - 1, index, t);
                                getRenderer().setThreshold(t, index);
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------ BROADCAST RECEIVERS TOGGLES
    private void registerUpdateThresholdHandleListener(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBUpdateThresholdHandle");
                updateThresholdHandleListener = new UpdateThresholdHandleListener();

                context.registerReceiver(updateThresholdHandleListener, intentFilter);
            } else {
                context.unregisterReceiver(updateThresholdHandleListener);
                updateThresholdHandleListener = null;
            }
        }
    }

    private void registerThresholdHandlePosListener(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBThresholdHandlePos");
                thresholdHandlePosListener = new ThresholdHandlePosListener();
                context.registerReceiver(thresholdHandlePosListener, intentFilter);
            } else {
                context.unregisterReceiver(thresholdHandlePosListener);
                thresholdHandlePosListener = null;
            }
        }
    }

    // ---------------------------------------------------------------------------- REGISTER RECEIVERS
    @Override public void registerReceivers(boolean bReg) {
        super.registerReceivers(bReg);
        registerThresholdHandlePosListener(bReg);
        registerUpdateThresholdHandleListener(bReg);
    }
}