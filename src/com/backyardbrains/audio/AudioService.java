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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;


import android.app.NotificationManager;

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



/**
 * Manages a thread which monitors default audio input and pushes raw audio data
 * to bound activities.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */

public class AudioService extends Service implements ReceivesAudio {
	static final String	TAG				= AudioService.class.getCanonicalName();
	
	public boolean		running;
	
	public static final int LIVE_MODE = 0;
	public static final int PLAYBACK_MODE = 1;
	private int mode = LIVE_MODE;
	


	private final IBinder				mBinder		= new AudioServiceBinder();

	private MicListener					micThread;
	private AudioFilePlayer				audioPlayer	= null;
	private RingBuffer					audioBuffer;
	private RecordingSaver				mRecordingSaverInstance;

	private ToggleRecordingListener		toggleRecorder;
	private PlayAudioFileListener		playListener;
	private CloseButtonListener 		closeListener;
	private OnTabSelectedListener 		tabSelectedListener;
	private AveragesNumListener 		averagesNumListener;
	private TogglePlaybackListener 		togglePlaybackListener;
	
	private long						lastSamplesReceivedTimestamp;
	private int						micListenerBufferSizeInSamples;
	private Context					appContext		= null;

	private TriggerAverager					averager;
	

	
	private boolean bUseAverager = false;
	
	
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- GETTERS SETTERS
// -----------------------------------------------------------------------------------------------------------------------------
	/**
	 * Provides a reference to {@link AudioService} to all bound clients.
	 */
	public class AudioServiceBinder extends Binder {
		public AudioService getService() {
			return AudioService.this;
		}
	}
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
	public int getMode(){
		return mode;
	}
	public boolean isAudioPlayerPlaying(){
		if(audioPlayer != null){
			return audioPlayer.isPlaying();
		}
		return false;
	}
	/**
	 * return a byte array with in the appropriate order representing the last
	 * 1.5 seconds of audio or so
	 * 
	 * @return a ordinate-corrected version of the audio buffer
	 */
	public short[] getAudioBuffer() {
		boolean bReturnAudioPlayerBuffer = false;
		if(mode == PLAYBACK_MODE && audioPlayer != null){
			if(!audioPlayer.isPlaying()){
				bReturnAudioPlayerBuffer = true;
			}
		}
		if(bReturnAudioPlayerBuffer){
			return audioPlayer.getBuffer();
		}else{
			return audioBuffer.getArray();
		}
	}
	public short[] getAverageBuffer() {
		if(averager != null){
			return averager.getAveragedSamples();
		}else{
			return new short[0];
		}
	}
	public void setMicListenerBufferSizeInSamples(int i) {
		micListenerBufferSizeInSamples = i;
	}
	public Handler getTriggerHandler(){
		if(averager != null){
			return averager.getHandler();
		}else{
			return null;
		}
	}
	public void setUseAverager(boolean bUse){
		bUseAverager = bUse;
	}
	
	

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- LIFECYCLE OVERRIDES
// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public void onCreate() {
		super.onCreate();
		appContext = this.getApplicationContext();
		registerRecordingToggleReceiver(true);

		audioBuffer = new RingBuffer(131072);
		audioBuffer.zeroFill();

		averager = new TriggerAverager(TriggerAverager.defaultSize, appContext);
		
