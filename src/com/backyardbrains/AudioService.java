package com.backyardbrains;

import java.nio.ByteBuffer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AudioService extends Service implements RecievesAudio {

	static final String TAG = "BYBAudioService";
	public boolean running;
	private MicListener mic;

	private final IBinder mBinder = new AudioServiceBinder();

	public class AudioServiceBinder extends Binder {
		AudioService getService() {
			return AudioService.this;
		}
	}

	private BackyardBrainsApplication app;
	private Thread micThread;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		this.mic = new MicListener();
		this.app = (BackyardBrainsApplication) getApplication();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		this.running = false;
		this.app.setServiceRunning(this.running);
		this.mic.requestStop();

		this.mic = null;
		Log.d(TAG, "Update thread cleaned up");
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		running = true;
		app.setServiceRunning(this.running);
		micThread = new Thread() {
			public void run() {
				mic.start(AudioService.this);
				Log.d(TAG, "Mic thread started");
			}
		};
		micThread.start();
		return START_STICKY;
	}

	public ByteBuffer getAudioFromMicListener() {
		return mic.getAudioInfo();
	}
	
	@Override
	public void receiveAudio(ByteBuffer audioData) {
		Log.i(TAG, "Got audio data");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

}
