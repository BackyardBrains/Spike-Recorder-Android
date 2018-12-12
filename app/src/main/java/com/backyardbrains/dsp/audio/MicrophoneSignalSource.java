package com.backyardbrains.dsp.audio;

import android.media.AudioRecord;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import com.backyardbrains.dsp.AbstractSignalSource;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;
import com.crashlytics.android.Crashlytics;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class MicrophoneSignalSource extends AbstractSignalSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(MicrophoneSignalSource.class);

    // Number of seconds buffers should hold by default
    private static final int BUFFER_SIZE_IN_SEC = 1;
    // Number of samples buffers should hold by default
    private static final int BUFFER_SIZE_IN_SAMPLES = AudioUtils.DEFAULT_SAMPLE_RATE * BUFFER_SIZE_IN_SEC;

    /**
     * Thread used for reading audio from microphone.
     */
    private class ReadThread extends Thread {

        private byte[] buffer;

        ReadThread() {
            buffer = new byte[AudioUtils.DEFAULT_IN_BUFFER_SIZE];
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            working.set(true);
        }

        @Override public void run() {
            try {
                // recorder not set
                if (recorder == null) return;

                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    throw new RuntimeException(recorder.toString());
                }
                LOGD(TAG, "Recorder Created");

                recorder.startRecording();
                LOGD(TAG, "Recorder Started");
                int read;
                while (working.get() && recorder != null) {
                    if ((read = recorder.read(buffer, 0, buffer.length)) > 0) {
                        writeToBuffer(buffer, read);
                    }
                }
            } catch (Throwable e) {
                LOGE(TAG, "Could not open audio source", e);
                Crashlytics.logException(e);
            }
        }
    }

    // Microphone thread
    private ReadThread readThread;
    // AudioRecord instance
    @SuppressWarnings("WeakerAccess") AudioRecord recorder;
    // Flag that indicates whether thread should be running
    @SuppressWarnings("WeakerAccess") AtomicBoolean working = new AtomicBoolean();

    private static final Object lock = new Object();

    MicrophoneSignalSource(@NonNull AudioRecord recorder) {
        super(recorder.getSampleRate(), recorder.getChannelCount(), BUFFER_SIZE_IN_SAMPLES);

        this.recorder = recorder;
    }

    @SuppressWarnings("WeakerAccess") @RequiresApi(api = Build.VERSION_CODES.M) AudioRecord updateRecorder(
        @NonNull AudioRecord audioRecord) {
        // release active AudioRecord
        requestStop();
        // save new AudioRecord
        recorder = audioRecord;
        // update sample rate and channel count
        setSampleRate(recorder.getSampleRate());
        setChannelCount(recorder.getChannelCount());
        // restart read thread
        readThread = new ReadThread();
        readThread.start();

        return recorder;
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void start() {
        if (readThread == null) {
            // Start microphone in a thread
            readThread = new ReadThread();
            readThread.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void stop() {
        if (readThread != null) {
            working.set(false);
            readThread = null;

            LOGD(TAG, "Microphone stopped");
        }
    }

    //private final Benchmark benchmark = new Benchmark("MICROPHONE_TEST_WITH_AM_MODULATION").warmUp(200)
    //    .sessions(10)
    //    .measuresPerSession(200)
    //    .logBySession(false)
    //    .listener(() -> {
    //        //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
    //    });

    @Override public void processIncomingData(@NonNull SamplesWithEvents outData, byte[] inData, int inDataLength) {
        //benchmark.start();
        JniUtils.processMicrophoneStream(outData, inData, inDataLength);
        //benchmark.end();
    }

    @Override public int getType() {
        return Type.MICROPHONE;
    }

    /**
     * Clean up {@link AudioRecord} resource before exiting thread.
     */
    @SuppressWarnings("WeakerAccess") void requestStop() {
        LOGD(TAG, "Requesting Recorder stop");
        if (recorder != null) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                LOGD(TAG, "Before Stop Recorder");
                stopRecorder();
            }
            recorder = null;
        }
    }

    //
    private void stopRecorder() {
        synchronized (lock) {
            LOGD(TAG, "Stopping Recorder");
            if (recorder != null) {
                try {
                    LOGD(TAG, "About to Release");
                    recorder.stop();
                    recorder.release();

                    LOGD(TAG, "Recorder resources released");
                } catch (IllegalStateException e) {
                    LOGE(TAG, "Caught Illegal State Exception: " + e.toString());
                    Crashlytics.logException(e);
                }
                recorder = null;
            }
            LOGD(TAG, "Recorder Released");
        }
    }
}
