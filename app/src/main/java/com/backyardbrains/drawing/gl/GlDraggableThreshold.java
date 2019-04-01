package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import com.backyardbrains.utils.ThresholdOrientation;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlDraggableThreshold {

    private static final float DASH_SIZE = 30f;
    private static final int LINE_WIDTH = 1;

    private final GlDashedHLine glDashedHLine;
    private final GlHLine glHLine;
    private final GlHandle glHandle;
    private final Rect dragArea;

    private final @ThresholdOrientation int orientation;
    private final boolean dashed;

    public GlDraggableThreshold() {
        this(ThresholdOrientation.LEFT);
    }

    public GlDraggableThreshold(@ThresholdOrientation int orientation) {
        this(orientation, false);
    }

    public GlDraggableThreshold(@ThresholdOrientation int orientation, boolean dashed) {
        glDashedHLine = new GlDashedHLine();
        glHLine = new GlHLine();
        glHandle = new GlHandle();

        dragArea = new Rect();

        this.orientation = orientation;
        this.dashed = dashed;
    }

    public void getDragArea(Rect rect) {
        rect.set(dragArea.x, dragArea.y, dragArea.width, dragArea.height);
    }

    public void draw(@NonNull GL10 gl, float x1, float x2, float y, float waveformScaleFactor, float waveformPosition,
        float scaleX, float scaleY, @NonNull @Size(4) float[] color) {
        gl.glPushMatrix();
        gl.glScalef(scaleX, waveformScaleFactor, 1f);
        gl.glTranslatef(0f, waveformPosition + y, 0f);
        if (dashed) {
            glDashedHLine.draw(gl, x1, x2, DASH_SIZE, LINE_WIDTH, color);
        } else {
            glHLine.draw(gl, x1, x2, LINE_WIDTH, color);
        }
        gl.glPopMatrix();

        gl.glPushMatrix();
        if (orientation == ThresholdOrientation.LEFT) {
            gl.glTranslatef(0f, y * waveformScaleFactor + waveformPosition, 0f);
            gl.glScalef(scaleX, scaleY, 1f);
        } else {
            gl.glTranslatef(x2, y * waveformScaleFactor + waveformPosition, 0f);
            gl.glScalef(-scaleX, scaleY, 1f);
        }
        glHandle.draw(gl, color, true);
        gl.glPopMatrix();

        glHandle.getBorders(dragArea);
        dragArea.scale(scaleX, scaleY);
        dragArea.x += orientation == ThresholdOrientation.LEFT ? x1 : x2 - dragArea.width;
        dragArea.y += y * waveformScaleFactor + waveformPosition;
    }
}
