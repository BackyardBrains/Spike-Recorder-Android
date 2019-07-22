package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.Benchmark;
import java.lang.ref.WeakReference;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

abstract class BaseAnalysis<Params, Result> {

    private static final String TAG = makeLogTag(BaseAnalysis.class);

    private final String filePath;
    private final AnalysisListener<Result> listener;
    private final AnalysisThread<Params, Result> analysisThread;

    /**
     *
     */
    interface AnalysisListener<Result> {

        /**
         *
         */
        void onAnalysisDone(@NonNull String filePath, @Nullable Result results);

        /**
         *
         */
        void onAnalysisFailed(@NonNull String filePath);
    }

    BaseAnalysis(@NonNull String filePath, @NonNull AnalysisListener<Result> listener) {
        this.filePath = filePath;
        this.listener = listener;

        analysisThread = new AnalysisThread<>(this);
    }

    /**
     *
     */
    @Nullable protected abstract Result process(Params... params) throws Exception;

    /**
     * Triggers the analysis process.
     */
    @SafeVarargs final void startAnalysis(Params... params) {
        analysisThread.start(params);
    }

    /**
     *
     */
    @SuppressWarnings("WeakerAccess") void onResult(@Nullable Result result) {
    }

    /**
     *
     */
    @SuppressWarnings("WeakerAccess") void onFailed() {
    }

    /**
     *
     */
    @SuppressWarnings("WeakerAccess") void asyncOnResult(@Nullable Result result) {
        LOGD(TAG, "asyncOnResult");
        listener.onAnalysisDone(filePath, result);
        onResult(result);
    }

    /**
     *
     */
    @SuppressWarnings("WeakerAccess") void asyncOnFailed() {
        LOGD(TAG, "asyncOnFailed");
        listener.onAnalysisFailed(filePath);

        onFailed();
    }

    /**
     * Background thread that initializes the analysis process.
     */
    private static class AnalysisThread<Params, Result> extends Thread {

        private final Benchmark benchmark;

        private WeakReference<BaseAnalysis<Params, Result>> analysisRef;
        private Params[] params;

        AnalysisThread(BaseAnalysis<Params, Result> analysis) {
            analysisRef = new WeakReference<>(analysis);
            benchmark = new Benchmark("ANALYSIS_" + analysis.getClass().getName()).sessions(1)
                .measuresPerSession(1)
                .logBySession(true)
                .listener(() -> {
                    //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
                });
        }

        @SafeVarargs public synchronized final void start(Params... params) {
            this.params = params;

            start();
        }

        @Override public void run() {
            final BaseAnalysis<Params, Result> analysis;
            if ((analysis = analysisRef.get()) != null) {
                try {
                    //benchmark.start();
                    analysis.asyncOnResult(analysis.process(params));
                    //benchmark.end();
                } catch (Exception e) {
                    analysis.asyncOnFailed();
                }
            }
        }
    }
}
