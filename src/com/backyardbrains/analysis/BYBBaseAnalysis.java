package com.backyardbrains.analysis;

import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

abstract class BYBBaseAnalysis {

    private static final String TAG = makeLogTag(BYBBaseAnalysis.class);

    private final AnalysisListener listener;
    private final AnalysisTask analysisTask;

    /**
     *
     */
    interface AnalysisListener {

        /**
         *
         */
        void onAnalysisDone();

        /**
         *
         */
        void onAnalysisCanceled();
    }

    BYBBaseAnalysis(@NonNull AnalysisListener listener) {
        this.listener = listener;
        this.analysisTask = new AnalysisTask();
    }

    /**
     *
     */
    abstract void process();

    /**
     * Triggers the analysis process.
     */
    public final void execute() {
        analysisTask.execute();
    }

    /**
     * Cancels the background process that performing the analysis.
     */
    public void stop() {
        analysisTask.cancel(true);
    }

    /**
     *
     */
    protected void asyncPreExecute() {
        LOGD(TAG, "asyncPreExecute");
    }

    /**
     *
     */
    @CallSuper protected void asyncPostExecute() {
        LOGD(TAG, "asyncPostExecute");
        listener.onAnalysisDone();
    }

    /**
     *
     */
    @CallSuper protected void asyncOnCancelled() {
        LOGD(TAG, "asyncOnCancelled");
        listener.onAnalysisCanceled();
    }

    /**
     * Background task that initializes the analysis process.
     */
    private class AnalysisTask extends AsyncTask<Void, Void, Void> {

        @Override protected void onPreExecute() {
            asyncPreExecute();
        }

        @SafeVarargs @Override protected final Void doInBackground(Void... params) {
            process();

            return null;
        }

        @Override protected void onPostExecute(Void v) {
            asyncPostExecute();
        }

        @Override protected void onCancelled(Void v) {
            asyncOnCancelled();
        }
    }
}
