package com.backyardbrains.drawing.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlMeasurementArea {

    private static final float LIMIT_LINES_WIDTH = 2f;
    private static final int LINES_VERTICES_COUNT = 8;
    private static final float AREA_LINE_WIDTH = 1f;
    private static final int AREA_VERTICES_COUNT = 8;
    private static final short[] INDICES = { 0, 1, 2, 0, 2, 3 };

    private final FloatBuffer limitLinesVFB;
    private final FloatBuffer areaVFB;
    private final ShortBuffer indicesBuffer;

    private final float[] limitLinesVertices = new float[8];
    private final float[] areaVertices = new float[8];

    public GlMeasurementArea() {
        ByteBuffer limitLinesVBB = ByteBuffer.allocateDirect(LINES_VERTICES_COUNT * 4);
        limitLinesVBB.order(ByteOrder.nativeOrder());
        limitLinesVFB = limitLinesVBB.asFloatBuffer();

        ByteBuffer areaVBB = ByteBuffer.allocateDirect(AREA_VERTICES_COUNT * 4);
        areaVBB.order(ByteOrder.nativeOrder());
        areaVFB = areaVBB.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(INDICES.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indicesBuffer = ibb.asShortBuffer();
        indicesBuffer.put(INDICES);
        indicesBuffer.position(0);
    }

    public void draw(GL10 gl, float x0, float x1, float y0, float y1) {
        // draw limit lines
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(0.8f, 0.8f, 0.8f, 1f);
        gl.glLineWidth(LIMIT_LINES_WIDTH);
        limitLinesVertices[0] = x0;
        limitLinesVertices[1] = y0;
        limitLinesVertices[2] = x0;
        limitLinesVertices[3] = y1;
        limitLinesVertices[4] = x1;
        limitLinesVertices[5] = y0;
        limitLinesVertices[6] = x1;
        limitLinesVertices[7] = y1;
        limitLinesVFB.put(limitLinesVertices);
        limitLinesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, limitLinesVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, 4);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw measurement area
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(0.4f, 0.4f, 0.4f, 0.5f);
        gl.glLineWidth(AREA_LINE_WIDTH);
        areaVertices[0] = x0;
        areaVertices[1] = y0;
        areaVertices[2] = x0;
        areaVertices[3] = y1;
        areaVertices[4] = x1;
        areaVertices[5] = y1;
        areaVertices[6] = x1;
        areaVertices[7] = y0;
        areaVFB.put(areaVertices);
        areaVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, areaVFB);
        gl.glDrawElements(GL10.GL_TRIANGLES, INDICES.length, GL10.GL_UNSIGNED_SHORT, indicesBuffer);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
