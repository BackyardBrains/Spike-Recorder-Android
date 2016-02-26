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

public class AudioFilePlayer implements PlaybackListener, RecordingReader.AudiofileReadListener{
	
	private static final String TAG = "AudioFilePlayer";
	
	private PlaybackThread			playbackThread;
	private RecordingReader			reader		= null;
	private ReceivesAudio			audioReceiver;
	private Context					context		= null;
	private boolean					bFileLoaded	= false;
	private boolean					bPlaying	= false;
	private boolean 				bShouldPlay = false;
	private boolean					bLooping = false;
	
	
	
	public AudioFilePlayer(ReceivesAudio audioReceiver, Context context) {
		this.context = context.getApplicationContext();
		this.audioReceiver = audioReceiver;
		bFileLoaded = false;
		bPlaying = false;
		bShouldPlay = false;
		bLooping = false;
	}

	public void audioFileRead(){
		Log.d(TAG, "AudioFileRead");
		bFileLoaded = true;
		if (bShouldPlay) {
			play();
			bShouldPlay = false;
		}
	}
	public void load(String filePath) {
		load(new File(filePath));
	}

	public void load(File f) {
		if (context != null) {
			if (f.exists()) {
				reader = null;
				reader = new RecordingReader(f, (RecordingReader.AudiofileReadListener)this);
				bFileLoaded = false;
				bShouldPlay = false;
			}else{
				Log.d("AudioFilePlayer","Cant load file: it doent exist!!");
			}
		}
	}

	public void play() {
		if (bFileLoaded) {
			turnOnPlaybackThread();
		}else{
			bShouldPlay = true;
		}
	}

	public void stop() {
		turnOffPlaybackThread();
		if (reader != null) {
			reader.close();
			reader = null;
		}
	}

	private void turnOnPlaybackThread() {
		Log.d("AudioFilePlayer","turnOnPlaybackThread");
		if (reader != null && audioReceiver != null && bFileLoaded) {
			playbackThread = null;
			playbackThread = new PlaybackThread(reader.getDataShorts(), this, audioReceiver);
			playbackThread.startPlayback();
			bPlaying = true;
		}else{
			String m = "TurnOnPlaybackThread failed! ";
			if(reader == null ){ m += "Reader is Null ";}
			if(audioReceiver == null ){m += "audioReceiver is null";}
			if(!bFileLoaded) {m += "file not loaded";}
			Log.d("AudioFilePlayer", m);
		}
	}

	private void turnOffPlaybackThread() {
		bPlaying = false;
		if (playbackThread != null) {
			playbackThread.stopPlayback();
			playbackThread = null;
			 Log.d("AudioFilePlayer", "Playback Thread Shut Off");
		}	
	}

	public boolean isPlaying() {
		return bPlaying;
	}
	public boolean isLooping(){
		return bLooping;
	}
	public void enableLooping(boolean loop){
		bLooping = loop;
	}

	public void onProgress(int progress) {
	}

	public void onCompletion() {
		if(bLooping){
			turnOnPlaybackThread();
		}else{
			stop();
			Intent i = new Intent();
			i.setAction("BYBAudioFilePlaybackEnded");
			context.sendBroadcast(i);
		}
	}
}
