package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class MicListener extends Thread {
	private static final String TAG = "BYBMicListener";

	private static final int sampleRate = 44100;
	private boolean mDone = false;
	private AudioRecord recorder;
	private ByteBuffer audioInfo;
	private RecievesAudio service;
	private int buffersize;

	MicListener() {
		buffersize = AudioRecord.getMinBufferSize(sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		audioInfo = ByteBuffer.allocateDirect(buffersize * 2);
		audioInfo.order(ByteOrder.nativeOrder());
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
	}

	/**
	 * @return the audioInfo
	 */
	public ByteBuffer getAudioInfo() {
		return audioInfo;
	}

	public void start(RecievesAudio svc) {
		service = svc;
		if (service != null) {
			Log.d(TAG, "Service interface successfully bound from Thread");
		} else {
			throw new RuntimeException(TAG + ": No interface could be bound");
		}
		super.start();
	}

	public void run() {
		Log.d(TAG, "Thread Launched");
		recorder = null;
		try {
			recorder = newRecorder();
			if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
				throw new RuntimeException(recorder.toString());
			}
			Log.d(TAG, "Recorder Created");

			recorder.startRecording();
			Log.d(TAG, "Recorder Started");

			int limit = audioInfo.limit();
			while (recorder.read(audioInfo, limit / 2) > 0 && !mDone) {
				audioInfo.position(0);
				service.receiveAudio(audioInfo);
			}

			if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				recorder.stop();
				recorder.release();
				Log.d(TAG, "Recorder Released");
			}

		} catch (Throwable e) {
			Log.e(TAG, "Could not open audio souce", e);
		} finally {
			requestStop();
		}
	}

	private AudioRecord newRecorder() {
		return new AudioRecord(AudioSource.DEFAULT, sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				buffersize * 4);
	}

	public void requestStop() {
		mDone = true;
		if (recorder != null) {
			if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				recorder.stop();
				recorder.release();
			}
			recorder = null;
		}
		Log.d(TAG, "Thread cleaned up");
	}
}
