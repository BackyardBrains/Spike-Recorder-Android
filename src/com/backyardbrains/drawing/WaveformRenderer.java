/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BYBBaseRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    private EventMarker eventMarker;
    private Context context;

    public WaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        this.context = fragment.getContext();
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        eventMarker = new EventMarker(context, gl);
    }

    @Override protected void drawingHandler(GL10 gl) {
        autoScaleCheck();

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        // save window hor/ver sizes before drawing cause they could change while drawing
        int glWindowHorizontalSize = getGlWindowHorizontalSize();
        int glWindowVerticalSize = getGlWindowVerticalSize();

        final SparseArray<String> markers = new SparseArray<>();
        final FloatBuffer mVertexBuffer = getWaveformBuffer(drawingBuffer, markers, glWindowHorizontalSize);
        if (mVertexBuffer != null) {
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glLineWidth(1f);
            gl.glColor4f(0f, 1f, 0f, 1f);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (mVertexBuffer.limit() * .5));
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        }

        for (int i = 0; i < markers.size(); i++) {
            eventMarker.draw(gl, markers.valueAt(i), markers.keyAt(i), -glWindowVerticalSize * .5f,
                glWindowVerticalSize * .5f, getScaleX(glWindowHorizontalSize), getScaleY(glWindowVerticalSize));
        }
    }
}