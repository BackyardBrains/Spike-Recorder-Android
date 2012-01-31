package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

/**
 * A specialized thread to manage Android's {@link AudioRecord} objects and
 * continuously poll out information to an accompanying {@link RecievesAudio}
 * interface.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * 
 */
public class MicListener extends Thread {
	private static final String TAG = MicListener.class.getCanonicalName();

	/**
	 * @TODO turn this into a list of sample rates to loop over
	 */
	private static final int sampleRate = 44100;
	private boolean mDone = false;
	private AudioRecord recorder;
	private ByteBuffer audioInfo;
	private ReceivesAudio service;
	private int buffersize;

	/**
	 * Find the appropriate buffer size for working on this device and allocate
	 * space for the audioInfo {@link ByteBuffer} based on that size, then tell
	 * Android we'll be using high-priority audio-processing.
	 */
	MicListener() {
		setBufferSize();
		audioInfo = ByteBuffer.allocateDirect(buffersize);
		audioInfo.order(ByteOrder.nativeOrder());
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
	}

	private void setBufferSize() {
		buffersize = AudioRecord.getMinBufferSize(sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		Log.d(TAG, "Found buffer size of :" + buffersize);
	}

	/**
	 * An alternate to {@link Thread#start()}, which allows us to provide a
	 * service which implements {@link RecievesAudio} then start the thread as
	 * normal. This allows for {@link MicListener#run()} to poll the
	 * implementing interface with new data from {@link AudioRecord} as it
	 * becomes available.
	 * 
	 * @see com.backyardbrains.audio.AudioService#turnOnMicThread()
	 * @param svc
	 *            the service that implements the {@link RecievesAudio}
	 */
	public void start(ReceivesAudio svc) {
		service = svc;
		if (service != null) {
			Log.d(TAG,
					"Service interface successfully bound from MicListener Thread");
		} else {
			throw new RuntimeException(TAG + ": No interface could be bound");
		}
		super.start();
	}

	/**
	 * Get a new recorder, check to see that we can actually record, start
	 * recording, and then continuously read audio from the recording interface,
	 * while pushing it out to the receiving service that implements
	 * {@link RecievesAudio} (set in {@link MicListener#start(RecievesAudio)}.
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
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
			
			while (!mDone && recorder.read(audioInfo, audioInfo.limit()/4) > 0) {
				audioInfo.clear();
				byte[] swapper = new byte[audioInfo.limit()/4];
				audioInfo.get(swapper);
				ByteBuffer derp = ByteBuffer.wrap(swapper);
				derp.order(ByteOrder.nativeOrder());
				synchronized (service) {
					service.receiveAudio(derp);
				}
				audioInfo.clear();
			}
		} catch (Throwable e) {
			Log.e(TAG, "Could not open audio souce", e);
		} finally {
			if (!mDone)
				requestStop();
		}
	}

	private void stopRecorder() {
		try {
			recorder.stop();
			recorder.release();
		} catch (IllegalStateException e) {
			Log.w(TAG, "Caught Illegal State Exception: " + e.toString());
		}
		recorder = null;
		Log.d(TAG, "Recorder Released");
	}

	/**
	 * Convenience function for spitting out new {@link AudioRecord} objects
	 * 
	 * @return an {@link AudioRecord} object preset to match the rest of the
	 *         application
	 */
	private AudioRecord newRecorder() {
		return new AudioRecord(AudioSource.DEFAULT, sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				buffersize);
	}

	/**
	 * clean up {@link AudioRecord} resource before exiting thread.
	 */
	public void requestStop() {
		mDone = true;
		if (recorder != null) {
			if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				stopRecorder();
			}
			recorder = null;
		}
		// @TODO - figure out why joining here causes service to not stop.
	}
}
