package com.backyardbrains.drawing.gl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import com.android.texample.GLText;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.vo.EventTriggeredAverages;
import java.text.NumberFormat;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlEventTriggeredAveragesGraph {

    private static final float[] H_AXIS_VALUES = new float[7];
    private static final float AVG_LINE_WIDTH = 4f;
    private static final float MC_LINE_WIDTH = 2f;
    private static final float TEXT_SIZE_SP = 14f;
    private static final float MARGIN_DP = 5f;
    private static final float LEGEND_ITEM_Y_OFFSET_DP = 5f;
    private static final float H_AXIS_MIN_VALUE = -0.7f;
    private static final float H_AXIS_MAX_VALUE = 0.7f;
    private static final int ZERO_DASH_LINE_WIDTH = 2;
    private static final float ZERO_DASH_LINE_SIZE = 15f;
    private static final String MC_LEGEND_ITEM = "-Monte Carlo";
    private static final String MC_LEGEND_SD_PLUS = "+2SD";
    private static final String MC_LEGEND_SD_MINUS = "-2SD";
    private static final String EVENT_LEGEND_ITEM_PREFIX = "-Event ";

    static {
        H_AXIS_VALUES[0] = -0.6f;
        H_AXIS_VALUES[1] = -0.4f;
        H_AXIS_VALUES[2] = -0.2f;
        H_AXIS_VALUES[3] = 0.0f;
        H_AXIS_VALUES[4] = 0.2f;
        H_AXIS_VALUES[5] = 0.4f;
        H_AXIS_VALUES[6] = 0.6f;
    }

    private final GlLineGraph glLineGraph;
    private final GlGraphAxes glGraphAxes;
    private final GlDashedVLine glDashedVLine;
    private final GlRectangle glRectangle;
    private final GLText glText;

    private float[] mappedData;
    private float margin;
    private float legendItemYOffset;

    public GlEventTriggeredAveragesGraph(@NonNull Context context, @NonNull GL10 gl,
        @Nullable NumberFormat hAxisValueFormatter, @Nullable NumberFormat vAxisValueFormatter) {
        glLineGraph = new GlLineGraph();
        glGraphAxes = new GlGraphAxes(context, gl, hAxisValueFormatter, vAxisValueFormatter);
        glDashedVLine = new GlDashedVLine();
        glRectangle = new GlRectangle();
        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", (int) ViewUtils.spToPx(context.getResources(), TEXT_SIZE_SP), 0, 0);

        margin = ViewUtils.dpToPx(context.getResources(), MARGIN_DP);
        legendItemYOffset = ViewUtils.dpToPx(context.getResources(), LEGEND_ITEM_Y_OFFSET_DP);
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull EventTriggeredAverages data) {
        final int eventCount = data.getAverages().length;
        final float min = data.getMin();
        final float max = data.getMax();
        final float middle = (max + min) * .5f;
        final float[] vAxisValues = new float[5];
        vAxisValues[0] = min;
        vAxisValues[1] = (middle + min) * .5f;
        vAxisValues[2] = middle;
        vAxisValues[3] = (max + middle) * .5f;
        vAxisValues[4] = max;

        if (min > max || Float.isInfinite(min) || Float.isInfinite(max)) return;

        final float graphVAxisMin = min * 1.1f;
        final float graphVAxisMax = max * 1.1f;
        // draw graph axes
        glGraphAxes.draw(gl, x, y, w, h, H_AXIS_VALUES, H_AXIS_MIN_VALUE, H_AXIS_MAX_VALUE, vAxisValues, graphVAxisMin,
            graphVAxisMax);
        float graphX = x + glGraphAxes.getVAxisOffset();
        float graphY = y + glGraphAxes.getHAxisOffset();
        float graphW = w - glGraphAxes.getVAxisOffset();
        float graphH = h - glGraphAxes.getHAxisOffset();

        gl.glPushMatrix();
        gl.glTranslatef(graphX + graphW * .5f, graphY, 0f);
        // draw the event line
        glDashedVLine.draw(gl, 0f, graphH, ZERO_DASH_LINE_SIZE, ZERO_DASH_LINE_WIDTH, Colors.WHITE);
        gl.glPopMatrix();

        int event;
        final int legendDataLength = eventCount + (data.isShowConfidenceIntervals() ? 1 : 0);
        final String[] legendData = new String[legendDataLength];
        final float[][] legendDataColors = new float[legendDataLength][];
        for (int i = 0; i < eventCount; i++) {
            event = Integer.valueOf(data.getEvents()[i]);
            // draw graph average current event
            drawGraph(gl, graphX, graphY, graphW, graphH, data.getNormAverages()[i], AVG_LINE_WIDTH,
                Colors.MARKER_COLORS[event % Colors.MARKER_COLORS.length]);

            // set data for graph legend
            legendData[i] = EVENT_LEGEND_ITEM_PREFIX + event;
            legendDataColors[i] = Colors.MARKER_COLORS[event % Colors.MARKER_COLORS.length];
        }

        if (data.isShowConfidenceIntervals()) {
            // draw +2*SD graph
            drawGraph(gl, graphX, graphY, graphW, graphH, data.getNormMonteCarloTop(), MC_LINE_WIDTH,
                Colors.GRAY_LIGHT);
            // draw SD average graph
            drawGraph(gl, graphX, graphY, graphW, graphH, data.getNormMonteCarloAverages(), AVG_LINE_WIDTH,
                Colors.BLUE_LIGHT);
            // draw -2*SD graph
            drawGraph(gl, graphX, graphY, graphW, graphH, data.getNormMonteCarloBottom(), MC_LINE_WIDTH,
                Colors.GRAY_LIGHT);

            // set data for graph legend
            legendData[eventCount] = MC_LEGEND_ITEM;
            legendDataColors[eventCount] = Colors.BLUE_LIGHT;
        }

        // draw graph legend
        drawLegend(gl, graphX, graphY, graphW, graphH, legendData, legendDataColors);

        // draw MC 2SD labels
        if (data.isShowConfidenceIntervals()) {
            gl.glEnable(GL10.GL_TEXTURE_2D);
            glText.begin(Colors.GRAY_LIGHT[0], Colors.GRAY_LIGHT[1], Colors.GRAY_LIGHT[2], Colors.GRAY_LIGHT[3]);

            float[] minMax = new float[2];
            float[] tmpMinMax = new float[2];
            int sampleCount = data.getNormMonteCarloAverages().length;
            JniUtils.minMax(minMax, data.getNormMonteCarloTop(), sampleCount);
            JniUtils.map(tmpMinMax, minMax, 2, -1f, 1f, -0.9f, 0.9f);
            JniUtils.map(minMax, tmpMinMax, 2, -1f, 1f, 0f, graphH);
            glText.draw(MC_LEGEND_SD_PLUS, graphX + margin, graphY + minMax[1]);
            JniUtils.minMax(minMax, data.getNormMonteCarloBottom(), sampleCount);
            JniUtils.map(tmpMinMax, minMax, 2, -1f, 1f, -0.9f, 0.9f);
            JniUtils.map(minMax, tmpMinMax, 2, -1f, 1f, 0f, graphH);
            glText.draw(MC_LEGEND_SD_MINUS, graphX + margin, graphY + minMax[0] - glText.getHeight());

            glText.end();
            gl.glDisable(GL10.GL_TEXTURE_2D);
        }
    }

    private void drawGraph(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull float[] data, float lineWidth,
        @NonNull @Size(4) float[] lineColor) {
        int dataLength = data.length;
        if (mappedData == null || mappedData.length != dataLength) mappedData = new float[dataLength];
        JniUtils.map(mappedData, data, dataLength, -1f, 1f, -0.9f, 0.9f);

        glLineGraph.draw(gl, x, y, w, h, mappedData, lineWidth, lineColor);
    }

    private void drawLegend(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull String[] data,
        @NonNull float[][] colors) {
        int length = data.length;
        float textW;
        float maxTextW = 0f;
        float textH = glText.getHeight();
        float[] yCoords = new float[length];
        for (String item : data) {
            textW = glText.getLength(item);
            if (maxTextW < textW) maxTextW = textW;
        }
        x = x + w - (3 * margin + maxTextW);
        y = y + h - (3 * margin + textH * length + legendItemYOffset * (length - 1));
        w = maxTextW + 2 * margin;
        h = textH * length + legendItemYOffset * (length - 1) + 2 * margin;
        int counter = 0;
        for (int i = length - 1; i >= 0; i--) {
            yCoords[counter++] = y + margin + textH * i + legendItemYOffset * i;
        }

        // draw legend background
        gl.glPushMatrix();
        gl.glTranslatef(x, y, 0f);
        glRectangle.draw(gl, w, h, Colors.BLACK, Colors.WHITE);
        gl.glPopMatrix();

        // draw legend items
        gl.glEnable(GL10.GL_TEXTURE_2D);
        for (int i = 0; i < data.length; i++) {
            glText.begin(colors[i][0], colors[i][1], colors[i][2], colors[i][3]);
            glText.draw(data[i], x + margin, yCoords[i]);
            glText.end();
        }
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}