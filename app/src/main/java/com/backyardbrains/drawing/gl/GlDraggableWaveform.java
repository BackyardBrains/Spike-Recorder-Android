package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlDraggableWaveform {

    private final GlWaveform glWaveform;
    private final GlHandle glHandle;
    private final Rect dragArea;

    public GlDraggableWaveform() {
        glWaveform = new GlWaveform();
        glHandle = new GlHandle();

        dragArea = new Rect();
    }

    public void getDragArea(@NonNull Rect rect) {
        rect.set(dragArea.x, dragArea.y, dragArea.width, dragArea.height);
    }

    public void draw(@NonNull GL10 gl, short[] waveformVertices, int waveformVerticesCount, float waveformScaleFactor,
        float waveformPosition, float scaleX, float scaleY, @NonNull @Size(4) float[] color, boolean selected,
        boolean showWaveformHandle) {

        gl.glPushMatrix();
        gl.glScalef(1f, waveformScaleFactor, 1f);
        glWaveform.draw(gl, waveformVertices, waveformVerticesCount, color);
        gl.glPopMatrix();

        if (showWaveformHandle) {
            gl.glPushMatrix();
            gl.glScalef(scaleX, scaleY, 1f);
            glHandle.draw(gl, color, selected);
            gl.glPopMatrix();

            glHandle.getBorders(dragArea);
            dragArea.scale(scaleX, scaleY);
            dragArea.y += waveformPosition;
        }
    }
}
