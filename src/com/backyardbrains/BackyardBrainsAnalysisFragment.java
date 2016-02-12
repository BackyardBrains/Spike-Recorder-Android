package com.backyardbrains;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


import com.backyardbrains.audio.RecordingReader;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class BackyardBrainsAnalysisFragment extends Fragment {

		private static final String					TAG					= BackyardBrainsAnalysisFragment.class.getCanonicalName();
		
		
		protected ContinuousGLSurfaceView			mAndroidSurface		= null;
		private FrameLayout							mainscreenGLLayout;
		private SharedPreferences					settings			= null;
		private Context								context				= null;
		private boolean								bRenderersCreated	= false;
		protected int								currentRenderer		= -1;
		
		private RecordingReader			reader		= null;
		private AudioFileReadListener	audioFileReadListener;
		private boolean					bFileLoaded	= false;
	
	
		
	// ----------------------------------------------------------------------------------------
		public BackyardBrainsAnalysisFragment(Context context) {
			super();
			this.context = context.getApplicationContext();
			Log.d("BackyardBrainsAnalysisFragment", "Constructor");
		}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- FRAGMENT LIFECYCLE
	// -----------------------------------------------------------------------------------------------------------------------------
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			if (context != null) {
				createRenderers();
			
			} else {
				Log.d(TAG, "onCreate failed, context == null");
			}
		}

	// ----------------------------------------------------------------------------------------
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.backyard_main, container, false);
			getSettings();
			mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);
			Log.d(TAG,"onCreateView");
	
			return rootView;

		}
	// ----------------------------------------------------------------------------------------
		@Override
		public void onStart() {
			super.onStart();
		}
	// ----------------------------------------------------------------------------------------
		@Override
		public void onResume() {
			super.onResume();
		}

	// ----------------------------------------------------------------------------------------
		@Override
		public void onPause() {
			super.onPause();
		}

	// ----------------------------------------------------------------------------------------
		@Override
		public void onStop() {
			super.onStop();
		}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- GL RENDERING
	// -----------------------------------------------------------------------------------------------------------------------------
		private void createRenderers() {
//			
			bRenderersCreated = true;
		}

	// ----------------------------------------------------------------------------------------
		public void setRenderer(int i) {
//			if (i == WAVE || i == THRESH) {
				currentRenderer = i;
				reassignSurfaceView();
//			}
		}

	// ----------------------------------------------------------------------------------------
		protected void reassignSurfaceView() {
			if (context != null && bRenderersCreated) {
				mAndroidSurface = null;
				mainscreenGLLayout.removeAllViews();
//				if (currentRenderer < 1) {
//					setGlSurface(waveRenderer);
//					mScaleListener.setRenderer(waveRenderer);
//					currentRenderer = WAVE;
//				} else {
//					setGlSurface(threshRenderer);
//					mScaleListener.setRenderer(threshRenderer);
//					currentRenderer = THRESH;
//				}
				mainscreenGLLayout.addView(mAndroidSurface);

				readSettings();

			// enableUiForActivity();
				Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
			}
		}

	// ----------------------------------------------------------------------------------------
		protected void setGlSurface(BYBBaseRenderer renderer) {
			if (context != null) {
				if (mAndroidSurface != null) {
					mAndroidSurface = null;
				}
				mAndroidSurface = new ContinuousGLSurfaceView(context, renderer);
				Log.d(TAG, "setGLSurface oK.");
			} else {
				Log.d(TAG, "setGLSurface failed. Context == null.");
			}
		}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- TOUCH
	// -----------------------------------------------------------------------------------------------------------------------------
		public boolean onTouchEvent(MotionEvent event) {
		
			return mAndroidSurface.onTouchEvent(event);
		}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- SETTINGS
	// -----------------------------------------------------------------------------------------------------------------------------
		private void getSettings() {
			if (settings == null) {
			// settings = (
			// context).getPreferences(BackyardBrainsMain.MODE_PRIVATE);
			}
		}

	// ----------------------------------------------------------------------------------------
		protected void readSettings() {
			if (settings != null) {
//				waveRenderer.setAutoScaled(settings.getBoolean("waveRendererAutoscaled", waveRenderer.isAutoScaled()));
//				waveRenderer.setGlWindowHorizontalSize(settings.getInt("waveRendererGlWindowHorizontalSize", waveRenderer.getGlWindowHorizontalSize()));
//				waveRenderer.setGlWindowVerticalSize(settings.getInt("waveRendererGlWindowVerticalSize", waveRenderer.getGlWindowVerticalSize()));
//
//				threshRenderer.setAutoScaled(settings.getBoolean("threshRendererAutoscaled", threshRenderer.isAutoScaled()));
//				threshRenderer.setGlWindowHorizontalSize(settings.getInt("threshRendererGlWindowHorizontalSize", threshRenderer.getGlWindowHorizontalSize()));
//				threshRenderer.setGlWindowVerticalSize(settings.getInt("threshRendererGlWindowVerticalSize", threshRenderer.getGlWindowVerticalSize()));
			}
		}

	// ----------------------------------------------------------------------------------------
		protected void saveSettings() {
			if (settings != null) {
				final SharedPreferences.Editor editor = settings.edit();
//				editor.putBoolean("waveRendererAutoscaled", waveRenderer.isAutoScaled());
//				editor.putInt("waveRendererGlWindowHorizontalSize", waveRenderer.getGlWindowHorizontalSize());
//				editor.putInt("waveRendererGlWindowVerticalSize", waveRenderer.getGlWindowVerticalSize());
//
//				editor.putBoolean("threshRendererAutoscaled", threshRenderer.isAutoScaled());
//				editor.putInt("threshRendererGlWindowHorizontalSize", threshRenderer.getGlWindowHorizontalSize());
//				editor.putInt("threshRendererGlWindowVerticalSize", threshRenderer.getGlWindowVerticalSize());
				editor.commit();
			}
		}
		public void load(String filePath) {
			load(new File(filePath));
		}

		public void load(File f) {
			if (context != null) {
				if (f.exists()) {
					reader = null;
					registerAudioFileReadReceiver(true);
					reader = new RecordingReader(f, context);
					bFileLoaded = false;
				}else{
					Log.d("AudioFilePlayer","Cant load file: it doent exist!!");
				}
			}
		}

		// -----------------------------------------------------------------------------------------------------------------------------
		// ----------------------------------------- BROADCAST RECEIVERS CLASS
		// -----------------------------------------------------------------------------------------------------------------------------
		private class AudioFileReadListener extends BroadcastReceiver {
			@Override
			public void onReceive(android.content.Context context, android.content.Intent intent) {
				Log.d("AudioFileReadListener", "onReceive");
				bFileLoaded = true;
				
				registerAudioFileReadReceiver(false);
			};
		}

		// -----------------------------------------------------------------------------------------------------------------------------
		// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
		// -----------------------------------------------------------------------------------------------------------------------------
		
		private void registerAudioFileReadReceiver(boolean reg) {
			if (reg) {
				IntentFilter intentFilter = new IntentFilter("BYBAudioFileRead");
				audioFileReadListener = new AudioFileReadListener();
				context.registerReceiver(audioFileReadListener, intentFilter);
			} else {
				context.unregisterReceiver(audioFileReadListener);
				audioFileReadListener = null;
			}
		}
}
