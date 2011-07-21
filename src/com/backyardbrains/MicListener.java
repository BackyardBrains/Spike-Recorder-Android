/**
 * 
 */
package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class MicListener extends Thread {
	private static final int sampleRate = 44100;
	private boolean mDone = false;
	private AudioService service;

	MicListener(AudioService service) {
		this.service = service;
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
	}

	@Override
	public void run() {
		AudioRecord recorder = null;
		int buffersize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		ByteBuffer audioInfo = ByteBuffer.allocateDirect(buffersize);
		audioInfo.order(ByteOrder.nativeOrder());
		try {
			recorder = new AudioRecord(AudioSource.DEFAULT, sampleRate,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, buffersize);

			if(recorder.getState() == AudioRecord.STATE_INITIALIZED) {
				//throw new RuntimeException();
			}
			
			recorder.startRecording();

			while (recorder.read(audioInfo, audioInfo.limit()) > 0 && !mDone) {
				audioInfo.position(0);
				service.receivedAudioData(audioInfo);
			}
		recorder.stop();
		recorder.release();

		} catch (Throwable e) {
			Log.e("MicListener", "Could not open audio souce", e);
		} finally {
			requestStop();
		}
	}

	public void requestStop() {
		mDone = true;
		try {
			join();
		} catch (InterruptedException e) {
			Log.e("BYB", "Mic Listener Thread couldn't rejoin!", e);
		}
	}

}
