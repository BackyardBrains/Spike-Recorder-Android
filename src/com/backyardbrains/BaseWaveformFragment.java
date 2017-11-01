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
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.events.SampleRateChangeEvent;
import com.backyardbrains.view.WaveformLayout;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
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
     * Subclasses should override this method if they need to do some work when sample rate changes.
     */
    protected void onSampleRateChange(int sampleRate) {
    }

    //==============================================
    //  EVENT BUS
    //==============================================

    @Subscribe(threadMode = ThreadMode.MAIN) public final void onSampleRateChangeEvent(SampleRateChangeEvent event) {
        LOGD(TAG, "onSampleRateChangeEvent(" + event.getSampleRate() + ")");
        if (getRenderer() != null) getRenderer().setSampleRate(event.getSampleRate());
        onSampleRateChange(event.getSampleRate());
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
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

    // Destroys renderer
    private void destroyRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }
}
