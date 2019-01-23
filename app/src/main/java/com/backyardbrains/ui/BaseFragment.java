package com.backyardbrains.ui;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.backyardbrains.analysis.AnalysisManager;
import com.backyardbrains.dsp.ProcessingService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BaseFragment extends Fragment {

    private ResourceProvider provider;

    /**
     * This interface needs to be implemented by the activities which contain this fragment so that it can have access
     * to active {@link ProcessingService} and {@link AnalysisManager}.
     */
    @SuppressWarnings("WeakerAccess") public interface ResourceProvider {
        /**
         * Reference to active {@link ProcessingService}.
         */
        @Nullable ProcessingService processingService();

        /**
         * Reference to active {@link AnalysisManager}.
         */
        @Nullable AnalysisManager analysisManager();
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ResourceProvider) {
            this.provider = (ResourceProvider) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ResourceProvider");
        }
    }

    @CallSuper @Override public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @CallSuper @Override public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
    }

    @Override public void onDetach() {
        super.onDetach();
        this.provider = null;
    }

    @Nullable public ResourceProvider getProvider() {
        return provider;
    }

    protected boolean onBackPressed() {
        return false;
    }

    @Nullable protected ProcessingService getAudioService() {
        return provider != null ? provider.processingService() : null;
    }

    @Nullable protected AnalysisManager getAnalysisManager() {
        return provider != null ? provider.analysisManager() : null;
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNoSubscriberEvent(NoSubscriberEvent event) {
        // we need this to avoid EventBusException exception thrown by EventBus
    }
}
