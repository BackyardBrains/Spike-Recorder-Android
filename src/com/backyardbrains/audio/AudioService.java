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

package com.backyardbrains.audio;

import java.nio.ByteBuffer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.R;

/**
 * Manages a thread which monitors default audio input and pushes raw audio data
 * to bound activities.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */



public class AudioService extends Service implements ReceivesAudio {
	static final String TAG = AudioService.class.getCanonicalName();
	private int NOTIFICATION = R.string.mic_thread_running;
	public boolean running;

	/**
	 * Provides a reference to {@link AudioService} to all bound clients.
	 */
public class AudioServiceBinder extends Binder {
		public AudioService getService() {
			return AudioService.this;
		}
	}

	private ToggleRecordingListener toggleRecorder;

	private final IBinder mBinder = new AudioServiceBinder();
	//private int mBindingsCount;

	private MicListener micThread;
	private RingBuffer audioBuffer;
	private RecordingSaver mRecordingSaverInstance;

	private NotificationManager mNM;
	private TriggerAverager triggerAverager;
	private boolean triggerMode;
	private ToggleTriggerListener toggleTrigger;
	private SetSampleSizeListener sampleSizeListener;
	private long lastSamplesReceivedTimestamp;
	private int micListenerBufferSizeInSamples;

	/**
	 * @return the micListenerBufferSizeInSamples
	 */
	public int getMicListenerBufferSizeInSamples() {
		return micListenerBufferSizeInSamples;
	}

	/**
	 * @return the lastSamplesReceivedTimestamp
	 */
	public long getLastSamplesReceivedTimestamp() {
		return lastSamplesReceivedTimestamp;
	}

	/**
	 * return a byte array with in the appropriate order representing the last
	 * 1.5 seconds of audio or so
	 * 
	 * @return a ordinate-corrected version of the audio buffer
	 */
	public short[] getAudioBuffer() {
		return audioBuffer.getArray();
	}
	
	public short[] getTriggerBuffer() {
		return triggerAverager.getAveragedSamples();
	}

	public void setMicListenerBufferSizeInSamples(int i) {
		micListenerBufferSizeInSamples = i;
	}

	/**
	 * Register a receiver for toggling recording funcionality, then instantiate
	 * our ringbuffer and turn on mic thread
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		registerRecordingToggleReceiver(true);

		audioBuffer = new RingBuffer(131072);
		audioBuffer.zeroFill();
		
		registerTriggerToggleReceiver(true);
		triggerAverager = new TriggerAverager(50);
		triggerMode = false;

		registerSetSampleSizeReceiver(true);
		
		turnOnMicThread();
	}
	
	/**
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, Service.START_STICKY, startId);
	}

	/**
	 * unregister our recording listener, then kill {@link MicListener}
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		registerRecordingToggleReceiver(false);
		registerTriggerToggleReceiver(false);
		registerSetSampleSizeReceiver(false);
		turnOffMicThread();
		super.onDestroy();
	}

	/**
	 * return a binding pointer for GL threads to reference this object
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 * @return binding reference to this object
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		//mBindingsCount++;
		//Log.d(TAG, "Bound to service: " + mBindingsCount + " instances");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		//mBindingsCount--;
		//Log.d(TAG, "Unbound from service: " + mBindingsCount + " instances");
		return super.onUnbind(intent);
	}

	/**
	 * Instantiate {@link MicListener} thread, tell it to start, and put up the
	 * notification via {@link AudioService#showNotification()}
	 */
	public void turnOnMicThread() {
		micThread = null;
		micThread = new MicListener();
		micThread.start(AudioService.this);
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification(true);
		Log.d(TAG, "Mic thread started");
	}

	/**
	 * Clean up {@link MicListener} resources and remove notification
	 */
	public void turnOffMicThread() {
		if (micThread != null) {
			micThread.requestStop();
			micThread = null;
			Log.d(TAG, "Mic Thread Shut Off");
		}
		showNotification(false);
	}

