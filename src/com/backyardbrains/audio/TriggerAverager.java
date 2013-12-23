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

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.os.Handler;

public class TriggerAverager {

	// private static final String TAG = TriggerAverager.class.getCanonicalName();
	private int maxsize;
	private short[] averagedSamples;
	private ArrayList<short[]> sampleBuffersInAverage;

	private Handler handler;
	private int triggerValue;
	private int lastTriggeredValue;

	public TriggerAverager(int size) {
		resetBuffers();
		setMaxsize(size);
		handler = new TriggerHandler();
	}

	public void resetBuffers() {
		sampleBuffersInAverage = new ArrayList<short[]>();
		averagedSamples = null;
	}

	public void push (ByteBuffer incoming) {
		incoming.clear();
		//Log.d(TAG, "Got buffer of samples: "+incoming.capacity());

		ShortBuffer sb = incoming.asShortBuffer();

		short[] incomingAsArray = new short [sb.capacity()];
		sb.get(incomingAsArray, 0, incomingAsArray.length);
		
		// Scan for triggers 

		
		for (int i = 0; i<incomingAsArray.length; i++) {
			short s = incomingAsArray[i];

			if ((triggerValue >= 0 && s > triggerValue) || (triggerValue < 0 && s < triggerValue)) {
				if(lastTriggeredValue != triggerValue) {
					resetBuffers();
					lastTriggeredValue = triggerValue;
				}
				incomingAsArray = wrapToCenter(incomingAsArray, i);
				pushToSampleBuffers(incomingAsArray);
			}
		}
		
		if (averagedSamples == null) {
			return;
		}
		
		for(int i = 0; i < averagedSamples.length; i++) {
			Integer curAvg = 0;
			for (short[] prev : sampleBuffersInAverage) {
				curAvg += prev[i];
			}
			curAvg /= sampleBuffersInAverage.size();
			averagedSamples[i] = curAvg.shortValue();
		}
	}

	private short[] wrapToCenter(short[] incomingAsArray, int index) {
		final int middleOfArray = incomingAsArray.length / 2;
		// create a new array to copy the adjusted samples into
		short [] sampleChunk = new short[incomingAsArray.length];
		int sampleChunkPosition = 0;
		if(index > middleOfArray) {
			// Log.d(TAG, "Wrapping from end onto beginning");
			final int samplesToMove = index - middleOfArray;
			for (int i = 0; i<incomingAsArray.length-samplesToMove; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i+samplesToMove];
			}
			for (int i = 0; i<samplesToMove; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
			}
		} else {
			// it's near beginning, wrap from end on to front
			// Log.d(TAG, "Wrapping from beginning onto end");
			final int samplesToMove = middleOfArray - index;
			for (int i = incomingAsArray.length - samplesToMove - 1; i < incomingAsArray.length; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
			}
			for (int i = 0; i<incomingAsArray.length - samplesToMove -1; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
			}
			
		}
		return sampleChunk;
	}

	private void pushToSampleBuffers(short[] incomingAsArray) {
		if (averagedSamples == null) {
			averagedSamples = incomingAsArray;
			sampleBuffersInAverage.add(incomingAsArray);
			return;
		}

		while (sampleBuffersInAverage.size() >= getMaxsize()) {
			sampleBuffersInAverage.remove(0);
		}
		
		sampleBuffersInAverage.add(incomingAsArray);
	}
	
	public short[] getAveragedSamples() {
		return averagedSamples;
	}
	
	public int getMaxsize() {
		return maxsize;
	}

	public void setMaxsize(int maxsize) {
		this.maxsize = maxsize;
	}

	public Handler getHandler() {
		return handler;
	}

	public class TriggerHandler extends Handler {
		public void setThreshold(float y) {
			//Log.d(TAG, "Got new triggerValue of "+y);
			triggerValue = (int) y;
		}
	}
}
