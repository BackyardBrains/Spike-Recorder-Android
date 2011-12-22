package com.backyardbrains;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.backyardbrains.drawing.OscilloscopeGLSurfaceView;

/**
 * Primary activity of the Backyard Brains app. By default shows the continuous
 * oscilloscope view for use with the spikerbox
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
public class BackyardAndroidActivity extends Activity {

	/**
	 * Reference to the {@link OscilloscopeGLSurfaceView} to draw in this
	 * activity
	 */
	private OscilloscopeGLSurfaceView mAndroidSurface;
	/**
	 * Reference to the {@link BackyardBrainsApplication} for message passing
	 */
	private BackyardBrainsApplication application;
	private TextView msView;
	private ImageButton mRecordButton;
	private UpdateMillisecondsReciever upmillirec;
	private boolean isRecording = false;
	private View tapToStopRecView;
	private View mFileButton;
	private TextView mVView;
	private BroadcastReceiver upmillivolt;
	private SetMillivoltViewSizeReceiver milliVoltSize;
	private View recordingBackground;
	private ShowRecordingButtonsReceiver showRecordingButtonsReceiver;
	private FrameLayout mainscreenGLLayout;
	private boolean triggerMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backyard_main);

		// get application
		application = (BackyardBrainsApplication) getApplication();

		msView = (TextView) findViewById(R.id.millisecondsView);
		mVView = (TextView) findViewById(R.id.mVLabelView);

		mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);

		BybConfigHolder oldConfig = (BybConfigHolder) getLastNonConfigurationInstance();
		reassignSurfaceView(false);
		if (oldConfig != null) {
			mAndroidSurface.setScaleFactor(oldConfig.configScaleFactor);
			mAndroidSurface.setBufferLengthDivisor(oldConfig.configBufferLengthDivisor);
			Log.d("BYBAndroidActivity", "Setting surface AutoScaled to " + oldConfig.configAlreadyAutoScaled);
			mAndroidSurface.setAutoScaled(oldConfig.configAlreadyAutoScaled);
		}		
		setUpRecordingButtons();
		
	}
	
	public BybConfigHolder collectConfigFromSurface () {
		BybConfigHolder bch = new BybConfigHolder();
		bch.configScaleFactor = mAndroidSurface.getScaleFactor();
		bch.configBufferLengthDivisor = mAndroidSurface.getBufferLengthDivisor();
		bch.configAlreadyAutoScaled = mAndroidSurface.isAutoScaled();
		return bch;
	}

	@Override
	public BybConfigHolder onRetainNonConfigurationInstance() {
		return collectConfigFromSurface();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.waveview:
			// mAndroidSurface.setContinuousViewMode();
			triggerMode = false;
			reassignSurfaceView(triggerMode);
			return true;
		case R.id.threshold:
			// mAndroidSurface.setTriggerViewMode();
			triggerMode = true;
			reassignSurfaceView(triggerMode);
			return true;
		case R.id.expandX:
			mAndroidSurface.growXdimension();
			return true;
		case R.id.shrinkX:
			mAndroidSurface.shrinkXdimension();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService has been moved to OpenGLThread
		IntentFilter intentFilter = new IntentFilter(
				"BYBUpdateMillisecondsReciever");
		upmillirec = new UpdateMillisecondsReciever();
		registerReceiver(upmillirec, intentFilter);

		IntentFilter intentFilterVolts = new IntentFilter(
				"BYBUpdateMillivoltReciever");
		upmillivolt = new UpdateMillivoltReciever();
		registerReceiver(upmillivolt, intentFilterVolts);

		IntentFilter intentFilterVoltSize = new IntentFilter(
				"BYBMillivoltsViewSize");
		milliVoltSize = new SetMillivoltViewSizeReceiver();
		registerReceiver(milliVoltSize, intentFilterVoltSize);

		IntentFilter intentFilterRecordingButtons = new IntentFilter(
				"BYBShowRecordingButtons");
		showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
		registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);

		application.startAudioService();
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service has been moved to OpenGLThread
		application.stopAudioService();
		unregisterReceiver(upmillirec);
		unregisterReceiver(upmillivolt);
		unregisterReceiver(milliVoltSize);
		unregisterReceiver(showRecordingButtonsReceiver);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mAndroidSurface.onTouchEvent(event);
		return super.onTouchEvent(event);
	}
	
	private void setUpRecordingButtons() {
		recordingBackground = findViewById(R.id.recordButtonBackground);
		
		mRecordButton = (ImageButton) findViewById(R.id.recordButton);
		OnClickListener toggleRecListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecording();
			}
		};
		mRecordButton.setOnClickListener(toggleRecListener);

		mFileButton = (ImageButton) findViewById(R.id.fileButton);
		mFileButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent i = new Intent(view.getContext(), FileListActivity.class);
				startActivityForResult(i, 0);
			}

		});
		tapToStopRecView = findViewById(R.id.TapToStopRecordingTextView);
		tapToStopRecView.setOnClickListener(toggleRecListener);
	}
	
	private void showRecordingButtons() {
		if(mFileButton != null) {
			mFileButton.setVisibility(View.VISIBLE);
		}
		if(mRecordButton != null) {
			mRecordButton.setVisibility(View.VISIBLE);
		}
		if(recordingBackground != null) {
			recordingBackground.setVisibility(View.VISIBLE);
		}
	}

	private void hideRecordingButtons() {
		if(mFileButton != null) {
			mFileButton.setVisibility(View.GONE);
		}
		if(mRecordButton != null) {
			mRecordButton.setVisibility(View.GONE);
		}
		if(recordingBackground != null) {
			recordingBackground.setVisibility(View.GONE);
		}
	}

	void reassignSurfaceView(boolean isTriggerView) {
		BybConfigHolder bch = null; 
		if(mAndroidSurface != null) bch = collectConfigFromSurface();
		mAndroidSurface = null;
		mainscreenGLLayout.removeAllViews();
		if (bch != null) {
			mAndroidSurface = new OscilloscopeGLSurfaceView(this, bch, isTriggerView);
		} else {
			mAndroidSurface = new OscilloscopeGLSurfaceView(this, isTriggerView);
		}
		mainscreenGLLayout.addView(mAndroidSurface);
		if (isTriggerView) {
			hideRecordingButtons();
		} else {
			showRecordingButtons();
		}
		Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
	}

	protected void toggleRecording() {

		try {
			ShowRecordingAnimation anim = new ShowRecordingAnimation(this,
					isRecording);
			anim.run();
			Intent i = new Intent();
			i.setAction("BYBToggleRecording");
			getBaseContext().sendBroadcast(i);
			if (isRecording == false) {
				tapToStopRecView.setVisibility(View.VISIBLE);
			} else {
				tapToStopRecView.setVisibility(View.GONE);
			}
			isRecording = !isRecording;
		} catch (RuntimeException e) {
			Toast.makeText(getApplicationContext(),
					"No SD Card is available. Recording is disabled",
					Toast.LENGTH_LONG).show();

		}
	}

	public void setDisplayedMilliseconds(Float ms) {
		msView.setText(ms.toString());
	}
	
	private class ShowRecordingAnimation implements Runnable {

		private Activity activity;
		private boolean recording;

		public ShowRecordingAnimation(Activity a, Boolean b) {
			this.activity = a;
			this.recording = b;
		}

		@Override
		public void run() {
			Animation a = null;
			if (this.recording == false) {
				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, -1,
						Animation.RELATIVE_TO_SELF, 0);
			} else {
				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, -1);
			}
			a.setDuration(250);
			a.setInterpolator(AnimationUtils.loadInterpolator(this.activity,
					android.R.anim.anticipate_overshoot_interpolator));
			View stopRecView = findViewById(R.id.TapToStopRecordingTextView);
			stopRecView.startAnimation(a);
		}

	}

	private class UpdateMillisecondsReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
		};
	}

	private class UpdateMillivoltReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
		};
	}

	private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
		};
	}

	private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			boolean yesno = intent.getBooleanExtra("showRecordingButton", true);
			if (yesno) {
				showRecordingButtons();
			} else {
				hideRecordingButtons();
			}
		}	
		};
}