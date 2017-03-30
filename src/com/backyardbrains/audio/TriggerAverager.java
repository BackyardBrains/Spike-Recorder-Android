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

import android.os.Handler;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class TriggerAverager {

    private static final String TAG = makeLogTag(TriggerAverager.class);

    public static final int DEFAULT_SIZE = 30;

    private int maxsize;
    private int[] summedSamples;
    private short[] averagedSamples;

    private ArrayList<short[]> sampleBuffersInAverage;
    private Handler handler;
    private int triggerValue;
    private int lastTriggeredValue;
    private int lastIncomingBufferSize = 0;

    // ---------------------------------------------------------------------------------------------
    TriggerAverager(int size) {
        resetBuffers();
        setMaxsize(size);
        handler = new TriggerHandler();
    }

    public void close() {
        sampleBuffersInAverage.clear();
        sampleBuffersInAverage = null;
    }

    // ---------------------------------------------------------------------------------------------
    private void resetBuffers() {
        sampleBuffersInAverage = new ArrayList<>();
        summedSamples = null;
        averagedSamples = null;
    }

    // ---------------------------------------------------------------------------------------------
    void push(ShortBuffer incoming) {
        incoming.clear();
        processIncomingData(incoming);
    }

    // ---------------------------------------------------------------------------------------------
    void push(ByteBuffer incoming) {
        incoming.clear();
        processIncomingData(incoming.asShortBuffer());
    }

    // ---------------------------------------------------------------------------------------------
    private void processIncomingData(ShortBuffer sb) {
        // reset buffers if size  of buffer changed
        if (sb.capacity() != lastIncomingBufferSize) {
            resetBuffers();
            lastIncomingBufferSize = sb.capacity();
        }
        // reset buffers if threshold changed
        if (lastTriggeredValue != triggerValue) {
            resetBuffers();
            lastTriggeredValue = triggerValue;
        }

        // initialize incoming array
        short[] incomingAsArray = new short[sb.capacity()];
        sb.get(incomingAsArray, 0, incomingAsArray.length);

        // check if we hit the threshold
        for (int i = 0; i < incomingAsArray.length; i++) {
            short s = incomingAsArray[i];
            if ((triggerValue >= 0 && s > triggerValue) || (triggerValue < 0 && s < triggerValue)) {
                // we hit the threshold, center the spike and push to buffers
                incomingAsArray = wrapToCenter(incomingAsArray, i);
                pushToSampleBuffers(incomingAsArray);
                break;
            }
        }
        if (summedSamples == null) summedSamples = new int[incomingAsArray.length];
        if (averagedSamples == null) averagedSamples = new short[summedSamples.length];
        // save averages only if we have where to read to read from
        if (sampleBuffersInAverage.size() > 0) {
            for (int i = 0; i < summedSamples.length; i++) {
                averagedSamples[i] = (short) (summedSamples[i] / sampleBuffersInAverage.size());
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    private short[] wrapToCenter(short[] incomingAsArray, int index) {
        final int middleOfArray = incomingAsArray.length / 2;
        // create a new array to copy the adjusted samples into
        short[] sampleChunk = new short[incomingAsArray.length];
        int sampleChunkPosition = 0;
        if (index > middleOfArray) {
            final int samplesToMove = index - middleOfArray;
            for (int i = 0; i < incomingAsArray.length - samplesToMove; i++) {
                sampleChunk[sampleChunkPosition++] = incomingAsArray[i + samplesToMove];
            }
            for (int i = 0; i < samplesToMove; i++) {
                sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
            }
        } else {
            // it's near beginning, wrap from end on to front
            final int samplesToMove = middleOfArray - index;
            for (int i = incomingAsArray.length - samplesToMove - 1; i < incomingAsArray.length; i++) {
                sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
            }
            for (int i = 0; i < incomingAsArray.length - samplesToMove - 1; i++) {
                sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
            }
        }
        return sampleChunk;
    }

    // ---------------------------------------------------------------------------------------------
    private void pushToSampleBuffers(short[] incomingAsArray) {
        // init summed samples array
        if (summedSamples == null) {
            summedSamples = new int[incomingAsArray.length];
            for (int i = 0; i < incomingAsArray.length; i++) {
                summedSamples[i] = incomingAsArray[i];
            }
            sampleBuffersInAverage.add(incomingAsArray);
            return;
        }

        if (sampleBuffersInAverage.size() >= maxsize) {
            // we have more then max samples, subtract values from first sample
            for (int i = 0; i < sampleBuffersInAverage.get(maxsize - 1).length; i++) {
                summedSamples[i] -= sampleBuffersInAverage.get(0)[i];
            }
            // and remove the first sample
            sampleBuffersInAverage.remove(0);
        }
        // add values from the last sample
        for (int i = 0; i < incomingAsArray.length; i++) {
            summedSamples[i] += incomingAsArray[i];
        }
        // and add the last sample
        sampleBuffersInAverage.add(incomingAsArray);
    }

    // ---------------------------------------------------------------------------------------------
    short[] getAveragedSamples() {
        return averagedSamples;
    }

    // ---------------------------------------------------------------------------------------------
    void setMaxsize(int maxsize) {
        if (maxsize > 0) this.maxsize = maxsize;
    }

    // ---------------------------------------------------------------------------------------------
    Handler getHandler() {
        return handler;
    }

    // ---------------------------------------------------------------------------------------------
    public class TriggerHandler extends Handler {
        public void setThreshold(float y) {
            LOGD(TAG, "setThreshold: " + y);
            triggerValue = (int) y;
        }
    }
}
