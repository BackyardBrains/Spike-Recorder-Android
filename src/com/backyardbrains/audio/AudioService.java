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

	/**
	 * Tag for logging
	 */
	static final String TAG = "BYBAudioService";

	/**
	 * Indicator of whether the service is properly running
	 */
	public boolean running;

	/**
	 * Provides a reference to {@link AudioService} to all bound clients.
	 * 
	 */
	public class AudioServiceBinder extends Binder {
		public AudioService getService() {
			return AudioService.this;
		}
	}

	private final IBinder mBinder = new AudioServiceBinder();

	/**
	 * {@link MicListener} the service uses to listen to default audio device
	 * 
	 */
	private MicListener micThread;

	/**
	 * Unique id to turn on-and-off service notification
	 */
	private int NOTIFICATION = R.string.mic_thread_running;
	private NotificationManager mNM;

	private ByteBuffer currentAudioInfo;

	private RingBuffer audioBuffer;
	
	private int mBindingsCount;

	private RecordingSaver mRecordingSaverInstance;

	private ToggleRecordingListener toggleRecorder;

	/**
	 * @return the currentAudioInfo
	 */
	public ByteBuffer getCurrentAudioInfo() {
		return currentAudioInfo;
	}

	public Byte[] getAudioBuffer() {
		return audioBuffer.getArray();
	}
	
	/**
	 * Create service and grab reference to {@link BackyardBrainsApplication}
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
		
		turnOnMicThread();
	}

	/**
	 * Tell application we're no longer running, then kill {@link MicListener}
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
	 * Get a copy of the necessary activity, and push the received data out to
	 * said activity
	 * 
	 * @see com.backyardbrains.audio.RecievesAudio#receiveAudio(byte[])
	 */
	@Override
	public void receiveAudio(ByteBuffer audioInfo) {
		this.currentAudioInfo = audioInfo;
		audioBuffer.add(audioInfo);
		if (mRecordingSaverInstance != null) {
			mRecordingSaverInstance.receiveAudio(audioInfo);
		}

	}

	private class ToggleRecordingListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			if (!startRecording()) {
				stopRecording();
			}
		};
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

	/**
	 * return a binding pointer for activities to reference this object
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

	/**
	 * make sure we clean up. shuts down {@link MicListener} thread and assures
	 * someone has told us to stop (in this case, ourselves)
	 * 
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		// turnOffMicThread();
		mBindingsCount--;
		Log.d(TAG, "Bound to service: " + mBindingsCount + " instances");
		if (mBindingsCount < 1) {
			// stopSelf();
		}
		return super.onUnbind(intent);
	}

}
