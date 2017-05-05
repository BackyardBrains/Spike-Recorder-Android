package com.backyardbrains;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
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
    private BYBBaseRenderer renderer;

    protected float[] bufferWithXs = BYBBaseRenderer.initTempBuffer();

    @Override public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        LOGD(TAG, "onCreateView()");

        final View root = inflater.inflate(R.layout.fragment_base, container, false);
        waveform = (WaveformLayout) root.findViewById(R.id.waveform);

        // inflate subclass defined content view instead of the view stub
        final ViewStub contentStub = (ViewStub) root.findViewById(R.id.content_container);
        contentStub.setLayoutResource(getLayoutRes());
        final View content = contentStub.inflate();
        initView(content, container, savedInstanceState);

        setupUI();

        return root;
    }

    @CallSuper @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");
        waveform.resumeGL();
    }

    @CallSuper @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
        waveform.pauseGL();
    }

    @Override public void onDestroy() {
        LOGD(TAG, "onDestroy()");
        destroyRenderer();
        super.onDestroy();
    }

    protected abstract @LayoutRes int getLayoutRes();

    protected abstract void initView(@NonNull View view, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState);

    protected abstract BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer);

    protected void setMillivolts(float millivolts) {
        waveform.setMillivolts(millivolts);
    }

    protected void setMilliseconds(float milliseconds) {
        waveform.setMilliseconds(milliseconds);
    }

    protected BYBBaseRenderer getRenderer() {
        return renderer;
    }

    protected boolean shouldUseAverager() {
        return false;
    }

    // Initializes user interface
    private void setupUI() {
        setUseAverager();
        this.renderer = createRenderer(this, bufferWithXs);
        waveform.setRenderer(renderer);
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
