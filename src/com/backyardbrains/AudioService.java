package com.backyardbrains;

import java.nio.ByteBuffer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioService extends Service {
	static final String TAG = "BYBAudioService";
	
	public boolean running;

	private MicListener mic;

	private BackyardBrainsApplication app;

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		this.mic = new MicListener(this);
		this.app = (BackyardBrainsApplication) getApplication();
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		this.running = false;
		this.app.setServiceRunning(this.running);
		this.mic.interrupt();

		this.mic= null;
		Log.d(TAG, "Update thread cleaned up");
		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.running = true;
		this.app.setServiceRunning(this.running);
		this.mic.start();
		Log.d(TAG, "Mic thread started");
		return START_STICKY;
	}

	public void receivedAudioData(ByteBuffer audioData) {
		String msg = "";
		Byte i;
		while (audioData.hasRemaining()) {
			i = audioData.get();
			msg = msg + i.toString();
			}
		Log.i(TAG, "Got audio data: " + msg);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
