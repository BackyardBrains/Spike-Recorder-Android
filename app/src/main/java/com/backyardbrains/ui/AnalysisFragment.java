package com.backyardbrains.ui;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.R;
import com.backyardbrains.analysis.AnalysisConfig;
import com.backyardbrains.analysis.AnalysisType;
import com.backyardbrains.drawing.AutoCorrelationRenderer;
import com.backyardbrains.drawing.AverageSpikeRenderer;
import com.backyardbrains.drawing.BaseAnalysisRenderer;
import com.backyardbrains.drawing.CrossCorrelationRenderer;
import com.backyardbrains.drawing.EventTriggeredAverageRenderer;
import com.backyardbrains.drawing.ISIRenderer;
import com.backyardbrains.drawing.TouchGlSurfaceView;
import com.backyardbrains.events.AnalysisDoneEvent;
import com.backyardbrains.events.RedrawAnalysisGraphEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.ViewUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AnalysisFragment extends BaseFragment {

    private static final String TAG = makeLogTag(AnalysisFragment.class);

    private static final String ARG_FILE_PATH = "bb_file_path";
    private static final String ARG_ANALYSIS_TYPE = "bb_analysis_type";
    private static final String ARG_ANALYSIS_CONFIG = "bb_analysis_config";

    @BindView(R.id.fl_container) FrameLayout flGL;
    @BindView(R.id.tv_analysis_title) TextView tvTitle;
    @BindView(R.id.ibtn_back) ImageButton ibtnBack;
    @BindView(R.id.pb_waiting) ProgressBar pbWaiting;
    @BindView(R.id.tv_waiting) TextView tvWaiting;

    private Unbinder unbinder;
    private TouchGlSurfaceView glSurface;
    private BaseAnalysisRenderer currentRenderer;

    private AnalysisConfig analysisConfig;

    /**
     * Factory for creating a new instance of the fragment.
     *
     * @return A new instance of fragment {@link AnalysisFragment}.
     */
    public static AnalysisFragment newInstance(@Nullable Parcelable config) {
        final AnalysisFragment fragment = new AnalysisFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ANALYSIS_CONFIG, config);
        fragment.setArguments(args);
        return fragment;
    }

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) analysisConfig = getArguments().getParcelable(ARG_ANALYSIS_CONFIG);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG, "onCreateView()");
        final View root = inflater.inflate(R.layout.fragment_analysis, container, false);
        unbinder = ButterKnife.bind(this, root);

        setupUI();

        return root;
    }

    @Override public void onStart() {
        LOGD(TAG, "onStart()");
        super.onStart();

        if (glSurface != null) glSurface.onResume();

        if (ApacheCommonsLang3Utils.isBlank(analysisConfig.getFilePath())) {
            if (getContext() != null) ViewUtils.toast(getContext(), getString(R.string.error_message_files_no_file));
            return;
        }

        if (getAnalysisManager() != null) {
            // show "Waiting..." screen and start analysis
            showWaiting(true);
            getAnalysisManager().startAnalysis(analysisConfig);
        }
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
        if (glSurface != null) glSurface.onPause();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override public void onDestroy() {
        LOGD(TAG, "onDestroy()");
        destroyRenderer();
        super.onDestroy();
    }

    //=================================================
    //  EVENT BUS
    //=================================================

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnalysisDoneEvent(AnalysisDoneEvent event) {
        LOGD(TAG, "Analysis of audio file finished. Success - " + event.isSuccess());
        // if everything is OK set render and request GL surface render
        if (event.isSuccess()) setRenderer(event.getType());
    }

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRedrawAnalysisGraphEvent(RedrawAnalysisGraphEvent event) {
        redraw();
    }

    //=================================================
    // OVERRIDES
    //=================================================

    @Override public boolean onBackPressed() {
        // if we are currently rendering cross correlation analysis specific train
        // just redraw to show thumbs view
        if (currentRenderer instanceof CrossCorrelationRenderer
            && !((CrossCorrelationRenderer) currentRenderer).isThumbsView()) {
            ((CrossCorrelationRenderer) currentRenderer).setThumbsView();
            redraw();

            return true;
        }

        return false;
    }

    //=================================================
    //  RENDERING OF ANALYSIS GRAPHS
    //=================================================

    // Sets new renderer for the GL surface view
    private void setRenderer(@AnalysisType int type) {
        LOGD(TAG, "setRenderer()");
        if (type >= AnalysisType.FIND_SPIKES && type <= AnalysisType.EVENT_TRIGGERED_AVERAGE) {
            reassignSurfaceView(analysisConfig.getAnalysisType());
        }
    }

    // Redraws the GL surface view
    private void redraw() {
        LOGD(TAG, "redraw()");

        if (glSurface != null) glSurface.requestRender();
    }

    // Sets visibility of "Waiting" views.
    private void showWaiting(final boolean show) {
        pbWaiting.setVisibility(show ? View.VISIBLE : View.GONE);
        tvWaiting.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Initializes GL surface view with the renderer of specified type
    @SuppressLint("SwitchIntDef") private void reassignSurfaceView(@AnalysisType int rendererType) {
        LOGD(TAG, "reassignSurfaceView  renderer: " + getTitle(rendererType) + "  " + rendererType);
        // hide waiting screen
        showWaiting(false);

        switch (rendererType) {
            case AnalysisType.AUTOCORRELATION:
                currentRenderer = new AutoCorrelationRenderer(this);
                break;
            case AnalysisType.AVERAGE_SPIKE:
                currentRenderer = new AverageSpikeRenderer(this);
                break;
            case AnalysisType.CROSS_CORRELATION:
                currentRenderer = new CrossCorrelationRenderer(this);
                break;
            case AnalysisType.ISI:
                currentRenderer = new ISIRenderer(this);
                break;
            case AnalysisType.EVENT_TRIGGERED_AVERAGE:
                currentRenderer = new EventTriggeredAverageRenderer(this);
                break;
        }

        if (flGL != null) {
            flGL.removeAllViews();
            // create new GL surface
            if (glSurface != null) glSurface = null;
            glSurface = new TouchGlSurfaceView(getContext());
            if (currentRenderer != null) glSurface.setRenderer(currentRenderer);
            glSurface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            // and add GL surface to UI
            flGL.addView(glSurface);
        }

        if (tvTitle != null) tvTitle.setText(getTitle(rendererType));

        LOGD(TAG, "Analysis GLSurfaceView reassigned");
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    // Initializes user interface
    private void setupUI() {
        ibtnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    // Returns analysis title depending on the analysis type
    private String getTitle(@AnalysisType int analysisType) {
        switch (analysisType) {
            case AnalysisType.AUTOCORRELATION:
                return getString(R.string.analysis_autocorrelation);
            case AnalysisType.AVERAGE_SPIKE:
                return getString(R.string.analysis_average_spike);
            case AnalysisType.CROSS_CORRELATION:
                return getString(R.string.analysis_cross_correlation);
            case AnalysisType.ISI:
                return getString(R.string.analysis_isi);
            case AnalysisType.EVENT_TRIGGERED_AVERAGE:
                return getString(R.string.analysis_event_triggered_averages);
            case AnalysisType.NONE:
                return getString(R.string.analysis_please_wait);
            case AnalysisType.FIND_SPIKES:
            default:
                return "";
        }
    }

    // Destroys renderer
    private void destroyRenderer() {
        if (currentRenderer != null) {
            currentRenderer.close();
            currentRenderer = null;
        }
    }
}
