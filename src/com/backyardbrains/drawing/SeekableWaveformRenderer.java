package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SeekableWaveformRenderer extends BYBBaseRenderer {

    private static final String TAG = makeLogTag(SeekableWaveformRenderer.class);

    long fromSample;
    long toSample;

    public SeekableWaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);
    }

    @Override public void onDrawFrame(GL10 gl) {
        // let's save start and end sample positions that are being drawn before triggering the actual draw
        toSample = getAudioService() != null ? getAudioService().getPlaybackProgress() : 0;
        fromSample = Math.max(0, toSample - getGlWindowHorizontalSize());
        //LOGD(TAG, "from: " + fromSample + ", to: " + toSample + ", horizontal: " + getGlWindowHorizontalSize());

        super.onDrawFrame(gl);
    }

    @Override protected void drawingHandler(GL10 gl) {
        setGlWindow(gl, getGlWindowHorizontalSize(), drawingBuffer.length);

        final FloatBuffer mVertexBuffer = getWaveformBuffer(drawingBuffer);
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