package com.backyardbrains.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.util.Log;

public class BYBFindSpikesAnalysis extends BYBBaseAsyncAnalysis {

	public final static int		BUFFER_SIZE	= 524288;
	private static final String	TAG			= "BYBFindSpikesAnalysis";

	private static final int	kSchmittON	= 1;
	private static final int	kSchmittOFF	= 2;

	private int					sampleRate;
	private int					numChannels;

	private float				highestPeak	= 0;
	private float				lowestPeak	= 0;
	private int					totalSamples;

	public BYBFindSpikesAnalysis(Context context, short[] data, int sampleRate, int numChannels) {
		super(context, BYBAnalysisType.BYB_ANALYSIS_FIND_SPIKES, true, true);
		this.numChannels = numChannels;
		this.sampleRate = sampleRate;
		allSpikes = new ArrayList<BYBSpike>();
		execute(data);
	}

	public BYBFindSpikesAnalysis(AnalysisListener listener, short[] data, int sampleRate, int numChannels) {
		super(listener, BYBAnalysisType.BYB_ANALYSIS_FIND_SPIKES, true, true);
		this.numChannels = numChannels;
		this.sampleRate = sampleRate;
		allSpikes = new ArrayList<BYBSpike>();
		execute(data);
	}

	private ArrayList<BYBSpike> allSpikes;

	public float getHighestPeak() {
		return highestPeak;
	}

	public float getLowestPeak() {
		return lowestPeak;
	}

	public int getTotalSamples() {
		return totalSamples;
	}

	public BYBSpike[] getSpikes() {
		return allSpikes.toArray(new BYBSpike[allSpikes.size()]);
	}

