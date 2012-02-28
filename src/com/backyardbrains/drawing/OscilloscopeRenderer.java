package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.opengl.GLSurfaceView;
import android.os.IBinder;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;

public class OscilloscopeRenderer implements GLSurfaceView.Renderer {

	private AudioService mAudioService;
	private int glHorizontalSize = 4000;
	private int glVerticalSize = 10000;
	private Activity context;

	public OscilloscopeRenderer(Activity backyardAndroidActivity) {
		context = backyardAndroidActivity;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO Auto-generated method stub
		short[] audioBuffer = getCurrentAudio();
		if (!isValidAudioBuffer(audioBuffer)) {
			return;
		}
		
		float[] arr = new float[audioBuffer.length * 2]; // array to fill
		int j = 0; // index of arr
		float interval = 1;
		for (int i = 0; i < audioBuffer.length; i++) {
			arr[j++] = i * interval;
			arr[j++] = audioBuffer[i];
		}
		final ByteBuffer temp = ByteBuffer.allocateDirect(arr.length * 4);
		temp.order(ByteOrder.nativeOrder());
		final FloatBuffer buf = temp.asFloatBuffer();
		buf.put(arr);
		buf.position(0);

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	
		gl.glOrthof(audioBuffer.length - glHorizontalSize, audioBuffer.length, -glVerticalSize/2, glVerticalSize/2, -1f, 1f);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glLineWidth(1f);
		gl.glColor4f(0f, 1f, 0f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buf);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, buf.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	private boolean isValidAudioBuffer(short[] audioBuffer) {
		return audioBuffer != null && audioBuffer.length > 0;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glRotatef(0f, 0f, 0f, 1f);
		gl.glClearColor(0f, 0f, 0f, 0.5f);
		gl.glClearDepthf(1.0f);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		
		Intent intent = new Intent(context, AudioService.class);
		context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}

	
	protected  short[] getCurrentAudio () {
		return mAudioService.getAudioBuffer();
	}
	
	protected ServiceConnection mConnection = new ServiceConnection() {

		/**
		 * Sets a reference in this activity to the {@link AudioService}, which
		 * allows for {@link ByteBuffer}s full of audio information to be passed
		 * from the {@link AudioService} down into the local
		 * {@link OscilloscopeGLSurfaceView}
		 * 
		 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
		 *      android.os.IBinder)
		 */
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			AudioServiceBinder binder = (AudioServiceBinder) service;
			mAudioService = binder.getService();
		}

		/**
		 * Clean up bindings
		 * 
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioService = null;
		}
	};

}
