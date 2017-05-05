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

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class WaveformRenderer extends BYBBaseRenderer {

    public WaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);
    }

    @Override protected void drawingHandler(GL10 gl) {
        setGlWindow(gl, getGlWindowHorizontalSize(), mBufferToDraws.length);

        autoScaleCheck();

        final FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraws);
        if (mVertexBuffer != null) {
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glLineWidth(1f);
            gl.glColor4f(0f, 1f, 0f, 1f);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        }
    }
}