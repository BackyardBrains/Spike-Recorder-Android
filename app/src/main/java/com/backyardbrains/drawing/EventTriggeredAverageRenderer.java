package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.drawing.gl.GlEventTriggeredAveragesGraph;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.vo.EventTriggeredAverages;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverageRenderer extends BaseAnalysisRenderer {

    static final String TAG = makeLogTag(EventTriggeredAverageRenderer.class);

    private static final NumberFormat FORMATTER = new DecimalFormat("#0.000");

    private final Context context;

    private GlEventTriggeredAveragesGraph eventTriggeredAveragesGraph;

    @SuppressWarnings("WeakerAccess") EventTriggeredAverages[] eventTriggeredAverageAnalysis;

    public EventTriggeredAverageRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        eventTriggeredAveragesGraph = new GlEventTriggeredAveragesGraph(context, gl, null, FORMATTER);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getEventTriggeredAverageAnalysis()) {
            int len = eventTriggeredAverageAnalysis.length;
            if (len > 0) {
                final float thumbSize = len > 1 ? getDefaultGraphThumbSize(surfaceWidth, surfaceHeight, len) : 0f;
                final boolean portraitOrientation = surfaceWidth < surfaceHeight;
                float x, y, w, h;
                //for (int i = 0; i < len; i++) {
                //    x = portraitOrientation ? MARGIN * (i + 1) + thumbSize * i
                //        : (float) surfaceWidth - (thumbSize + MARGIN);
                //    y = portraitOrientation ? MARGIN : (float) surfaceHeight - (MARGIN * (i + 1) + thumbSize * (i + 1));
                //    w = h = thumbSize;
                //    // pass thumb to parent class so we can detect thumb click
                //    glGraphThumbTouchHelper.registerTouchableArea(new Rect(x, y, thumbSize, thumbSize));
                //    glBarGraphThumb.draw(gl, x, y, w, h, eventTriggeredAverageAnalysis[i],
                //        Colors.CHANNEL_COLORS[i % Colors.CHANNEL_COLORS.length],
                //        SPIKE_TRAIN_THUMB_GRAPH_NAME_PREFIX + (i + 1));
                //}
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN : surfaceWidth - 3 * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - 3 * MARGIN - thumbSize : surfaceHeight - 2 * MARGIN;

                int selected = glGraphThumbTouchHelper.getSelectedTouchableArea();
                eventTriggeredAveragesGraph.draw(gl, x, y, w, h, eventTriggeredAverageAnalysis[selected]);
            }
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getEventTriggeredAverageAnalysis() {
        if (eventTriggeredAverageAnalysis != null && eventTriggeredAverageAnalysis.length > 0) return true;

        if (getAnalysisManager() != null) {
            eventTriggeredAverageAnalysis = getAnalysisManager().getEventTriggeredAverages();
            if (eventTriggeredAverageAnalysis != null) {
                LOGD(TAG, "EVENT TRIGGERED AVERAGE ANALYSIS RETURNED: " + eventTriggeredAverageAnalysis.length);
            }
            return eventTriggeredAverageAnalysis != null;
        }

        return false;
    }
}
