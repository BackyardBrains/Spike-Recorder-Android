
package com.backyardbrains.audio;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class PlaybackThread {
	static final int			SAMPLE_RATE	= 44100;
	private static final String	LOG_TAG		= PlaybackThread.class.getSimpleName();

	public PlaybackThread(short[] samples, PlaybackListener listener, ReceivesAudio service) {
		mSamples = ShortBuffer.wrap(samples);
		mNumSamples = samples.length;
		mListener = listener;
		mService = service;
	}

	private Thread				mThread;
	private boolean				mShouldContinue;
	private ShortBuffer			mSamples;
	private int					mNumSamples;
	private PlaybackListener	mListener;
	private ReceivesAudio		mService;
	private boolean				bPlaying	= false;

	public boolean isPlaying() {
		return bPlaying;
		// return mThread != null;
	}

	public void startPlayback() {
		if (mThread != null){
			bPlaying  = true;
		}else{

		// Start streaming in a thread
		mShouldContinue = true;
		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				play();
			}
		});
		mThread.start();
		}
		onPlaybackStateChange();
	}

	public void stopPlayback() {
		if (mThread == null) return;

		mShouldContinue = false;
		bPlaying = false;
		mThread = null;
		onPlaybackStateChange();
	}

	public void pausePlayback() {
		if (mThread == null) return;
		bPlaying = false;
		onPlaybackStateChange();
	}
	private void onPlaybackStateChange(){
		if(mListener!=null){
			mListener.onPlaybackStateChange();
		}
	}
	public void rewind(){
		mSamples.rewind();
	}
	private void play() {
		int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
			bufferSize = SAMPLE_RATE * 2;
		}

		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

		audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
			@Override
			public void onPeriodicNotification(AudioTrack track) {
				if (mListener != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
					mListener.onProgress((track.getPlaybackHeadPosition() * 1000) / SAMPLE_RATE);
				}
			}

			@Override
			public void onMarkerReached(AudioTrack track) {
				Log.v(LOG_TAG, "Audio file end reached");
				track.release();
				if (mListener != null) {
					mListener.onCompletion();
				}
				onPlaybackStateChange();
			}
		});
		audioTrack.setPositionNotificationPeriod(SAMPLE_RATE / 30); // 30 times per  second
		audioTrack.setNotificationMarkerPosition(mNumSamples);
		audioTrack.play();
		bPlaying = true;
		onPlaybackStateChange();

		Log.v(LOG_TAG, "Audio streaming started");

		short[] buffer = new short[bufferSize];
		mSamples.rewind();
		int limit = mNumSamples;
		int totalWritten = 0;
		while (mSamples.position() < limit && mShouldContinue) {
			if (bPlaying) {
				int numSamplesLeft = limit - mSamples.position();
				int samplesToWrite;
				if (numSamplesLeft >= buffer.length) {
					mSamples.get(buffer);
					samplesToWrite = buffer.length;
				} else {
					for (int i = numSamplesLeft; i < buffer.length; i++) {
						buffer[i] = 0;
					}
					mSamples.get(buffer, 0, numSamplesLeft);
					samplesToWrite = numSamplesLeft;
				}
				totalWritten += samplesToWrite;
				synchronized (mService) {
					mService.receiveAudio(ShortBuffer.wrap(buffer));
				}
				audioTrack.write(buffer, 0, samplesToWrite);
			}
		}

		if (!mShouldContinue) {
			audioTrack.release();
		}
		Log.v(LOG_TAG, "Audio streaming finished. Samples written: " + totalWritten);
	}
}
