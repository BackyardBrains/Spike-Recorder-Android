package com.backyardbrains.audio;

import java.nio.ByteBuffer;

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
	 * Reference to instantiating {@link BackyardBrainsApplication}
	 */
	private BackyardBrainsApplication app;

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

	/**
	 * Create service and grab reference to {@link BackyardBrainsApplication}
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		app = (BackyardBrainsApplication) getApplication();
	}

	/**
	 * Tell application we're no longer running, then kill {@link MicListener}
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		app.setServiceRunning(false);
		turnOffMicThread();
		super.onDestroy();
	}

	/**
	 * Do standard {@link Service#onStartCommand(Intent, int, int)}, then turn
	 * on {@link MicListener} and let app know we're running.
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		turnOnMicThread();
		app.setServiceRunning(true);
		return START_STICKY;
	}

	/**
	 * Instantiate {@link MicListener} thread, tell it to start, and put up the
	 * notification via {@link AudioService#showNotification()}
	 */
	public void turnOnMicThread() {
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
	 * Called by bound activities (for now, just {@link BackyardAndroidActivity}
	 * ) to get audio data to hand off to a drawing surface
	 * 
	 * @return {@link ByteBuffer} of audio data from {@link MicListener}
	 * @deprecated
	 */
	@Deprecated
	public ByteBuffer getAudioFromMicListener() {
		return micThread.getAudioInfo();
	}

	/**
	 * Get a copy of the necessary activity, and push the received data out to
	 * said activity
	 * 
	 * @see com.backyardbrains.audio.RecievesAudio#receiveAudio(java.nio.ByteBuffer)
	 */
	@Override
	public void receiveAudio(ByteBuffer audioData) {
		BackyardAndroidActivity l_activity = app.getRunningActivity();
		if (l_activity != null && audioData != null) {
			l_activity.setCurrentAudio(audioData);
		} else {
			Log.e(TAG, "Prevented NPE while trying to push audio to GL thread.");
		}
	}

	/**
	 * return a binding pointer for activities to reference this object
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 * @return binding reference to this object
	 */
	@Override
	public IBinder onBind(Intent arg0) {
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
		turnOffMicThread();
		stopSelf();
		return super.onUnbind(intent);
	}

}
