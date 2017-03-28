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

import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class TriggerAverager {

    private static final String TAG = makeLogTag(TriggerAverager.class);

    private int maxsize;
    private short[] averagedSamples;
    private ArrayList<short[]> sampleBuffersInAverage;

    private Handler handler;
    private int triggerValue;
    private int lastTriggeredValue;
    private int lastIncomingBufferSize = 0;
    public static final int defaultSize = 100;

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
        if (sb.capacity() != lastIncomingBufferSize) {
            resetBuffers();
            lastIncomingBufferSize = sb.capacity();
        }

        short[] incomingAsArray = new short[sb.capacity()];
        sb.get(incomingAsArray, 0, incomingAsArray.length);

        for (int i = 0; i < incomingAsArray.length; i++) {
            short s = incomingAsArray[i];
            if ((triggerValue >= 0 && s > triggerValue) || (triggerValue < 0 && s < triggerValue)) {
                if (lastTriggeredValue != triggerValue) {
                    resetBuffers();
                    lastTriggeredValue = triggerValue;
                }
                incomingAsArray = wrapToCenter(incomingAsArray, i);
                pushToSampleBuffers(incomingAsArray);
                break;
            }
        }
        if (averagedSamples == null) {
            return;
        }
        for (int i = 0; i < averagedSamples.length; i++) {
            Integer curAvg = 0;
            for (short[] prev : sampleBuffersInAverage) {
                curAvg += prev[i];
            }
            curAvg /= sampleBuffersInAverage.size();
            averagedSamples[i] = curAvg.shortValue();
        }
    }

    //----------------------------------------------------------------------------------------------
    private short[] wrapToCenter(short[] incomingAsArray, int index) {
        final int middleOfArray = incomingAsArray.length / 2;
        // create a new array to copy the adjusted samples into
        short[] sampleChunk = new short[incomingAsArray.length];
        int sampleChunkPosition = 0;
        if (index > middleOfArray) {
            // //Log.d(TAG, "Wrapping from end onto beginning");
            final int samplesToMove = index - middleOfArray;
            for (int i = 0; i < incomingAsArray.length - samplesToMove; i++) {
                sampleChunk[sampleChunkPosition++] = incomingAsArray[i + samplesToMove];
            }
            for (int i = 0; i < samplesToMove; i++) {
                sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
            }
        } else {
            // it's near beginning, wrap from end on to front
            // //Log.d(TAG, "Wrapping from beginning onto end");
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
        if (averagedSamples == null) {
            averagedSamples = incomingAsArray;
            //*
            sampleBuffersInAverage.add(incomingAsArray);
            return;
        }

        while (sampleBuffersInAverage.size() >= maxsize) {
            sampleBuffersInAverage.remove(0);
        }
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
            triggerValue = (int) y;
        }
    }
}