	/**
	 * Toggle our receiver. If true, register a receiver for intents with the
	 * action "BYBToggleRecording", otherwise, unregister the same receiver.
	 * 
	 * @param reg register if true, unregister otherwise.
	 */
	private void registerRecordingToggleReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBToggleRecording");
			toggleRecorder = new ToggleRecordingListener();
			registerReceiver(toggleRecorder, intentFilter);
		} else {
			unregisterReceiver(toggleRecorder);
		}
	}

	private void registerTriggerToggleReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBToggleTrigger");
			toggleTrigger = new ToggleTriggerListener();
			registerReceiver(toggleTrigger, intentFilter);
		} else {
			unregisterReceiver(toggleTrigger);
		}
	}

	private void registerSetSampleSizeReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("setSampleSize");
			sampleSizeListener = new SetSampleSizeListener();
			registerReceiver(sampleSizeListener, intentFilter);
		} else {
			unregisterReceiver(sampleSizeListener);
		}
	}
	/**
	 * Toggle a notification that this service is running.
	 * 
	 * @param show show if true, hide otherwise.
	 * @see android.app.Notification
	 * @see NotificationManager#notify()
	 */
	private void showNotification(boolean show) {
		if (show) {
			CharSequence text = getText(R.string.mic_thread_running);
			Notification not = new Notification(R.drawable.ic_launcher_byb,
					text, System.currentTimeMillis());
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, BackyardAndroidActivity.class), 0);
			not.setLatestEventInfo(this, "Backyard Brains", text, contentIntent);
			mNM.notify(NOTIFICATION, not);
		} else {
			if (mNM != null) {
				mNM.cancel(NOTIFICATION);
			}
		}
	}

	/**
	 * On receiving audio, add it to the RingBuffer. If we're recording, also
	 * dispatch it to the RecordingSaver instance.
	 * 
	 * @see com.backyardbrains.audio.RecievesAudio#receiveAudio(byte[])
	 */
	@Override
	public void receiveAudio(ByteBuffer audioInfo) {
		audioBuffer.add(audioInfo);
		lastSamplesReceivedTimestamp = System.currentTimeMillis();
		if(triggerMode) {
//			Log.d(TAG, "Pushing audio to triggerAverager, length: "+audioInfo.capacity());
			triggerAverager.push(audioInfo);
		}
		if (mRecordingSaverInstance != null) {
			recordAudio(audioInfo);
		}

	}

	/**
	 * dispatch audio to the active RecordingSaver instance
	 * 
	 * @param audioInfo
	 */
	private void recordAudio(ByteBuffer audioInfo) {
		audioInfo.clear();
		try {
			mRecordingSaverInstance.receiveAudio(audioInfo);
		} catch (IllegalStateException e) {
			Log.w(getClass().getCanonicalName(),
					"Ignoring bytes received while not synced: "
							+ e.getMessage());
		}
	}

	public boolean startRecording() {
		if (mRecordingSaverInstance != null) {
			return false;
		}
		Long theTime = (Long) System.currentTimeMillis();
		try {
			mRecordingSaverInstance = new RecordingSaver(theTime.toString());
		} catch (IllegalStateException e) {
			Toast.makeText(getApplicationContext(),
					"No SD Card is available. Recording is disabled",
					Toast.LENGTH_LONG).show();
		}
		return true;
	}

	public boolean stopRecording() {
		if (mRecordingSaverInstance != null) {
			mRecordingSaverInstance.finishRecording();
			mRecordingSaverInstance = null;
			return true;
		}
		return false;
	}
	
	public Handler getTriggerHandler() {
		return triggerAverager.getHandler();
	}

	private class ToggleRecordingListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			if (!startRecording()) {
				if (!stopRecording()) {
					Log.w(TAG, "There was an error recording properly");
				}
			}
		};
	}

	private class ToggleTriggerListener extends BroadcastReceiver {

		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			triggerMode = intent.getBooleanExtra("triggerMode", false);
			Log.d(TAG, "Switched triggerMode to "+triggerMode);
		};
		
	}
	
	private class SetSampleSizeListener extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			triggerAverager.setMaxsize(intent.getIntExtra("newSampleSize", 1));
			Log.d(TAG, "Set triggeraverager sample size to "+triggerAverager.getMaxsize()); 
		}
		
	}

}
