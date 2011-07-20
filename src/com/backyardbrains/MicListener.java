/**
 * 
 */
package com.backyardbrains;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;
import android.view.SurfaceView;

public class MicListener extends Thread {
	private boolean mDone = false;

	MicListener() {
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
	}

	@Override
	public void run() {
		AudioRecord recorder = null;
		short[][] buffers = new short[256][160];
		int ix = 0;

		try {

			int N = AudioRecord
					.getMinBufferSize(441000, AudioFormat.CHANNEL_IN_MONO,
							AudioFormat.ENCODING_PCM_16BIT);

			recorder = new AudioRecord(AudioSource.MIC, 441000,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, N * 10);

			recorder.startRecording();

			while (!mDone) {
				short[] buffer = buffers[ix++ % buffers.length];
				N = recorder.read(buffer, 0, buffer.length);
			}
		} catch (Throwable x) {
			Log.w("MicListener", "Error reading voice audio", x);
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
