package com.backyardbrains;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import com.backyardbrains.analysis.BYBAnalysisType;
import com.backyardbrains.drawing.AutoCorrelationRenderer;
import com.backyardbrains.drawing.AverageSpikeRenderer;
import com.backyardbrains.drawing.BYBAnalysisBaseRenderer;
import com.backyardbrains.drawing.CrossCorrelationRenderer;
import com.backyardbrains.drawing.ISIRenderer;
import com.backyardbrains.drawing.TouchGLSurfaceView;
import com.backyardbrains.drawing.WaitRenderer;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BackyardBrainsAnalysisFragment extends Fragment {

    private static final String TAG = makeLogTag(BackyardBrainsAnalysisFragment.class);

    protected TouchGLSurfaceView mAndroidSurface = null;
    private FrameLayout mainscreenGLLayout;
    private SharedPreferences settings = null;
    private Context context = null;
    private BYBAnalysisBaseRenderer currentRenderer = null;
    private View waitingView = null;
    protected int currentAnalyzer = BYBAnalysisType.BYB_ANALYSIS_NONE;

    private TextView title;
    private ImageButton backButton;

    //----------------------------------------------------------------------------------------------
    // ----------------------------------------- FRAGMENT LIFECYCLE
    // ---------------------------------------------------------------------------------------------
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerListeners();
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.analysis_layout, container, false);
        getSettings();
        mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.analysisGlContainer);
        waitingView = rootView.findViewById(R.id.waitingLayout);
        title = (TextView) rootView.findViewById(R.id.analysis_title);
        backButton = (ImageButton) rootView.findViewById(R.id.backButton);

        backButton.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    if (event.getActionIndex() == 0) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            onBackPressed();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        ((BackyardBrainsMain) getActivity()).showButtons(false);
        LOGD(TAG, "onCreateView");

        return rootView;
    }

    @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart");
        if (context != null) {
            reassignSurfaceView(currentAnalyzer);
            Intent i = new Intent();
            i.setAction("BYBAnalysisFragmentReady");
            getContext().sendBroadcast(i);
        }
    }

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onPause() {
        super.onPause();
        unregisterListeners();
    }

    @Override public void onStop() {
        super.onStop();
        destroySurfaceView();
    }

    @Override public void onDestroy() {
        super.onDestroy();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- GL RENDERING
    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    public void setRenderer(int i) {
        LOGD(TAG, "setRenderer");
        if (i >= 0 && i <= 4) {
            currentAnalyzer = i;
            reassignSurfaceView(currentAnalyzer);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void destroySurfaceView() {
        mAndroidSurface = null;
        if (mainscreenGLLayout != null) {
            mainscreenGLLayout.removeAllViews();
            mainscreenGLLayout = null;
        }
    }

    private void showWaiting(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            waitingView.setVisibility(show ? View.VISIBLE : View.GONE);
            waitingView.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        waitingView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
        } else {
            waitingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void reassignSurfaceView(int renderer) {
        LOGD(TAG, "reassignSurfaceView  renderer: " + getRendererTitle(renderer) + "  " + renderer);
        context = getContext();

        showWaiting(renderer == BYBAnalysisType.BYB_ANALYSIS_NONE && waitingView != null);

        if (mainscreenGLLayout != null) {

            if (context != null) {
                mAndroidSurface = null;
                mainscreenGLLayout.removeAllViews();
                switch (renderer) {
                    case BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION:
                        setGlSurface(new AutoCorrelationRenderer(context), true);
                        break;
                    case BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION:
                        setGlSurface(new CrossCorrelationRenderer(context), true);
                        break;
                    case BYBAnalysisType.BYB_ANALYSIS_ISI:
                        setGlSurface(new ISIRenderer(context), true);
                        break;
                    case BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE:
                        setGlSurface(new AverageSpikeRenderer(context), true);
                        break;
                    case BYBAnalysisType.BYB_ANALYSIS_NONE:
                        setGlSurface(new WaitRenderer(context), true);
                        break;
                }
            }
            mainscreenGLLayout.addView(mAndroidSurface);
        }
        if (title != null) {
            title.setText(getRendererTitle(renderer));
        }

        LOGD(TAG, "Reassigned AnalysisGLSurfaceView");
    }

    // ----------------------------------------------------------------------------------------
    private String getRendererTitle(int renderer) {
        switch (renderer) {
            case BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION:
                return "Auto Correlation";
            case BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION:
                return "Cross Correlation";
            case BYBAnalysisType.BYB_ANALYSIS_ISI:
                return "Inter Spike Interval (ISI)";
            case BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE:
                return "Average Spike";
            case BYBAnalysisType.BYB_ANALYSIS_NONE:
                return "Please wait...";
        }
        return "";
    }

    // ----------------------------------------------------------------------------------------
    protected void setGlSurface(final BYBAnalysisBaseRenderer renderer, boolean bSetOnDemand) {
        context = getContext();
        if (context != null && renderer != null) {
            if (mAndroidSurface != null) {
                mAndroidSurface = null;
            }
            currentRenderer = renderer;
            mAndroidSurface = new TouchGLSurfaceView(context, renderer);
            //mAndroidSurface.setEGLContextClientVersion(2);
            //			mAndroidSurface.setRenderer(renderer);
            if (bSetOnDemand) {
                mAndroidSurface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        } else {
            //LOGD(TAG, "setGLSurface failed. Context == null.");
        }
    }

    @Override public Context getContext() {
        if (context == null) {
            FragmentActivity act = getActivity();
            if (act == null) {
                return null;
            }
            context = act.getApplicationContext();
        }
        return context;
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- TOUCH
    // ---------------------------------------------------------------------------------------------
    public boolean onTouchEvent(MotionEvent event) {
        LOGD(TAG, "onTouchEvent");
        if (currentRenderer != null) {
            currentRenderer.onTouchEvent(event);
        }
        if (mAndroidSurface != null) {
            return mAndroidSurface.onTouchEvent(event);
        }
        return false;
    }

    public void onBackPressed() {
        switch (currentAnalyzer) {
            case BYBAnalysisType.BYB_ANALYSIS_ISI:
            case BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE:
            case BYBAnalysisType.BYB_ANALYSIS_NONE:
            case BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION:
                ((BackyardBrainsMain) getActivity()).loadFragment(BackyardBrainsMain.RECORDINGS_LIST);
                break;
            case BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION:
                if (currentRenderer instanceof CrossCorrelationRenderer) {
                    if (((CrossCorrelationRenderer) currentRenderer).isDrawThumbs()) {
                        ((BackyardBrainsMain) getActivity()).loadFragment(BackyardBrainsMain.RECORDINGS_LIST);
                    } else {
                        ((CrossCorrelationRenderer) currentRenderer).setDrawThumbs(true);
                    }
                }
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- SETTINGS
    // ---------------------------------------------------------------------------------------------
    private void getSettings() {
        if (settings == null) {
            // settings = (
            // context).getPreferences(BackyardBrainsMain.MODE_PRIVATE);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS INSTANCES
    // ---------------------------------------------------------------------------------------------
    private RenderAnalysisListener renderAnalysisListener;

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    // ---------------------------------------------------------------------------------------------
    private class RenderAnalysisListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (intent.hasExtra("ISI")) {
                setRenderer(BYBAnalysisType.BYB_ANALYSIS_ISI);
            } else if (intent.hasExtra("AutoCorrelation")) {
                setRenderer(BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION);
            } else if (intent.hasExtra("CrossCorrelation")) {
                setRenderer(BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION);
            } else if (intent.hasExtra("AverageSpike")) {
                setRenderer(BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE);
            } else if (intent.hasExtra("requestRender")) {
                if (mAndroidSurface != null) {
                    mAndroidSurface.requestRender();
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ----------------------------------------- BROADCAST RECEIVERS TOGGLES
    // ---------------------------------------------------------------------------------------------
    private void registerRenderAnalysisReceiver(boolean reg) {
        if (reg) {
            IntentFilter intentFilter = new IntentFilter("BYBRenderAnalysis");
            renderAnalysisListener = new RenderAnalysisListener();
            if (getContext() != null) {
                context.registerReceiver(renderAnalysisListener, intentFilter);
            }
        } else {
            if (getContext() != null) {
                context.unregisterReceiver(renderAnalysisListener);
            }
            renderAnalysisListener = null;
        }
    }

    protected void registerListeners() {
        registerRenderAnalysisReceiver(true);
    }

    protected void unregisterListeners() {
        registerRenderAnalysisReceiver(false);
    }
}
