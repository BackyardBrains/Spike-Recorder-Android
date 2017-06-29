package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.view.WaveformLayout;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGW;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public abstract class BaseWaveformFragment extends BaseFragment {

    private String TAG = makeLogTag(BaseWaveformFragment.class);

    private WaveformLayout waveform;
    private ImageButton ibtnBack;

    private BYBBaseRenderer renderer;

    protected float[] bufferWithXs = BYBBaseRenderer.initTempBuffer();

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        LOGD(TAG, "onCreateView()");

        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_base, container, false);
        waveform = (WaveformLayout) root.findViewById(R.id.waveform);
        ibtnBack = (ImageButton) root.findViewById(R.id.ibtn_back);

        setupUI();

        // inflate subclass defined content view instead of the view stub
        final FrameLayout contentContainer = (FrameLayout) root.findViewById(R.id.fl_content_container);
        contentContainer.addView(createView(inflater, contentContainer, savedInstanceState));

        return root;
    }

    @CallSuper @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");
        waveform.resumeGL();
        renderer.onLoadSettings(getContext());
    }

    @CallSuper @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
        waveform.pauseGL();
        renderer.onSaveSettings(getContext());
    }

    @Override public void onDestroy() {
        LOGD(TAG, "onDestroy()");
        destroyRenderer();
        super.onDestroy();
    }

    //==============================================
    //  ABSTRACT METHODS DEFINITION
    //==============================================

    protected abstract View createView(LayoutInflater inflater, @NonNull ViewGroup container,
        @Nullable Bundle savedInstanceState);

    protected abstract BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer);

    protected abstract boolean isBackable();

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    /**
     * Update millivolts UI.
     */
    protected void setMillivolts(float millivolts) {
        waveform.setMillivolts(millivolts);
    }

    /**
     * Updates milliseconds UI.
     */
    protected void setMilliseconds(float milliseconds) {
        waveform.setMilliseconds(milliseconds);
    }

    /**
     * Returns renderer for the surface view.
     */
    protected BYBBaseRenderer getRenderer() {
        return renderer;
    }

    /**
     * Whether renderer should use averager when doing calculations.
     */
    protected boolean shouldUseAverager() {
        return false;
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        setUseAverager();
        renderer = createRenderer(this, bufferWithXs);
        waveform.setRenderer(renderer);

        if (isBackable()) {
            ibtnBack.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (getActivity() != null) getActivity().onBackPressed();
                }
            });
        } else {
            ibtnBack.setVisibility(View.GONE);
        }
    }

    // Sets whether audio service should use averager or not
    private void setUseAverager() {
        final AudioService provider = getAudioService();
        if (provider != null) {
            provider.setUseAverager(shouldUseAverager());
        } else {
            LOGW(TAG, "AudioService is null");
        }
    }

    // Destroys renderer
    private void destroyRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }
}