		registerPlayAudioFileReceiver(true);
		registerOnTabSelectedReceiver(true);
		registerCloseButtonReceiver(true);
		registerPlaybackToggleReceiver(true);
		registerAveragerSetMaxReceiver(true);
		turnOnMicThread();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, Service.START_STICKY, startId);
	}
	@Override
	public void onDestroy() {
		registerRecordingToggleReceiver(false);
		registerPlayAudioFileReceiver(false);
		registerOnTabSelectedReceiver(false);
		registerCloseButtonReceiver(false);
		registerAveragerSetMaxReceiver(false);
		registerPlaybackToggleReceiver(false);
		turnOffMicThread();
		turnOffAudioPlayerThread();
		averager = null;
		super.onDestroy();
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BIND
// -----------------------------------------------------------------------------------------------------------------------------

	/**
	 * return a binding pointer for GL threads to reference this object
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 * @return binding reference to this object
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// mBindingsCount++;
		// Log.d(TAG, "Bound to service: " + mBindingsCount + " instances");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// mBindingsCount--;
		// Log.d(TAG, "Unbound from service: " + mBindingsCount + " instances");
		return super.onUnbind(intent);
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- START/STOP THREADS
// -----------------------------------------------------------------------------------------------------------------------------
	protected void togglePlayback(boolean bPlay){
		if(audioPlayer != null){
			if(bPlay){
				audioPlayer.play();
			}else{
				audioPlayer.pause();
			}
		}
	}
	protected void turnOnAudioPlayerThread() {
		
		if (audioPlayer != null) {
			turnOffMicThread();
			audioPlayer.play();
		}
		mode = PLAYBACK_MODE;
	}

	protected void turnOffAudioPlayerThread() {
		if (audioPlayer != null) {
			audioPlayer.stop();
			audioPlayer = null;
		}
		Log.d(TAG, "Turn Off Audio Player");
	}

	/**
	 * Instantiate {@link MicListener} thread, tell it to start, and put up the
	 * notification via {@link AudioService#showNotification()}
	 */
	protected void turnOnMicThread() {
		
		turnOffAudioPlayerThread();
		if (micThread == null) {
			micThread = null;
			micThread = new MicListener();
			micThread.start(AudioService.this);
			Log.d(TAG, "Mic thread started");
		}
		mode = LIVE_MODE;
	}

	/**
	 * Clean up {@link MicListener} resources and remove notification
	 */
	protected void turnOffMicThread() {
		stopRecording();
		if (micThread != null) {
			micThread.requestStop();
			micThread = null;
			Log.d(TAG, "Mic Thread Shut Off");
		}
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- ReceivesAudio OVERRIDES
// -----------------------------------------------------------------------------------------------------------------------------

	/**
	 * On receiving audio, add it to the RingBuffer. If we're recording, also
	 * dispatch it to the RecordingSaver instance.
	 * 
	 * @see com.backyardbrains.audio.RecievesAudio#receiveAudio(byte[])
	 */
	@Override
	public void receiveAudio(ByteBuffer audioInfo) {
		if(!bUseAverager){
		audioBuffer.add(audioInfo);
		}else{
		averager.push(audioInfo);
		}
		lastSamplesReceivedTimestamp = System.currentTimeMillis();
		if (mRecordingSaverInstance != null) {
			recordAudio(audioInfo);
		}
	}

	@Override
	public void receiveAudio(ShortBuffer audioInfo) {
		if(!bUseAverager){
		audioBuffer.add(audioInfo);
		}else{
		averager.push(audioInfo);
		}
		lastSamplesReceivedTimestamp = System.currentTimeMillis();
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- RECORD AUDIO
// -----------------------------------------------------------------------------------------------------------------------------

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
			Log.w(getClass().getCanonicalName(), "Ignoring bytes received while not synced: " + e.getMessage());
		}
	}

	public boolean startRecording() {
		if (mRecordingSaverInstance != null) {
			return false;
		}
		try {
			mRecordingSaverInstance = new RecordingSaver("BYB", this.getApplicationContext());// theTime.toString());
		} catch (IllegalStateException e) {
			Toast.makeText(getApplicationContext(), "No SD Card is available. Recording is disabled", Toast.LENGTH_LONG).show();
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

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// -----------------------------------------------------------------------------------------------------------------------------
	private class ToggleRecordingListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			if (!startRecording()) {
				if (!stopRecording()) {
					Log.w(TAG, "There was an error recording properly");
				}
			}
		};
	}
	
	private class TogglePlaybackListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			if(intent.hasExtra("play")){
				togglePlayback(intent.getBooleanExtra("play", true));
			}
		};
	}

	private class PlayAudioFileListener extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("PlayAudioFileListener", "onReceive");
			if (appContext != null) {
				if (intent.hasExtra("filePath")) {
					String path = intent.getStringExtra("filePath");
					if (!path.isEmpty()) {
						turnOffAudioPlayerThread();
						audioPlayer = new AudioFilePlayer((ReceivesAudio) AudioService.this, appContext);
						audioPlayer.load(path);
						stopRecording();
						turnOnAudioPlayerThread();
						
						Intent i = new Intent();
						i.setAction("BYBAudioPlaybackStart");
						appContext.sendBroadcast(i);
						
						Log.d("PlayAudioFileListener","BroadcastReceiver ");
						
					}else{Log.d("PlayAudioFileListener", "onReceive:: filePath is empty!");}
				}else{Log.d("PlayAudioFileListener", "onReceive:: there's no extra in intent!");}
			}else{Log.d("PlayAudioFileListener", "onReceive::appContext == null");}
			
		}
	}
	private class CloseButtonListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//turnOffAudioPlayerThread();
			turnOnMicThread();
		}
	}
	private class OnTabSelectedListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.hasExtra("tab")){
				int currentTab = intent.getIntExtra("tab", 0); 
				if(currentTab < 2 && audioPlayer == null){
					turnOnMicThread();
				}
			}
		}
	}
	private class AveragesNumListener extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
		if(intent.hasExtra("num")){
			averager.setMaxsize(intent.getIntExtra("num", TriggerAverager.defaultSize));//, false);
		}
		}
	}
	
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
// -----------------------------------------------------------------------------------------------------------------------------
	private void registerCloseButtonReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBCloseButton");
			closeListener = new  CloseButtonListener();
			appContext.registerReceiver(closeListener, intentFilter);
		} else {
			appContext.unregisterReceiver(closeListener);
		}
	}
	private void registerPlaybackToggleReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBTogglePlayback");
			togglePlaybackListener = new TogglePlaybackListener();
			appContext.registerReceiver(togglePlaybackListener, intentFilter);
		} else {
			appContext.unregisterReceiver(togglePlaybackListener);
		}
	}
	
	private void registerRecordingToggleReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBToggleRecording");
			toggleRecorder = new ToggleRecordingListener();
			appContext.registerReceiver(toggleRecorder, intentFilter);
		} else {
			appContext.unregisterReceiver(toggleRecorder);
		}
	}
	private void registerOnTabSelectedReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBonTabSelected");
			tabSelectedListener = new OnTabSelectedListener();
			appContext.registerReceiver(tabSelectedListener , intentFilter);
		} else {
			appContext.unregisterReceiver(tabSelectedListener);
		}
	}
	private void registerPlayAudioFileReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBPlayAudioFile");
			playListener = new PlayAudioFileListener();
			appContext.registerReceiver(playListener, intentFilter);
		} else {
			appContext.unregisterReceiver(playListener);
		}
	}
	private void registerAveragerSetMaxReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBThresholdNumAverages");
			averagesNumListener = new AveragesNumListener();
			appContext.registerReceiver(averagesNumListener, intentFilter);
		} else {
			appContext.unregisterReceiver(playListener);
		}
	}
}
