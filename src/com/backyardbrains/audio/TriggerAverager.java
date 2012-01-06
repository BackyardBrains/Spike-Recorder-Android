package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.os.Handler;
//import android.util.Log;

public class TriggerAverager {

	//private static final String TAG = TriggerAverager.class.getCanonicalName();
	private int maxsize;
	private short[] averagedSamples;
	private ArrayList<short[]> sampleBuffersInAverage;

	private Handler handler;
	public int triggerValue;
	
	public TriggerAverager(int size) {
		sampleBuffersInAverage = new ArrayList<short[]>();
		setMaxsize(10);
		averagedSamples = null;
		handler = new TriggerHandler();
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
			if (s > triggerValue) {
				incomingAsArray = findCenterAndWrap(incomingAsArray, i);
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

	private short[] findCenterAndWrap(short[] incomingAsArray, int index) {
		final boolean isPositive = incomingAsArray[index] > 0;
		int centerIndex = -1;
		// Now find where the spike crosses back down across zero
		for (int i = index; i < incomingAsArray.length; i++) {
			if (isPositive && incomingAsArray[i] < 0) {
				centerIndex = i; break;
			}
			/*
			if (!isPositive && incomingAsArray[i] > 0) {
				centerIndex = i; break;
			}
			*/
		}
		final int middleOfArray = incomingAsArray.length / 2;
		if (centerIndex == -1) {
			return incomingAsArray;
		}
		// create a new array to copy the adjusted samples into
		short [] sampleChunk = new short[incomingAsArray.length];
		int sampleChunkPosition = 0;
		if(centerIndex > middleOfArray) {
			final int samplesToMove = centerIndex - middleOfArray;
			for (int i = 0; i<incomingAsArray.length-samplesToMove; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i+samplesToMove];
			}
			for (int i = 0; i<samplesToMove; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
			}
		} else {
			// it's near beginning, wrap from end on to front
			final int samplesToMove = middleOfArray - centerIndex;
			for (int i = samplesToMove; i<incomingAsArray.length; i++) {
				sampleChunk[sampleChunkPosition++] = incomingAsArray[i];
			}
			for (int i = 0; i<samplesToMove -1; i++) {
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
