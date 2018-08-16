package com.backyardbrains.audio;

import android.media.AudioRecord;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.processing.AbstractSampleSource;
import com.backyardbrains.data.processing.SamplesWithEvents;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.JniUtils;
import com.crashlytics.android.Crashlytics;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class MicrophoneSampleSource extends AbstractSampleSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(MicrophoneSampleSource.class);

    // Number of seconds buffers should hold by default
    private static final int BUFFER_SIZE_IN_SEC = 1;
    // Number of samples buffers should hold by default
    private static final int BUFFER_SIZE_IN_SAMPLES = AudioUtils.SAMPLE_RATE * BUFFER_SIZE_IN_SEC;
    // Number of bytes buffers should hold by default
    private static final int BUFFER_SIZE_IN_BYTES = BUFFER_SIZE_IN_SAMPLES * 2;

    /**
     * Thread used for reading audio from microphone.
     */
    private class ReadThread extends Thread {

        private AudioRecord recorder;
        private byte[] buffer;

        // Flag that indicates whether thread should be running
        private AtomicBoolean working = new AtomicBoolean(true);

        ReadThread() {
            buffer = new byte[AudioUtils.IN_BUFFER_SIZE];
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        }

        @Override public void run() {
            recorder = null;

            try {
                recorder = AudioUtils.createAudioRecord();
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    throw new RuntimeException(recorder.toString());
                }
                LOGD(TAG, "Recorder Created");

                recorder.startRecording();
                LOGD(TAG, "Recorder Started");
                int read;
                while (working.get() && recorder != null) {
                    if ((read = recorder.read(buffer, 0, buffer.length)) > 0) {
                        //LOGD(TAG, "READING: " + read);
                        writeToBuffer(buffer, 0, read);
                    }
                }
            } catch (Throwable e) {
                LOGE(TAG, "Could not open audio source", e);
                Crashlytics.logException(e);
            } finally {
                requestStop();
            }
        }

        void stopWorking() {
            working.set(false);
        }

        /**
         * Clean up {@link AudioRecord} resource before exiting thread.
         */
        void requestStop() {
            LOGD(TAG, "Requesting Recorder stop");
            working.set(false);
            if (recorder != null) {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    LOGD(TAG, "Before Stop Recording");
                    stopRecorder();
                }

                recorder = null;
            }
        }

        private void stopRecorder() {
            LOGD(TAG, "Stopping Recorder");
            if (recorder != null) {
                try {
                    LOGD(TAG, "About to Release ");
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

    // Microphone thread
    private ReadThread readThread;

    MicrophoneSampleSource(@Nullable SampleSourceListener listener) {
        super(BUFFER_SIZE_IN_BYTES, listener);

        setSampleRate(AudioUtils.SAMPLE_RATE);
    }

    @Override protected void onInputStart() {
        if (readThread == null) {
            // Start microphone in a thread
            readThread = new ReadThread();
            readThread.start();
        }
    }

    @Override protected void onInputStop() {
        if (readThread != null) {
            readThread.stopWorking();
            readThread = null;

            LOGD(TAG, "Microphone stopped");
        }
    }

    private final Benchmark benchmark = new Benchmark("MICROPHONE_TEST_WITH_AM_MODULATION").warmUp(200)
        .sessions(10)
        .measuresPerSession(200)
        .logBySession(false)
        .logToFile(false)
        .listener(new Benchmark.OnBenchmarkListener() {
            @Override public void onEnd() {
                //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
            }
        });

    @NonNull @Override protected SamplesWithEvents processIncomingData(byte[] data, int length) {
        //benchmark.start();
        JniUtils.processMicrophoneStream(samplesWithEvents, data, length);
        //benchmark.end();

        return samplesWithEvents;
    }

    @Override public int getType() {
        return Type.MICROPHONE;
    }
}
