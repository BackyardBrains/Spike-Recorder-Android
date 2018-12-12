package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.drawing.gl.GlHandleDragHelper.Rect;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlDraggableThreshold {

    private static final float DASH_SIZE = 30f;
    private static final int LINE_WIDTH = 1;

    private final GlDashedHLine glDashedHLine;
    private final GlHandle glHandle;
    private final Rect dragArea;

    public GlDraggableThreshold() {
        glDashedHLine = new GlDashedHLine();
        glHandle = new GlHandle();

        dragArea = new Rect();
    }

    public void getDragArea(Rect rect) {
        rect.set(dragArea.x, dragArea.y, dragArea.width, dragArea.height);
    }

    public void draw(@NonNull GL10 gl, float x1, float x2, float y, float waveformScaleFactor, float waveformPosition,
        float scaleX, float scaleY, @NonNull @Size(4) float[] color) {
        gl.glPushMatrix();
        gl.glTranslatef(0f, y * waveformScaleFactor + waveformPosition, 0f);
        gl.glScalef(scaleX, scaleY, 1f);
        glDashedHLine.draw(gl, x1, x2, DASH_SIZE, LINE_WIDTH, color);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslatef(x2, y * waveformScaleFactor + waveformPosition, 0f);
        gl.glScalef(-scaleX, scaleY, 1f);
        glHandle.draw(gl, color, true);
        gl.glPopMatrix();

        glHandle.getBorders(dragArea);
        dragArea.scale(scaleX, scaleY);
        dragArea.x += x2 - dragArea.width;
        dragArea.y += y * waveformScaleFactor + waveformPosition;
    }
}
