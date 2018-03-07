package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import javax.microedition.khronos.opengles.GL10;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private EventMarker eventMarker;

    public SeekableWaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);
    }

    @Override protected void drawingHandler(GL10 gl) {
        super.drawingHandler(gl);
        //gl.glMatrixMode(GL10.GL_MODELVIEW);
        //gl.glLoadIdentity();
        //
        //// save window hor/ver sizes before drawing cause they could change while drawing
        //int glWindowHorizontalSize = getGlWindowHorizontalSize();
        //int glWindowVerticalSize = getGlWindowVerticalSize();
        //
        //final SparseArray<String> markers = new SparseArray<>();
        //final FloatBuffer mVertexBuffer = getWaveformBuffer(drawingBuffer, markers, glWindowHorizontalSize);
        //if (mVertexBuffer != null) {
        //    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        //    gl.glLineWidth(1f);
        //    gl.glColor4f(0f, 1f, 0f, 1f);
        //    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
        //    gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (mVertexBuffer.limit() * .5));
        //    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        //}
        //
        //for (int i = 0; i < markers.size(); i++) {
        //    eventMarker.draw(gl, markers.valueAt(i), markers.keyAt(i), -glWindowVerticalSize * .5f,
        //        glWindowVerticalSize * .5f, getScaleX(glWindowHorizontalSize), getScaleY(glWindowVerticalSize));
        //}
    }
}