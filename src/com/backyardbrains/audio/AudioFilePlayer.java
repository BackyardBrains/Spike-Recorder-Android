package com.backyardbrains.audio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class AudioFilePlayer implements PlaybackListener{
	private PlaybackThread 	playbackThread;
	private RecordingReader reader = null;
	private ReceivesAudio audioReceiver;
	private Context context = null;
	private AudioFileReadListener audioFileReadListener;
	private boolean bFileLoaded = false;
	private boolean bPlaying = false;
	
	public AudioFilePlayer(ReceivesAudio audioReceiver, Context context){
		this.context = context;
		this.audioReceiver = audioReceiver;
		bFileLoaded = false;
		bPlaying = false;
	}
	public void load(String filePath){
		load(new File(filePath));
	}
	public void load(File f){
		if(context != null){
		reader = null;
		registerAudioFileReadReceiver(true);
		reader =  new RecordingReader(f, context);
		bFileLoaded = false;
		}
	}
	public void play(){
		if(bFileLoaded){
			turnOnPlaybackThread();
		}
		bPlaying = true;
	}
	public void stop(){
		turnOffPlaybackThread();
	}
	private void turnOnPlaybackThread(){
		if(reader != null && audioReceiver != null && bFileLoaded){
		playbackThread = null;
		playbackThread = new PlaybackThread(getDataShort(), this, audioReceiver);
		playbackThread.startPlayback();
		bPlaying = true;
		}		
	}
	private void turnOffPlaybackThread(){
		bPlaying = false;
		if(playbackThread != null){
			playbackThread.stopPlayback();
			playbackThread = null;
			
		//	Log.d(TAG, "Playback Thread Shut Off");
		}
	}
	public boolean isPlaying(){return bPlaying;}
	private short [] getDataShort(){
		short[] samples;
		if(reader != null){
	        byte[] data = reader.getData();
	        ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
	        samples = new short[sb.limit()];
	        sb.get(samples);
		}else{
			samples = new short[0];
		}
	        return samples;
	}
    public void onProgress(int progress){}
    public void onCompletion(){}
    
 // -----------------------------------------------------------------------------------------------------------------------------
 // ----------------------------------------- BROADCAST RECEIVERS CLASS
 // -----------------------------------------------------------------------------------------------------------------------------
 	private class AudioFileReadListener extends BroadcastReceiver {
 		@Override
 		public void onReceive(android.content.Context context, android.content.Intent intent) {
 			
 			bFileLoaded = true;
 			if(bPlaying){
 				play();
 			}
 			registerAudioFileReadReceiver(false);
 		};
 	}


 // -----------------------------------------------------------------------------------------------------------------------------
 // ----------------------------------------- BROADCAST RECEIVERS TOGGLES
 // -----------------------------------------------------------------------------------------------------------------------------
 	/**
 	 * Toggle our receiver. If true, register a receiver for intents with the
 	 * action "BYBToggleRecording", otherwise, unregister the same receiver.
 	 * 
 	 * @param reg
 	 *            register if true, unregister otherwise.
 	 */
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
