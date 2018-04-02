package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import javax.microedition.khronos.opengles.GL10;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private GlMeasurementArea glMeasurementArea;

    private boolean measuring;
    private float measuringAreaX1;
    private float measuringAreaX2;

    public SeekableWaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        glMeasurementArea = new GlMeasurementArea();
    }

    @Override
    protected void drawingHandler(GL10 gl, @NonNull float[] waveformVertices, @NonNull SparseArray<String> markers,
        int glWindowWidth, int glWindowHeight, float scaleX, float scaleY) {
        // draw measurement area
        if (measuring) {
            glMeasurementArea.draw(gl, measuringAreaX1 * scaleX, measuringAreaX2 * scaleX, -glWindowHeight * .5f,
                glWindowHeight * .5f);
        }

        super.drawingHandler(gl, waveformVertices, markers, glWindowWidth, glWindowHeight, scaleX, scaleY);
    }

    @Override protected void onMeasurementStart(float x) {
        measuringAreaX1 = x;
        measuringAreaX2 = x;
        measuring = true;
    }

    @Override protected void onMeasurement(float dx) {
        measuringAreaX2 += dx;
    }

    @Override protected void onMeasurementEnd(float x) {
        measuring = false;
        measuringAreaX1 = 0;
        measuringAreaX2 = 0;
    }
}