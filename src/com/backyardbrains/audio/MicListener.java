/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioRecord;
import android.support.annotation.NonNull;
import com.backyardbrains.R;
import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * A specialized thread to manage Android's {@link AudioRecord} objects and
 * continuously pull out information to an accompanying {@link ReceivesAudio}
 * interface.
 *
 * @author Nathan Dotz <nate@backyardbrains.com>
 */
class MicListener extends Thread {

    private static final String TAG = makeLogTag(MicListener.class);

    private final ReceivesAudio service;
    private final int bufferSize;
    private final ByteBuffer audioInfo;

    private AudioRecord recorder;
    private boolean done;

    /**
     * Find the appropriate buffer size for working on this device and allocate space for the audioInfo {@link
     * ByteBuffer} based on that size, then tell Android we'll be using high-priority audio-processing.
     *
     * @param service the service that implements the {@link ReceivesAudio}
     * @see com.backyardbrains.audio.AudioService#turnOnMicThread()
     */
    MicListener(@NonNull ReceivesAudio service) {
        this.service = service;

        bufferSize = AudioUtils.IN_BUFFER_SIZE;
        audioInfo = ByteBuffer.allocateDirect(bufferSize);
        audioInfo.order(ByteOrder.nativeOrder());
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    }

    int getBufferSize() {
        return bufferSize;
    }

    /**
     * An alternate to {@link Thread#start()}, which allows us to check whether a service which implements {@link
     * ReceivesAudio} is provided then start the thread as normal. This allows for {@link MicListener#run()} to pull the
     * implementing interface with new data from {@link AudioRecord} as it becomes available.
     */
    @Override public void start() {
        if (service != null) {
            LOGD(TAG, "Service interface successfully bound from MicListener Thread");
        } else {
            throw new RuntimeException(TAG + ": No interface could be bound");
        }
        super.start();
    }

    /**
     * Get a new recorder, check to see that we can actually record, start recording, and then continuously read audio
     * from the recording interface, while pushing it out to the receiving service that implements {@link ReceivesAudio}
     * (set in {@link MicListener#start()}.
     *
     * @see java.lang.Thread#run()
     */
    @Override public void run() {
        LOGD(TAG, "Thread Launched");
        recorder = null;

        final String prefsname = ((AudioService) service).getResources().getString(R.string.global_prefs);
        final String speedPrefsKey = ((AudioService) service).getResources().getString(R.string.microphone_read_speed);

        SharedPreferences prefs = ((AudioService) service).getSharedPreferences(prefsname, Context.MODE_PRIVATE);
        String preferencesSpeed = prefs.getString(speedPrefsKey, "1");
        final int readSpeedDivisor = Integer.parseInt(preferencesSpeed);
        try {
            recorder = AudioUtils.createAudioRecord();
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException(recorder.toString());
            }
            LOGD(TAG, "Recorder Created");

            recorder.startRecording();
            LOGD(TAG, "Recorder Started");
            int readAmt = audioInfo.limit() / readSpeedDivisor;
            //((AudioService) service).setMicListenerBufferSizeInSamples(readAmt/2);
            while (!done && recorder.read(audioInfo, readAmt) > 0) {
                audioInfo.clear();
                byte[] swapper = new byte[readAmt];
                audioInfo.get(swapper);
                ByteBuffer derp = ByteBuffer.wrap(swapper);
                derp.order(ByteOrder.nativeOrder());
                synchronized (service) {
                    service.receiveAudio(derp);
                }
                audioInfo.clear();
            }
        } catch (Throwable e) {
            LOGE(TAG, "Could not open audio source", e);
        } finally {
            if (!done) requestStop();
        }
    }

    private void stopRecorder() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (IllegalStateException e) {
                LOGE(TAG, "Caught Illegal State Exception: " + e.toString());
            }
            recorder = null;
        }
        LOGD(TAG, "Recorder Released");
    }

    /**
     * Clean up {@link AudioRecord} resource before exiting thread.
     */
    void requestStop() {
        done = true;
        if (recorder != null) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                stopRecorder();
            }
            recorder = null;
        }
    }
}
