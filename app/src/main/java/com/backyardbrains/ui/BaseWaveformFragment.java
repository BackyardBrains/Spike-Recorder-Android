package com.backyardbrains.ui;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.backyardbrains.R;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.view.WaveformLayout;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class BaseWaveformFragment extends BaseFragment {

    private String TAG = makeLogTag(BaseWaveformFragment.class);

    protected WaveformLayout waveform;
    protected ImageButton ibtnBack;

    BaseWaveformRenderer renderer;

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
        if (getContext() != null) renderer.onLoadSettings(getContext(), "onStart()");
    }

    @CallSuper @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
        waveform.pauseGL();
        if (getContext() != null) renderer.onSaveSettings(getContext(), "onStop()");
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
     * Returns renderer for the surface view.
     */
    protected BaseWaveformRenderer getRenderer() {
        return renderer;
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI() {
        renderer = createRenderer();
        waveform.setRenderer(renderer);

        if (isBackable()) {
            ibtnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
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
