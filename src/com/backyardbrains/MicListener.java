package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class MicListener {
	private static final String TAG = "BYBMicListener";

	private static final int sampleRate = 44100;
	private boolean mDone = false;
	private AudioRecord recorder;
	private ByteBuffer audioInfo;
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

	public void start(RecievesAudio service) {
		recorder = null;
		try {
			recorder = newRecorder();

			if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
				throw new RuntimeException(recorder.toString());
			}

			recorder.startRecording();

			int limit = audioInfo.limit();
			while (recorder.read(audioInfo, limit / 2) > 0 && !mDone) {
				audioInfo.position(0);
				service.receiveAudio(audioInfo);
			}

			recorder.stop();
			recorder.release();

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
	}
}
