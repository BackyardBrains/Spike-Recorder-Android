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
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.events.SampleRateChangeEvent;
import com.backyardbrains.utils.Func;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.view.WaveformLayout;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class BaseWaveformFragment extends BaseFragment {

    private String TAG = makeLogTag(BaseWaveformFragment.class);

    // Runnable used for updating viewable time span number
    protected final ViewableTimeSpanUpdateRunnable viewableTimeSpanUpdateRunnable =
        new ViewableTimeSpanUpdateRunnable();

    private WaveformLayout waveform;
    private ImageButton ibtnBack;

    BaseWaveformRenderer renderer;

    /**
     * Runnable that is executed on the UI thread every time GL window is scaled vertically or horizontally.
     */
    protected class ViewableTimeSpanUpdateRunnable implements Runnable {

        private int sampleRate;
        private int drawSurfaceWidth;
        private int drawSurfaceHeight;

        @Override public void run() {
            if (getAudioService() != null) setMilliseconds(drawSurfaceWidth / (float) sampleRate * 1000 / 2);

            //setMillivolts((float) drawSurfaceHeight / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale);
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public void setDrawSurfaceWidth(int drawSurfaceWidth) {
            this.drawSurfaceWidth = drawSurfaceWidth;
        }

        public void setDrawSurfaceHeight(int drawSurfaceHeight) {
            this.drawSurfaceHeight = drawSurfaceHeight;
        }
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        LOGD(TAG, "onCreateView()");

        final View root = inflater.inflate(R.layout.fragment_base, container, false);
        waveform = root.findViewById(R.id.waveform);
        ibtnBack = root.findViewById(R.id.ibtn_back);

        setupUI();

        // inflate subclass defined content view instead of the view stub
        final FrameLayout contentContainer = root.findViewById(R.id.fl_content_container);
        contentContainer.addView(createView(inflater, contentContainer, savedInstanceState));

        return root;
    }

    @CallSuper @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");
        waveform.resumeGL();
        if (getContext() != null) renderer.onLoadSettings(getContext());
    }

    @CallSuper @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
        waveform.pauseGL();
        if (getContext() != null) renderer.onSaveSettings(getContext());
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

    protected abstract BaseWaveformRenderer createRenderer();

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
    protected BaseWaveformRenderer getRenderer() {
        return renderer;
    }

    /**
     * Recreates renderer. This method should be called when
     */
    protected void recreateRenderer() {
        // if renderer already exist we should save it's settings and then close it and and destroy it
        if (renderer != null && getContext() != null) renderer.onSaveSettings(getContext());
        destroyRenderer();
        // create renderer and load it's settings
        renderer = createRenderer();
        waveform.setRenderer(renderer);
        ViewUtils.playAfterNextLayout(waveform, new Func<View, Void>() {
            @Nullable @Override public Void apply(@Nullable View source) {
                if (renderer != null && getContext() != null) renderer.onLoadSettings(getContext());
                return null;
            }
        });
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public final void onSampleRateChangeEvent(SampleRateChangeEvent event) {
        LOGD(TAG, "onSampleRateChangeEvent(" + event.getSampleRate() + ")");
        if (getRenderer() != null) getRenderer().setSampleRate(event.getSampleRate());
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        renderer = createRenderer();
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

    // Destroys renderer
    private void destroyRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }
}