	@Override
	public void process(short[] data) {

		int numberOfSamples = data.length;
		float killInterval = 0.005f;// 5ms
		int numberOfBins = 200;
		int lengthOfBin = numberOfSamples / numberOfBins;
		int maxLengthOfBin = (BUFFER_SIZE / numChannels);
		if (lengthOfBin > maxLengthOfBin) {
			lengthOfBin = maxLengthOfBin;
			numberOfBins = (int) Math.ceil((float) numberOfSamples / (float) lengthOfBin);
		}

		if (lengthOfBin < 50) {
			Log.d(TAG, "findSpikes: File too short.");
			return;
		}

		float[] stdArray = new float[numberOfBins];

		// calculate STD for each bin
		int ibin;
		// TODO: convert to non-interleaved so to process more than 1 channel.
		for (ibin = 0; ibin < numberOfBins; ibin++) {
			// [fileReader retrieveFreshAudio:tempCalculationBuffer
			// numFrames:(UInt32)(lengthOfBin) numChannels:aFile.numChannels];
			// get only one channel and put it on the begining of buffer in
			// non-interleaved form
			/*
			 * vDSP_vsadd((float *)&tempCalculationBuffer[channelIndex],
			 * aFile.numChannels, &zero,* tempCalculationBuffer, 1,
			 * lengthOfBin);//
			 */
			stdArray[ibin] = BYBAnalysis.SDT(data, ibin * lengthOfBin, lengthOfBin);
		}
		// sort array of STDs
		Arrays.sort(stdArray);

/// std::sort(stdArray, stdArray + numberOfBins, std::greater<float>());
// take value that is greater than 40% STDs
		int m = (int) Math.floor((float) stdArray.length / 2.0f);
		for (int i = 0; i < m; i++) {
			float tmp = stdArray[i];
			stdArray[i] = stdArray[stdArray.length - 1 - i];
			stdArray[stdArray.length - 1 - i] = tmp;
		}

		float sig = 2 * stdArray[(int) Math.ceil(((float) numberOfBins) * 0.4f)];
		float negsig = -1 * sig;

		// make maximal bins for faster processing
		lengthOfBin = (BUFFER_SIZE / numChannels);
		numberOfBins = (int) Math.ceil((float) numberOfSamples / (float) lengthOfBin);

		// find peaks
		int numberOfFramesRead;
		int isample;

		int schmitPosState = kSchmittOFF;
		int schmitNegState = kSchmittOFF;
		float maxPeakValue = -1000.0f;
		int maxPeakIndex = 0;
		float minPeakValue = 1000.0f;
		int minPeakIndex = 0;

		ArrayList<BYBSpike> peaksIndexes = new ArrayList<BYBSpike>();
		ArrayList<BYBSpike> peaksIndexesNeg = new ArrayList<BYBSpike>();

		// read lengthOfBin frames except for last one reading where we should
		// read only what is left
// numberOfFramesRead = ibin == (numberOfBins-1) ? (numberOfSamples %
// lengthOfBin):lengthOfBin;

		// [fileReader retrieveFreshAudio:tempCalculationBuffer
		// numFrames:(UInt32)(numberOfFramesRead) numChannels:aFile.numChannels
		// seek:ibin*lengthOfBin];

		// get only one channel and put it on the begining of buffer in
		// non-interleaved form
// vDSP_vsadd((float *)&tempCalculationBuffer[channelIndex],
// aFile.numChannels,
// &zero,
// tempCalculationBuffer,
// 1,
// lengthOfBin);
//
		for (isample = 0; isample < data.length; isample++) {
			// determine state of positive schmitt trigger
			if (schmitPosState == kSchmittOFF && data[isample] > sig) {
				schmitPosState = kSchmittON;
				maxPeakValue = -1000.0f;
			} else if (schmitPosState == kSchmittON && data[isample] < 0) {
				schmitPosState = kSchmittOFF;
				peaksIndexes.add(new BYBSpike(maxPeakValue, maxPeakIndex, ((float) maxPeakIndex) / sampleRate));
			}

			// determine state of negative schmitt trigger
			if (schmitNegState == kSchmittOFF && data[isample] < negsig) {
				schmitNegState = kSchmittON;
				minPeakValue = 1000.0f;
			} else if (schmitNegState == kSchmittON && data[isample] > 0) {
				schmitNegState = kSchmittOFF;
				peaksIndexesNeg.add(new BYBSpike(minPeakValue, minPeakIndex, ((float) minPeakIndex) / sampleRate));

			}

			// find max in positive peak
			if (schmitPosState == kSchmittON && data[isample] > maxPeakValue) {
				maxPeakValue = data[isample];
				maxPeakIndex = isample;
			}

			// find min in negative peak
			else if (schmitNegState == kSchmittON && data[isample] < minPeakValue) {
				minPeakValue = data[isample];
				minPeakIndex = isample;
			}
		}

		int i;
		if (peaksIndexes.size() > 0) {
			// Filter positive spikes using kill interval

			for (i = 0; i < peaksIndexes.size() - 1; i++) // look on the right
			{
				if (peaksIndexes.get(i).value < peaksIndexes.get(i + 1).value) {
					if ((peaksIndexes.get(i + 1).time - peaksIndexes.get(i).time) < killInterval) {
						peaksIndexes.remove(i);
						i--;
					}
				}
			}

			for (i = 1; i < peaksIndexes.size(); i++) // look on the left
														// neighbor
			{
				if (peaksIndexes.get(i).value < peaksIndexes.get(i - 1).value) {
					if ((peaksIndexes.get(i).time - peaksIndexes.get(i - 1).time) < killInterval) {
						peaksIndexes.remove(i);
						i--;
					}
				}
			}
		}
		if (peaksIndexesNeg.size() > 0) {
			// Filter positive spikes using kill interval

			for (i = 0; i < peaksIndexesNeg.size() - 1; i++) // look on the
																// right
			{
				if (peaksIndexesNeg.get(i).value > peaksIndexesNeg.get(i + 1).value) {
					if ((peaksIndexesNeg.get(i + 1).time - peaksIndexesNeg.get(i).time) < killInterval) {
						peaksIndexesNeg.remove(i);
						i--;
					}
				}
			}

			for (i = 1; i < peaksIndexesNeg.size(); i++) // look on the left
															// neighbor
			{
				if (peaksIndexesNeg.get(i).value > peaksIndexesNeg.get(i - 1).value) {
					if ((peaksIndexesNeg.get(i).time - peaksIndexesNeg.get(i - 1).time) < killInterval) {
						peaksIndexesNeg.remove(i);
						i--;
					}
				}
			}
		}

		peaksIndexes.addAll(peaksIndexesNeg);
		Collections.sort(peaksIndexes, new Comparator<BYBSpike>() {
			@Override
			public int compare(BYBSpike o1, BYBSpike o2) {
				return o1.index - o2.index;
			}
		});

		allSpikes.addAll(peaksIndexes);

		highestPeak = Float.MIN_VALUE;
		lowestPeak = Float.MAX_VALUE;
		for (int k = 0; k < allSpikes.size(); k++) {
			if (allSpikes.get(k).value > highestPeak) highestPeak = allSpikes.get(k).value;
			if (allSpikes.get(k).value < lowestPeak) lowestPeak = allSpikes.get(k).value;
		}
		totalSamples = data.length;
	}
}