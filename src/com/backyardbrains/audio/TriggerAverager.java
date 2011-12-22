package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class TriggerAverager {

	private int maxsize;
	private short[] averagedSamples;
	private ArrayList<short[]> sampleBuffersInAverage;
	
	public TriggerAverager(int size) {
		sampleBuffersInAverage = new ArrayList<short[]>();
		maxsize = 50;
		averagedSamples = null;
	}

	public void push (ByteBuffer incoming) {
		ShortBuffer sb = incoming.asShortBuffer();
		short[] incomingAsArray = new short [sb.capacity()];
		sb.get(incomingAsArray, 0, incomingAsArray.length);

		if (averagedSamples == null) {
			averagedSamples = incomingAsArray;
			sampleBuffersInAverage.add(incomingAsArray);
			return;
		}

		while (sampleBuffersInAverage.size() >= maxsize) {
			sampleBuffersInAverage.remove(0);
		}
		
		sampleBuffersInAverage.add(incomingAsArray);
		
		for(int i = 0; i < averagedSamples.length; i++) {
			Integer curAvg = 0;
			for (short[] prev : sampleBuffersInAverage) {
				curAvg += prev[i];
			}
			curAvg /= sampleBuffersInAverage.size();
			averagedSamples[i] = curAvg.shortValue();
		}
	}
	
	public short[] getAveragedSamples() {
		return averagedSamples;
	}
}
