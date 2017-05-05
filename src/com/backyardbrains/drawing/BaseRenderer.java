package com.backyardbrains.drawing;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.audio.AudioService;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
abstract class BaseRenderer implements GLSurfaceView.Renderer {

    private final BaseFragment context;

    BaseRenderer(@NonNull BaseFragment fragment) {
        this.context = fragment;
    }

    @Nullable protected Context getContext() {
        return context.getContext();
    }

    @Nullable protected AudioService getAudioService() {
        return context.getProvider() != null ? context.getProvider().audioService() : null;
    }

    @Nullable protected BYBAnalysisManager getAnalysisManager() {
        return context.getProvider() != null ? context.getProvider().analysisManager() : null;
    }
}
