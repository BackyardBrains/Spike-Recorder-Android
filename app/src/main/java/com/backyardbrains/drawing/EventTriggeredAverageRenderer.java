package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.drawing.gl.GlEventTriggeredAveragesGraph;
import com.backyardbrains.drawing.gl.GlEventTriggeredAveragesGraphThumb;
import com.backyardbrains.drawing.gl.Rect;
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

    private static final NumberFormat FORMATTER = new DecimalFormat("#0.00##");

    static {
        FORMATTER.setMaximumFractionDigits(4);
    }

    private final Context context;

    private GlEventTriggeredAveragesGraph glEventTriggeredAveragesGraph;
    private GlEventTriggeredAveragesGraphThumb glEventTriggeredAveragesGraphThumb;

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

        glEventTriggeredAveragesGraph = new GlEventTriggeredAveragesGraph(context, gl, null, FORMATTER);
        glEventTriggeredAveragesGraphThumb = new GlEventTriggeredAveragesGraphThumb(context, gl);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getEventTriggeredAverageAnalysis()) {
            int len = eventTriggeredAverageAnalysis.length;
            if (len > 0) {
                final float thumbSize = len > 1 ? getDefaultGraphThumbSize(surfaceWidth, surfaceHeight, len) : 0f;
                final boolean portraitOrientation = surfaceWidth < surfaceHeight;
                float x, y, w, h;
                if (len > 1) {
                    for (int i = 0; i < len; i++) {
                        x = portraitOrientation ? MARGIN * (i + 1) + thumbSize * i
                            : (float) surfaceWidth - (thumbSize + MARGIN);
                        y = portraitOrientation ? MARGIN
                            : (float) surfaceHeight - (MARGIN * (i + 1) + thumbSize * (i + 1));
                        w = h = thumbSize;
                        // pass thumb to parent class so we can detect thumb click
                        glGraphThumbTouchHelper.registerTouchableArea(new Rect(x, y, thumbSize, thumbSize));

                        glEventTriggeredAveragesGraphThumb.draw(gl, x, y, w, h, eventTriggeredAverageAnalysis[i],
                            CHANNEL_THUMB_GRAPH_NAME_PREFIX + (i + 1));
                    }
                }
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN
                    : surfaceWidth - (len > 1 ? 3 : 2) * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - (len > 1 ? 3 : 2) * MARGIN - thumbSize
                    : surfaceHeight - 2 * MARGIN;

                int selected = glGraphThumbTouchHelper.getSelectedTouchableArea();
                glEventTriggeredAveragesGraph.draw(gl, x, y, w, h, eventTriggeredAverageAnalysis[selected]);
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
