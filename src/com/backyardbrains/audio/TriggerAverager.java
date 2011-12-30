package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.os.Handler;
import android.util.Log;

public class TriggerAverager {

	private static final String TAG = TriggerAverager.class.getCanonicalName();
	private int maxsize;
	private short[] averagedSamples;
	private ArrayList<short[]> sampleBuffersInAverage;

	private Handler handler;
	public int triggerValue;
	
	public TriggerAverager(int size) {
		sampleBuffersInAverage = new ArrayList<short[]>();
		maxsize = 50;
		averagedSamples = null;
		handler = new TriggerHandler();
	}

	public void push (ByteBuffer incoming) {
		incoming.clear();
		//Log.d(TAG, "Got buffer of samples: "+incoming.capacity());

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
		
		for (short s : incomingAsArray) {
			if (s > triggerValue || s < -triggerValue) {
				sampleBuffersInAverage.add(incomingAsArray);
				break;
			}
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
	
	public Handler getHandler() {
		return handler;
	}
	
	public short[] getAveragedSamples() {
		return averagedSamples;
	}
	
	public class TriggerHandler extends Handler {
		public void setThreshold(float y) {
			//Log.d(TAG, "Got new triggerValue of "+y);
			triggerValue = (int) y;
		}
	}
}
