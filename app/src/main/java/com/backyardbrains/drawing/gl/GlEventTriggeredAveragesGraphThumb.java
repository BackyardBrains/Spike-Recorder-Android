package com.backyardbrains.drawing.gl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.vo.EventTriggeredAverages;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlEventTriggeredAveragesGraphThumb extends GlGraphThumb {

    private static final float LINE_WIDTH = 2f;

    private final GlLineGraph lineGraph;

    public GlEventTriggeredAveragesGraphThumb(@NonNull Context context, @NonNull GL10 gl) {
        super(context, gl);

        lineGraph = new GlLineGraph();
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @Nullable EventTriggeredAverages data,
        @Nullable String graphName) {
        if (data == null) return;

        final int length = data.getAverages().length;
        int event;
        for (int i = 0; i < length; i++) {
            event = Integer.valueOf(data.getEvents()[i]);
            // draw event triggered average graph
            lineGraph.draw(gl, x, y, w, h, data.getNormAverages()[i], LINE_WIDTH,
                Colors.MARKER_COLORS[event % Colors.MARKER_COLORS.length]);
        }

        // draw confidence intervals graph (Monte Carlo)
        if (data.isShowConfidenceIntervals()) {
            lineGraph.draw(gl, x, y, w, h, data.getNormMonteCarloTop(), LINE_WIDTH, Colors.GRAY_LIGHT);
            lineGraph.draw(gl, x, y, w, h, data.getNormMonteCarloAverages(), LINE_WIDTH, Colors.BLUE);
            lineGraph.draw(gl, x, y, w, h, data.getNormMonteCarloBottom(), LINE_WIDTH, Colors.GRAY_LIGHT);
        }

        // draw borders and name
        super.draw(gl, x, y, w, h, graphName);
    }
}
