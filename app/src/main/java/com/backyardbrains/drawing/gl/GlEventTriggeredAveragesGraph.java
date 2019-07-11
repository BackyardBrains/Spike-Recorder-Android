package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.utils.GlUtils;
import com.backyardbrains.vo.EventTriggeredAverages;
import java.text.NumberFormat;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlEventTriggeredAveragesGraph {

    private static final float[] H_AXIS_VALUES = new float[7];

    static {
        H_AXIS_VALUES[0] = -0.6f;
        H_AXIS_VALUES[1] = -0.4f;
        H_AXIS_VALUES[2] = -0.2f;
        H_AXIS_VALUES[3] = 0.0f;
        H_AXIS_VALUES[4] = 0.2f;
        H_AXIS_VALUES[5] = 0.4f;
        H_AXIS_VALUES[6] = 0.6f;
    }

    private static final float H_AXIS_MIN_VALUE = -0.7f;
    private static final float H_AXIS_MAX_VALUE = 0.7f;
    private static final int ZERO_DASH_LINE_WIDTH = 2;
    private static final float ZERO_DASH_LINE_SIZE = 15f;

    private final GlLineGraph lineGraph;
    private final GlGraphAxes graphAxes;
    private final GlDashedVLine dashedVLine;

    private float[] mappedData;

    public GlEventTriggeredAveragesGraph(@NonNull Context context, @NonNull GL10 gl,
        @Nullable NumberFormat hAxisValueFormatter, @Nullable NumberFormat vAxisValueFormatter) {
        lineGraph = new GlLineGraph();
        graphAxes = new GlGraphAxes(context, gl, hAxisValueFormatter, vAxisValueFormatter);
        dashedVLine = new GlDashedVLine();
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull EventTriggeredAverages data) {
        final int length = data.getMean().length;
        final float[] allVAxisMinMax = new float[length * 2];
        float[] vAxisMinMaxValues;
        int counter = 0;
        for (float[] analysis : data.getMean()) {
            vAxisMinMaxValues = GlUtils.getMinMax(analysis);
            allVAxisMinMax[counter++] = vAxisMinMaxValues[GlUtils.MIN_VALUE];
            allVAxisMinMax[counter++] = vAxisMinMaxValues[GlUtils.MAX_VALUE];
        }
        vAxisMinMaxValues = GlUtils.getMinMax(allVAxisMinMax);
        final float min = Math.round(vAxisMinMaxValues[GlUtils.MIN_VALUE] * 1000f + 1f) / 1000f;
        final float max = Math.round(vAxisMinMaxValues[GlUtils.MAX_VALUE] * 1000f - 1f) / 1000f;
        final float middle = (max + min) * .5f;
        final float[] vAxisValues = new float[5];
        vAxisValues[0] = min;
        vAxisValues[1] = (middle + min) * .5f;
        vAxisValues[2] = middle;
        vAxisValues[3] = (max + middle) * .5f;
        vAxisValues[4] = max;

        graphAxes.draw(gl, x, y, w, h, H_AXIS_VALUES, H_AXIS_MIN_VALUE, H_AXIS_MAX_VALUE, vAxisValues, min, max);
        float graphX = x + graphAxes.getVAxisOffset();
        float graphY = y + graphAxes.getHAxisOffset();
        float graphW = w - graphAxes.getVAxisOffset();
        float graphH = h - graphAxes.getHAxisOffset();

        gl.glPushMatrix();
        gl.glTranslatef(graphX + graphW * .5f, graphY, 0f);
        dashedVLine.draw(gl, 0f, graphH, ZERO_DASH_LINE_SIZE, ZERO_DASH_LINE_WIDTH, Colors.GRAY_LIGHT);
        gl.glPopMatrix();

        int event;
        for (int i = 0; i < length; i++) {
            //final int dataLength = data.getMean()[i].length;
            //if (mappedData == null || mappedData.length < dataLength) mappedData = new float[dataLength];
            //JniUtils.map(mappedData, data.getMean()[i], dataLength, vAxisMinMaxValues[GlUtils.MIN_VALUE],
            //    vAxisMinMaxValues[GlUtils.MAX_VALUE], min / vAxisMinMaxValues[GlUtils.MIN_VALUE],
            //    max / vAxisMinMaxValues[GlUtils.MAX_VALUE]);
            //JniUtils.map(mappedData, data.getNormalizedAverage()[i], dataLength, -1f, 1f,
            //    vAxisMinMaxValues[GlUtils.MIN_VALUE] / min, max);

            event = Integer.valueOf(data.getEvents()[i]);
            lineGraph.draw(gl, graphX, graphY, graphW, graphH, data.getNormalizedAverage()[i],
                Colors.MARKER_COLORS[event % Colors.MARKER_COLORS.length]);
        }
    }
}