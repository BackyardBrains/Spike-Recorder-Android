package com.backyardbrains;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.audio.AudioService;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BaseFragment extends Fragment {

    private ResourceProvider provider;

    /**
     * This interface needs to be implemented by the activities which contain this fragment so that it can have access
     * to active {@link AudioService} and {@link BYBAnalysisManager}.
     */
    public interface ResourceProvider {
        /**
         * Reference to active {@link AudioService}.
         */
        @Nullable AudioService audioService();

        /**
         * Reference to active {@link BYBAnalysisManager}.
         */
        @Nullable BYBAnalysisManager analysisManager();
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ResourceProvider) {
            this.provider = (ResourceProvider) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ResourceProvider");
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        this.provider = null;
    }

    @Nullable public ResourceProvider getProvider() {
        return provider;
    }

    @Nullable protected AudioService getAudioService() {
        return provider != null ? provider.audioService() : null;
    }

    @Nullable protected BYBAnalysisManager getAnalysisManager() {
        return provider != null ? provider.analysisManager() : null;
    }
}
