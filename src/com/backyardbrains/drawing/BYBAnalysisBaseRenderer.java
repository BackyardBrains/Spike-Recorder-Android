
package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.audio.AudioService;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

public class BYBAnalysisBaseRenderer implements GLSurfaceView.Renderer {
	private static final String	TAG						= BYBBaseRenderer.class.getCanonicalName();

	protected int				height;
	protected int				width;

	protected Context			context;
	protected float [] floatData = null;

	// ----------------------------------------------------------------------------------------
	public BYBAnalysisBaseRenderer(Context context) {
		this.context = context.getApplicationContext();
	}
	public void setFloatData(float [] d){
		floatData = d;
	}
	// ----------------------------------------------------------------------------------------
	public void close() {
	}
	// ----------------------------------------------------------------------------------------
	FloatBuffer getFloatBufferFromFloatArray(final float[] array) {
		final FloatBuffer buf;
		final ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	// ----------------------------------------------------------------------------------------
	protected boolean isValidAudioBuffer() {
		return floatData != null && floatData.length > 0;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onDrawFrame(GL10 gl) {
		// Log.d(TAG, "onDrawFrame");
		// grab current audio from audioservice
		preDrawingHandler();
		glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
	}

	// ----------------------------------------------------------------------------------------
	protected void preDrawingHandler() {}
	// ----------------------------------------------------------------------------------------
	protected void postDrawingHandler(GL10 gl) {}
	// ----------------------------------------------------------------------------------------
	protected void drawingHandler(GL10 gl) {}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.d(TAG, "onSurfaceChanged " + width + ", " + height );
		this.width = width;
		this.height = height;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}

	// ----------------------------------------------------------------------------------------
	protected void initGL(GL10 gl){///, float xBegin, float xEnd, float scaledYBegin, float scaledYEnd) {
		// set viewport
		gl.glViewport(0, 0, width, height);

		glClear(gl);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0f, width,height,0f, -1f, 1f);
		gl.glRotatef(0f, 0f, 0f, 1f);

		// Blackout, then we're ready to draw! \o/
		// mGL.glEnable(GL10.GL_TEXTURE_2D);
		gl.glClearColor(0f, 0f, 0f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		// Enable Blending
		gl.glEnable(GL10.GL_BLEND);
		// Specifies pixel arithmetic
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	// ----------------------------------------------------------------------------------------
	protected void glClear(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	// ----------------------------------------------------------------------------------------
	int map(float glHeight, int in_min, int in_max, int out_min, int out_max) {
		return (int) ((glHeight - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
	}
}
