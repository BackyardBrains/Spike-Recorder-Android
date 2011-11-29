package com.backyardbrains.audio;

import java.nio.ByteBuffer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.BackyardBrainsApplication;
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
	private int mBindingsCount;

	private MicListener micThread;
	private RingBuffer audioBuffer;
	private RecordingSaver mRecordingSaverInstance;

	private NotificationManager mNM;

	/**
	 * return a byte array with in the appropriate order representing the last
	 * 1.5 seconds of audio or so
	 * 
	 * @return a ordinate-corrected version of the audio buffer
	 */
	public byte[] getAudioBuffer() {
		return audioBuffer.getArray();
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

		IntentFilter intentFilter = new IntentFilter("BYBToggleRecording");
		toggleRecorder = new ToggleRecordingListener();
		registerReceiver(toggleRecorder, intentFilter);

		audioBuffer = new RingBuffer(131072);
		audioBuffer.zeroFill();

		turnOnMicThread();
	}

	/**
	 * unregister our recording listener, then kill {@link MicListener}
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		unregisterReceiver(toggleRecorder);
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
		mBindingsCount++;
		Log.d(TAG, "Bound to service: " + mBindingsCount + " instances");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mBindingsCount--;
		Log.d(TAG, "Bound to service: " + mBindingsCount + " instances");
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
		showNotification();
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
		if (mNM != null) {
			mNM.cancel(NOTIFICATION);
		}
	}

	/**
	 * Put up a notification that this service is running.
	 * 
	 * @see android.app.Notification
	 * @see NotificationManager#notify()
	 */
	private void showNotification() {
		CharSequence text = getText(R.string.mic_thread_running);
		Notification not = new Notification(R.drawable.ic_launcher_byb, text,
				System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, BackyardAndroidActivity.class), 0);
		not.setLatestEventInfo(this, "Backyard Brains", text, contentIntent);
		mNM.notify(NOTIFICATION, not);
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

}
