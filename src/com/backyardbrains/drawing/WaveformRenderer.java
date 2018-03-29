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
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BYBBaseRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    private GlWaveform glWaveform;
    private GlEventMarker glEventMarker;
    private Context context;

    public WaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        context = fragment.getContext();
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glWaveform = new GlWaveform();
        glEventMarker = new GlEventMarker(context, gl);
    }

    @Override
    protected void drawingHandler(GL10 gl, @NonNull float[] waveformVertices, @NonNull SparseArray<String> markers,
        int glWindowWidth, int glWindowHeight, float scaleX, float scaleY) {

        // draw waveform
        glWaveform.draw(gl, waveformVertices);
        // draw markers
        final float verticalHalfSize = glWindowHeight * .5f;
        for (int i = 0; i < markers.size(); i++) {
            glEventMarker.draw(gl, markers.valueAt(i), markers.keyAt(i), -verticalHalfSize, verticalHalfSize, scaleX,
                scaleY);
        }
    }
}